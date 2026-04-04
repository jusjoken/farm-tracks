#!/usr/bin/env bash
# deploy-local-jar.sh
# Copy an already-built production JAR locally and restart the app container.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_CONFIG_FILE="$SCRIPT_DIR/local-deploy.conf"

JAR_SRC="target/farm-tracks-1.0-SNAPSHOT.jar"
CONFIG_FILE="$DEFAULT_CONFIG_FILE"

while [ "$#" -gt 0 ]; do
    case "$1" in
        --config)
            shift
            [ "$#" -gt 0 ] || { echo "Missing value for --config"; exit 1; }
            CONFIG_FILE="$1"
            ;;
        --help|-h)
            echo "Usage: $0 [jar-path] [--config path]"
            exit 0
            ;;
        *)
            JAR_SRC="$1"
            ;;
    esac
    shift
done

if [ -f "$CONFIG_FILE" ]; then
    # shellcheck source=/dev/null
    source "$CONFIG_FILE"
fi

if [ ! -f "$JAR_SRC" ]; then
    echo "JAR not found: $JAR_SRC"
    echo "Build first with ./build-prod.sh"
    exit 1
fi

LOCAL_APP_JAR="${LOCAL_APP_JAR:-/home/birch/appdata/farmtracks/app.jar}"
LOCAL_APP_CONTAINER="${LOCAL_APP_CONTAINER:-farmtracks-app}"
LOCAL_STARTUP_TIMEOUT_SEC="${LOCAL_STARTUP_TIMEOUT_SEC:-90}"
# Accept common Spring Boot startup lines by default.
LOCAL_STARTUP_LOG_REGEX="${LOCAL_STARTUP_LOG_REGEX:-Started Application|Started .* in [0-9.]+ seconds}"
# When true, fail if startup log/health signal is not detected in time.
LOCAL_VALIDATION_STRICT="${LOCAL_VALIDATION_STRICT:-false}"

cp -f "$JAR_SRC" "$LOCAL_APP_JAR"
echo "Copied JAR -> $LOCAL_APP_JAR"

docker restart "$LOCAL_APP_CONTAINER" >/dev/null
echo "Restarted container: $LOCAL_APP_CONTAINER"

POLL_INTERVAL_SEC=5
MAX_POLLS=$((LOCAL_STARTUP_TIMEOUT_SEC / POLL_INTERVAL_SEC))
if [ "$MAX_POLLS" -lt 1 ]; then
    MAX_POLLS=1
fi

echo "Waiting for local app startup (up to ${LOCAL_STARTUP_TIMEOUT_SEC}s)"
for i in $(seq 1 "$MAX_POLLS"); do
    sleep 5
    HEALTH_STATUS=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$LOCAL_APP_CONTAINER" 2>/dev/null || echo "unknown")
    if [ "$HEALTH_STATUS" = "healthy" ]; then
        echo "Local application is healthy"
        exit 0
    fi

    if docker logs "$LOCAL_APP_CONTAINER" 2>&1 | grep -E -q "$LOCAL_STARTUP_LOG_REGEX"; then
        echo "Local application started successfully"
        exit 0
    fi
done

echo "Timed out waiting for startup. Check logs:"
echo "docker logs $LOCAL_APP_CONTAINER"
echo "Validation pattern: $LOCAL_STARTUP_LOG_REGEX"

RUNNING_STATUS=$(docker inspect --format '{{.State.Running}}' "$LOCAL_APP_CONTAINER" 2>/dev/null || echo "false")
if [ "$RUNNING_STATUS" = "true" ] && [ "$LOCAL_VALIDATION_STRICT" != "true" ]; then
    echo "Container is running; continuing despite missing startup signal (LOCAL_VALIDATION_STRICT=false)."
    exit 0
fi

exit 1
