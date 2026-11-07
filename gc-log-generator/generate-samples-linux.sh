#!/usr/bin/env bash
#
# Generates the Linux samples: builds the workload jar on the host, builds a Docker image with the same Corretto
# versions (docker/Dockerfile) and runs generate-samples.sh inside a container with the project mounted. The
# samples land in the same directory as the macOS ones, with "linux" in their names.
#
# Usage: gc-log-generator/generate-samples-linux.sh
#
# Environment variables (all optional):
#   IMAGE          name of the Docker image (default: gc-log-generator-linux)
#   SKIP_BUILD     set to 1 to reuse an already built jar
#   JDKS, COLLECTORS, LOCALES, PARALLEL_RUNS, SCALE are passed to generate-samples.sh (see there)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
IMAGE="${IMAGE:-gc-log-generator-linux}"

if [ "${SKIP_BUILD:-0}" != "1" ]; then
    (cd "$PROJECT_DIR" && mvn -q -pl gc-log-generator package -DskipTests)
fi

docker build -t "$IMAGE" "$SCRIPT_DIR/docker"

# Variables given without a value are passed only when they are set on the host
docker run --rm \
    -v "$PROJECT_DIR:/work" -w /work \
    -e SKIP_BUILD=1 -e PLATFORM=linux \
    -e JDKS -e COLLECTORS -e LOCALES -e PARALLEL_RUNS -e SCALE \
    "$IMAGE" gc-log-generator/generate-samples.sh
