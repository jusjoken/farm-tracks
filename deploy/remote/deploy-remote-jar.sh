#!/usr/bin/env bash
# deploy-remote-jar.sh
# Copy an already-built production JAR to a remote host and restart the app container.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_CONFIG_FILE="$SCRIPT_DIR/remote-deploy.conf"

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

: "${REMOTE_HOST:?Set REMOTE_HOST}"
: "${REMOTE_USER:?Set REMOTE_USER}"

REMOTE_APP_JAR="${REMOTE_APP_JAR:-/home/birch/appdata/farmtracks/app.jar}"
REMOTE_APP_CONTAINER="${REMOTE_APP_CONTAINER:-farmtracks-app}"
REMOTE_STARTUP_TIMEOUT_SEC="${REMOTE_STARTUP_TIMEOUT_SEC:-60}"
REMOTE_STARTUP_LOG_REGEX="${REMOTE_STARTUP_LOG_REGEX:-Started Application}"
# Optional override, e.g. "sudo -n docker".
REMOTE_DOCKER_CMD="${REMOTE_DOCKER_CMD:-}"

SSH_PORT="${SSH_PORT:-22}"
SSH_KEY_PATH="${SSH_KEY_PATH:-}"

SSH_OPTS=("-p" "$SSH_PORT")
SCP_OPTS=("-P" "$SSH_PORT")
if [ -n "$SSH_KEY_PATH" ]; then
    SSH_OPTS+=("-i" "$SSH_KEY_PATH")
    SCP_OPTS+=("-i" "$SSH_KEY_PATH")
fi

REMOTE_TMP_JAR="${REMOTE_APP_JAR}.upload"

printf -v ESC_REMOTE_DOCKER_CMD '%q' "$REMOTE_DOCKER_CMD"

echo "Checking remote Docker access"
ssh "${SSH_OPTS[@]}" "${REMOTE_USER}@${REMOTE_HOST}" \
    "REMOTE_DOCKER_CMD=${ESC_REMOTE_DOCKER_CMD} bash -s" <<'EOF'
set -euo pipefail

if [ -n "${REMOTE_DOCKER_CMD}" ]; then
    if ! eval "${REMOTE_DOCKER_CMD} info >/dev/null 2>&1"; then
        echo "Configured REMOTE_DOCKER_CMD does not work: ${REMOTE_DOCKER_CMD}"
        exit 1
    fi
elif docker info >/dev/null 2>&1; then
    :
elif sudo -n docker info >/dev/null 2>&1; then
    :
else
    echo "Remote Docker access check failed before upload."
    echo "Fix one of these on remote host:"
    echo "- Add user to docker group (then re-login)"
    echo "- Or allow passwordless sudo for docker and set REMOTE_DOCKER_CMD='sudo -n docker'"
    exit 1
fi
EOF

echo "Copying JAR to ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_TMP_JAR}"
scp "${SCP_OPTS[@]}" "$JAR_SRC" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_TMP_JAR}"

echo "Promoting JAR and restarting remote container"
printf -v ESC_REMOTE_TMP_JAR '%q' "$REMOTE_TMP_JAR"
printf -v ESC_REMOTE_APP_JAR '%q' "$REMOTE_APP_JAR"
printf -v ESC_REMOTE_APP_CONTAINER '%q' "$REMOTE_APP_CONTAINER"
printf -v ESC_REMOTE_STARTUP_TIMEOUT_SEC '%q' "$REMOTE_STARTUP_TIMEOUT_SEC"
printf -v ESC_REMOTE_STARTUP_LOG_REGEX '%q' "$REMOTE_STARTUP_LOG_REGEX"

ssh "${SSH_OPTS[@]}" "${REMOTE_USER}@${REMOTE_HOST}" \
    "REMOTE_TMP_JAR=${ESC_REMOTE_TMP_JAR} REMOTE_APP_JAR=${ESC_REMOTE_APP_JAR} REMOTE_APP_CONTAINER=${ESC_REMOTE_APP_CONTAINER} REMOTE_STARTUP_TIMEOUT_SEC=${ESC_REMOTE_STARTUP_TIMEOUT_SEC} REMOTE_STARTUP_LOG_REGEX=${ESC_REMOTE_STARTUP_LOG_REGEX} REMOTE_DOCKER_CMD=${ESC_REMOTE_DOCKER_CMD} bash -s" <<'EOF'
set -euo pipefail

if [ -n "${REMOTE_DOCKER_CMD}" ]; then
    DOCKER_CMD="${REMOTE_DOCKER_CMD}"
elif docker info >/dev/null 2>&1; then
    DOCKER_CMD="docker"
elif sudo -n docker info >/dev/null 2>&1; then
    DOCKER_CMD="sudo -n docker"
else
    echo "Remote Docker access failed for user '${USER}'."
    echo "Fix one of these on remote host:"
    echo "- Add user to docker group (then re-login)"
    echo "- Or set REMOTE_DOCKER_CMD='sudo -n docker' in remote deploy config"
    exit 1
fi

run_docker() {
    local quoted=()
    local arg
    for arg in "$@"; do
        printf -v arg '%q' "$arg"
        quoted+=("$arg")
    done
    eval "$DOCKER_CMD ${quoted[*]}"
}

mv -f "$REMOTE_TMP_JAR" "$REMOTE_APP_JAR"
run_docker restart "$REMOTE_APP_CONTAINER" >/dev/null

echo "Waiting for remote app startup (up to ${REMOTE_STARTUP_TIMEOUT_SEC}s)"
checks=$(( (REMOTE_STARTUP_TIMEOUT_SEC + 4) / 5 ))
for _ in $(seq 1 "$checks"); do
    sleep 5
    if run_docker logs "$REMOTE_APP_CONTAINER" 2>&1 | grep -q "$REMOTE_STARTUP_LOG_REGEX"; then
        echo "Remote application started successfully"
        exit 0
    fi
done

echo "Timed out waiting for startup logs matching: ${REMOTE_STARTUP_LOG_REGEX}"
echo "Run this on remote for details:"
echo "${DOCKER_CMD} logs ${REMOTE_APP_CONTAINER}"
exit 1
EOF
