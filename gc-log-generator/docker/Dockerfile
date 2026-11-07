# Linux environment for generate-samples.sh: the same Corretto builds as on the developer machine and the
# locales the samples are generated with. The project directory is mounted at runtime, nothing is copied in.
FROM debian:bookworm-slim

# Exact Corretto versions (as in IMPLEMENTOR_VERSION of the JDK release file)
ARG CORRETTO_VERSIONS="8.452.09.1 11.0.27.6.1 17.0.15.6.1 25.0.3.9.1 27.0.1.0.11"

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl locales \
    && rm -rf /var/lib/apt/lists/* \
    && sed -i -e 's/^# *\(en_US.UTF-8 UTF-8\)/\1/' -e 's/^# *\(pl_PL.UTF-8 UTF-8\)/\1/' /etc/locale.gen \
    && locale-gen

# JDK directories get the same names as on macOS (corretto-1.8.0_452, corretto-17.0.15, ...), so the default
# JDKS globs of generate-samples.sh work unchanged
RUN set -eu; \
    case "$(uname -m)" in \
        aarch64) arch=aarch64 ;; \
        x86_64) arch=x64 ;; \
        *) echo "Unsupported architecture: $(uname -m)"; exit 1 ;; \
    esac; \
    mkdir -p /opt/jdks; \
    for version in $CORRETTO_VERSIONS; do \
        case "$version" in \
            8.*) name="corretto-1.8.0_$(echo "$version" | cut -d. -f2)" ;; \
            *) name="corretto-$(echo "$version" | cut -d. -f1-3)" ;; \
        esac; \
        curl -fsSL "https://corretto.aws/downloads/resources/$version/amazon-corretto-$version-linux-$arch.tar.gz" \
            -o /tmp/jdk.tar.gz; \
        mkdir "/opt/jdks/$name"; \
        tar -xzf /tmp/jdk.tar.gz -C "/opt/jdks/$name" --strip-components=1; \
        rm /tmp/jdk.tar.gz; \
        "/opt/jdks/$name/bin/java" -version; \
    done

ENV JDK_ROOT=/opt/jdks
