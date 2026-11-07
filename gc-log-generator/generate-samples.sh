#!/usr/bin/env bash
#
# Generates JVM log samples (GC, safepoints, class loading, threads, JIT, TLAB, string deduplication) for several
# JDK versions, garbage collectors and locales, by running the gc-log-generator workload on each combination.
# A collector that is not available in a given JDK is skipped.
#
# Every sample is written as <output>/<jdk>/<collector>-<locale>-<platform>.log.gz and described by a row in
# <output>/samples.tsv (the exact JVM options included). Rows of other platforms or of combinations that were
# not run are kept, so samples from several machines can live in one directory. README.md is rendered from
# samples.tsv after every run.
#
# Usage: gc-log-generator/generate-samples.sh
#        gc-log-generator/generate-samples-linux.sh   (the same inside a Linux Docker container)
#
# Environment variables (all optional):
#   JDK_ROOT       directory with installed JDKs (default: ~/Library/Java/JavaVirtualMachines)
#   JDKS           space separated "label=directory-glob" pairs; the newest matching directory is used
#   COLLECTORS     collectors to try (default: "parallel g1 zgc shenandoah shenandoah-generational")
#   LOCALES        locales to run with (default: "en_US pl_PL")
#   PLATFORM       platform name used in file names (default: macos on macOS, linux on Linux)
#   OUTPUT_DIR     where the samples are written (default: <project>/gc-log-samples)
#   PARALLEL_RUNS  number of JVMs running at the same time (default: 3)
#   SCALE          duration multiplier passed to the workload (default: 1.0)
#   SKIP_BUILD     set to 1 to reuse an already built jar
#   README_ONLY    set to 1 to only render <output>/README.md from <output>/samples.tsv

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR="$SCRIPT_DIR/target/gc-log-generator.jar"
RUN_DIR="$SCRIPT_DIR/target/sample-runs"

default_platform() {
    case "$(uname -s)" in
        Darwin) echo "macos" ;;
        Linux) echo "linux" ;;
        *) uname -s | tr '[:upper:]' '[:lower:]' ;;
    esac
}

JDK_ROOT="${JDK_ROOT:-$HOME/Library/Java/JavaVirtualMachines}"
JDKS="${JDKS:-jdk8=corretto-1.8.* jdk11=corretto-11.* jdk17=corretto-17.* jdk25=corretto-25.* jdk27=corretto-27.*}"
COLLECTORS="${COLLECTORS:-parallel g1 zgc shenandoah shenandoah-generational}"
LOCALES="${LOCALES:-en_US pl_PL}"
PLATFORM="${PLATFORM:-$(default_platform)}"
OUTPUT_DIR="${OUTPUT_DIR:-$PROJECT_DIR/gc-log-samples}"
PARALLEL_RUNS="${PARALLEL_RUNS:-3}"
SCALE="${SCALE:-1.0}"
SKIP_BUILD="${SKIP_BUILD:-0}"
README_ONLY="${README_ONLY:-0}"

MANIFEST="$OUTPUT_DIR/samples.tsv"
MANIFEST_HEADER="file	platform	system	jdk	feature_version	build	collector	locale	lines	jvm_options"

HEAP_FLAGS="-Xms512m -Xmx512m"

# Unified logging selection recommended in the main README, extended with everything the analyzer parses
# (gc+heap=trace for region sizes, gc+tlab=trace for TLAB statistics).
COMMON_SELECTION="codecache=info,class+unload,class+load,os+thread,safepoint,gc*,gc+ergo=trace,gc+age=trace,gc+phases=trace,gc+humongous=trace,gc+heap=trace,gc+tlab=trace,jit+compilation=debug"
LOG_DECORATORS="level,tags,time,uptime"

log() {
    echo "[$(date '+%H:%M:%S')] $*" >&2
}

# Prints the home directory of the newest JDK matching the glob, or nothing.
find_java_home() {
    local glob="$1"
    local dir
    dir="$(ls -d "$JDK_ROOT"/$glob 2>/dev/null | sort -V | tail -1 || true)"
    if [ -z "$dir" ]; then
        return 0
    fi
    if [ -d "$dir/Contents/Home" ]; then
        echo "$dir/Contents/Home"
    else
        echo "$dir"
    fi
}

# Prints the feature version (8, 11, 17, ...) of the given java binary.
java_major() {
    local version
    version="$("$1" -version 2>&1 | head -1 | sed -E 's/.*version "([^"]+)".*/\1/')"
    case "$version" in
        1.*) echo "$version" | cut -d. -f2 ;;
        *) echo "$version" | cut -d. -f1 | cut -d- -f1 ;;
    esac
}

gc_flags() {
    local collector="$1" major="$2"
    case "$collector" in
        parallel) echo "-XX:+UseParallelGC" ;;
        g1) echo "-XX:+UseG1GC" ;;
        zgc)
            # ZGC is experimental before JDK 15 (and available on Linux x64 only in JDK 11)
            if [ "$major" -lt 15 ]; then
                echo "-XX:+UnlockExperimentalVMOptions -XX:+UseZGC"
            else
                echo "-XX:+UseZGC"
            fi
            ;;
        shenandoah) echo "-XX:+UseShenandoahGC" ;;
        shenandoah-generational) echo "-XX:+UseShenandoahGC -XX:ShenandoahGCMode=generational" ;;
        *) log "Unknown collector: $collector"; return 1 ;;
    esac
}

# String deduplication: G1 and Shenandoah always, other collectors since JDK 18.
dedup_flags() {
    local collector="$1" major="$2"
    case "$collector" in
        g1|shenandoah*) echo "-XX:+UseStringDeduplication" ;;
        *) if [ "$major" -ge 18 ]; then echo "-XX:+UseStringDeduplication"; fi ;;
    esac
}

# Unified logging selection for the given JDK: tags were renamed or removed over time and the JVM warns about
# selections that match nothing.
log_selection() {
    local major="$1"
    local selection="$COMMON_SELECTION"
    if [ "$major" -le 19 ]; then
        # The code cache sweeper was removed in JDK 20
        selection="$selection,codecache+sweep*=trace"
    fi
    if [ "$major" -le 16 ]; then
        selection="$selection,gc+stringdedup=debug"
    else
        # String deduplication was reimplemented in JDK 17 and got its own tags
        selection="$selection,stringdedup*=debug"
    fi
    echo "$selection"
}

# Prints JVM logging options. JDK 8 writes everything (GC log and safepoint statistics) to stdout, so the whole
# stdout becomes the sample; newer JDKs write to the file given as the second argument.
log_flags() {
    local major="$1" output="$2" dedup="$3"
    if [ "$major" -eq 8 ]; then
        local flags="-XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution"
        flags="$flags -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCApplicationConcurrentTime"
        flags="$flags -XX:+PrintSafepointStatistics -XX:PrintSafepointStatisticsCount=1"
        if [ -n "$dedup" ]; then
            flags="$flags -XX:+PrintStringDeduplicationStatistics"
        fi
        echo "$flags"
    else
        echo "-Xlog:$(log_selection "$major"):file=$output:$LOG_DECORATORS:filecount=0"
    fi
}

wait_for_slot() {
    while [ "$(jobs -rp | wc -l | tr -d ' ')" -ge "$PARALLEL_RUNS" ]; do
        sleep 1
    done
}

# Runs the workload once and compresses the log. Arguments: label, java binary, feature version, collector, locale.
run_sample() {
    local label="$1" java="$2" major="$3" collector="$4" locale="$5"
    local name="$label-$collector-$locale-$PLATFORM"
    local file="$label/$collector-$locale-$PLATFORM.log"
    local output="$OUTPUT_DIR/$file"
    local dedup flags
    dedup="$(dedup_flags "$collector" "$major")"
    flags="$HEAP_FLAGS $(gc_flags "$collector" "$major") $dedup $(log_flags "$major" "$output" "$dedup")"
    # Collapse the double space left by an empty dedup flag
    flags="$(echo "$flags" | tr -s ' ')"

    mkdir -p "$OUTPUT_DIR/$label"
    rm -f "$output" "$output.gz"

    local stdout="$RUN_DIR/$name.stdout"
    if [ "$major" -eq 8 ]; then
        stdout="$output"
    fi
    local status=0
    # The decimal separator in JVM logs follows the C locale of the process
    env LC_ALL="$locale.UTF-8" LANG="$locale.UTF-8" "$java" $flags -jar "$JAR" "--scale=$SCALE" \
        > "$stdout" 2> "$RUN_DIR/$name.stderr" || status=$?
    if [ "$status" -ne 0 ]; then
        log "FAILED $name (exit code $status), see $RUN_DIR/$name.stderr"
        return 0
    fi

    local lines options
    lines="$(wc -l < "$output" | tr -d ' ')"
    gzip -9 -f "$output"
    # The manifest shows the log file as <file> instead of the absolute path of this machine
    options="$(echo "$flags" | sed "s|$output|<file>|g")"
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$file.gz" "$PLATFORM" "$(uname -sm)" "$label" "$major" \
        "$(cat "$RUN_DIR/$label.build")" "$collector" "$locale" "$lines" "$options" \
        > "$RUN_DIR/$name.row"
    log "Finished $name ($lines lines, $(du -h "$output.gz" | cut -f1 | tr -d ' ') compressed)"
}

# Merges rows of this run into the manifest: rows for the same files are replaced, rows of files that no longer
# exist are dropped, everything else is kept.
update_manifest() {
    local new_rows="$RUN_DIR/new-rows.tsv"
    cat "$RUN_DIR"/*.row > "$new_rows" 2>/dev/null || true
    {
        echo "$MANIFEST_HEADER"
        {
            if [ -f "$MANIFEST" ]; then
                tail -n +2 "$MANIFEST" | awk -F'\t' -v rows="$new_rows" '
                    BEGIN { while ((getline line < rows) > 0) { split(line, f, "\t"); replaced[f[1]] = 1 } }
                    !($1 in replaced)' | while IFS= read -r row; do
                    if [ -f "$OUTPUT_DIR/${row%%	*}" ]; then
                        echo "$row"
                    fi
                done
            fi
            cat "$new_rows"
        } | sort -t '	' -k1,1V
    } > "$MANIFEST.tmp"
    mv "$MANIFEST.tmp" "$MANIFEST"
    log "Updated $MANIFEST ($(($(wc -l < "$MANIFEST") - 1)) samples)"
}

write_readme() {
    local readme="$OUTPUT_DIR/README.md"
    [ -f "$MANIFEST" ] || { log "Missing $MANIFEST"; return 1; }
    {
        echo "# GC log samples"
        echo
        echo "Generated by \`gc-log-generator/generate-samples.sh\` (macOS) and"
        echo "\`gc-log-generator/generate-samples-linux.sh\` (Linux in Docker); last update $(date '+%Y-%m-%d')."
        echo "Every file is a single gzipped run of the \`gc-log-generator\` workload (about 35 s, fixed 512 MB heap)."
        echo "File names are \`<jdk>/<collector>-<locale>-<platform>.log.gz\`. Collectors that a given JDK does not"
        echo "provide on a given platform are skipped. The exact JVM options of every file are in"
        echo "[\`samples.tsv\`](samples.tsv)."
        echo
        echo "Platforms:"
        echo
        tail -n +2 "$MANIFEST" | awk -F'\t' '!seen[$2]++ { printf "- `%s`: %s\n", $2, $3 }'
        echo
        echo "## Files"
        echo
        echo "| File | Platform | JDK build | Collector | Locale | Lines | Size (gzip) |"
        echo "|---|---|---|---|---|---|---|"
        local file platform system jdk major build collector locale lines options
        while IFS='	' read -r file platform system jdk major build collector locale lines options; do
            echo "| [\`$file\`]($file) | $platform | $build | $collector | $locale | $lines" \
                "| $(du -h "$OUTPUT_DIR/$file" | cut -f1 | tr -d ' ') |"
        done < <(tail -n +2 "$MANIFEST")
        echo
        echo "## JVM options"
        echo
        echo "Common: \`$HEAP_FLAGS\` and one of \`-XX:+UseParallelGC\`, \`-XX:+UseG1GC\`, \`-XX:+UseZGC\`,"
        echo "\`-XX:+UseShenandoahGC\`, \`-XX:+UseShenandoahGC -XX:ShenandoahGCMode=generational\`."
        echo "\`-XX:+UseStringDeduplication\` is added for G1 and Shenandoah, and since JDK 18 for every collector."
        echo "ZGC is generational on JDK 25+ (the only mode left) and non-generational on JDK 17."
        echo
        echo "Logging:"
        echo
        local label_major
        for label_major in $(tail -n +2 "$MANIFEST" | awk -F'\t' '{ print $4 "=" $5 }' | sort -u -t= -k2,2n); do
            jdk="${label_major%%=*}"
            major="${label_major#*=}"
            echo "- **$jdk**:"
            echo
            echo '  ```'
            if [ "$major" -eq 8 ]; then
                echo "  $(log_flags 8 "" "")"
                echo "  -XX:+PrintStringDeduplicationStatistics   (G1 only)"
                echo "  stdout is the log file"
            else
                echo "  -Xlog:$(log_selection "$major"):file=<file>:$LOG_DECORATORS:filecount=0"
            fi
            echo '  ```'
            echo
        done
        echo "## Workload"
        echo
        echo "Background allocation from three threads (objects with short, medium and long lifetimes, duplicated"
        echo "strings) runs all the time, while these scenarios run one after another:"
        echo
        echo "1. warm-up - steady young collections, aging and tenuring,"
        echo "2. humongous allocations - large primitive and reference arrays, some of them kept alive,"
        echo "3. old generation growth and churn - live set of ~45% of the heap that keeps changing (G1 concurrent"
        echo "   cycles and mixed collections, Parallel full collections, ZGC/Shenandoah cycles),"
        echo "4. class loading and unloading - 4500 classes defined in separate class loaders, some of them JIT"
        echo "   compiled, then dropped (Metadata GC Threshold, class unloading, code cache sweeper on JDK 19 and older),"
        echo "5. thread churn - 200 short lived threads and an expiring thread pool,"
        echo "6. JIT deoptimization - broken type profiles and an unstable if (made not entrant),"
        echo "7. safepoint operations - thread dumps, deadlock detection, biased lock revocation, System.gc(),"
        echo "   while a counted loop delays reaching the safepoint,"
        echo "8. memory pressure - heap filled with live data, then short bursts of objects that stay reachable"
        echo "   (to-space exhausted, full collections, allocation stalls, degenerated cycles, handled OutOfMemoryErrors),"
        echo "9. cool-down - everything released, final System.gc()."
        echo
        echo "## Locale"
        echo
        echo "HotSpot formats numbers in logs with the C library, so the decimal separator follows \`LC_ALL\`:"
        echo "\`en_US\` files use dots, \`pl_PL\` files use commas (\`[1,234s]\`, \`12,345ms\`). Date stamps (the \`time\`"
        echo "decorator, \`-XX:+PrintGCDateStamps\`) always use a dot. In JDK 11+ files the first few hundred lines are"
        echo "written before the JVM initializes the locale, so \`pl_PL\` files mix both separators; JDK 8 logs its first"
        echo "GC event after that point, so its \`pl_PL\` files use commas only."
        echo
        echo "## Regenerating"
        echo
        echo '```shell'
        echo "gc-log-generator/generate-samples.sh          # on macOS"
        echo "gc-log-generator/generate-samples-linux.sh    # Linux, in Docker"
        echo "# or a subset, e.g.:"
        echo "JDKS='jdk17=corretto-17.*' COLLECTORS='g1 zgc' LOCALES=en_US gc-log-generator/generate-samples.sh"
        echo '```'
    } > "$readme"
    log "Written $readme"
}

main() {
    if [ "$README_ONLY" = "1" ]; then
        write_readme
        return
    fi
    if [ "$SKIP_BUILD" != "1" ]; then
        log "Building gc-log-generator"
        (cd "$PROJECT_DIR" && mvn -q -pl gc-log-generator package -DskipTests)
    fi
    [ -f "$JAR" ] || { log "Missing $JAR"; exit 1; }
    rm -rf "$RUN_DIR"
    mkdir -p "$RUN_DIR" "$OUTPUT_DIR"
    log "Platform: $PLATFORM ($(uname -sm))"

    local label_pattern label glob home java major collector locale
    for label_pattern in $JDKS; do
        label="${label_pattern%%=*}"
        glob="${label_pattern#*=}"
        home="$(find_java_home "$glob")"
        if [ -z "$home" ]; then
            log "Skipping $label: no JDK matching $JDK_ROOT/$glob"
            continue
        fi
        java="$home/bin/java"
        major="$(java_major "$java")"
        "$java" -version 2>&1 | sed -n 2p | sed -E 's/.*\(build ([^)]+)\).*/\1/' > "$RUN_DIR/$label.build"
        log "$label: $home (feature version $major)"

        for collector in $COLLECTORS; do
            if ! "$java" $(gc_flags "$collector" "$major") -version > /dev/null 2>&1; then
                log "Skipping $label $collector: not supported by this JDK"
                continue
            fi
            for locale in $LOCALES; do
                wait_for_slot
                log "Starting $label-$collector-$locale-$PLATFORM"
                run_sample "$label" "$java" "$major" "$collector" "$locale" &
            done
        done
    done
    wait
    update_manifest
    write_readme
}

main "$@"
