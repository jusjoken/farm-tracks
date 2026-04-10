#!/usr/bin/env bash
# deploy-remote-jar.sh
# Copy an already-built production JAR to a remote host and restart the app container.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_CONFIG_FILE="$SCRIPT_DIR/remote-deploy.conf"

JAR_SRC="target/farm-tracks-1.0-SNAPSHOT.jar"
CONFIG_FILE="$DEFAULT_CONFIG_FILE"
PUBLISH_GITHUB_RELEASE=""
CLI_RELEASE_TAG=""

while [ "$#" -gt 0 ]; do
    case "$1" in
        --config)
            shift
            [ "$#" -gt 0 ] || { echo "Missing value for --config"; exit 1; }
            CONFIG_FILE="$1"
            ;;
        --publish-release)
            PUBLISH_GITHUB_RELEASE="true"
            ;;
        --release-tag)
            shift
            [ "$#" -gt 0 ] || { echo "Missing value for --release-tag"; exit 1; }
            CLI_RELEASE_TAG="$1"
            ;;
        --help|-h)
            echo "Usage: $0 [jar-path] [--config path] [--publish-release] [--release-tag tag]"
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

resolve_default_release_tag() {
    local app_major="${APP_VERSION_MAJOR:-}"
    local commit_count=""

    if [ -z "$app_major" ] && [ -f "src/main/resources/application.properties" ]; then
        app_major="$(sed -n 's/^app\.version\.major=\${APP_VERSION_MAJOR:\([0-9][0-9]*\)}$/\1/p' src/main/resources/application.properties | head -n1)"
    fi
    if [ -z "$app_major" ]; then
        app_major="1"
    fi

    if git rev-parse --git-dir >/dev/null 2>&1; then
        commit_count="$(git rev-list --count HEAD 2>/dev/null || true)"
    fi

    if [ -z "$commit_count" ] && [ -f "target/classes/git.properties" ]; then
        commit_count="$(sed -n 's/^git\.total\.commit\.count=\([0-9][0-9]*\)$/\1/p' target/classes/git.properties | head -n1)"
    fi

    if [ -z "$commit_count" ]; then
        commit_count="0"
    fi

    printf 'v%s.%s' "$app_major" "$commit_count"
}

if [ ! -f "$JAR_SRC" ]; then
    echo "JAR not found: $JAR_SRC"
    echo "Build first with ./build-prod.sh"
    exit 1
fi

: "${REMOTE_HOST:?Set REMOTE_HOST}"
: "${REMOTE_USER:?Set REMOTE_USER}"

PUBLISH_GITHUB_RELEASE="${PUBLISH_GITHUB_RELEASE:-${GITHUB_RELEASE_ENABLED:-false}}"
GITHUB_RELEASE_TAG="${CLI_RELEASE_TAG:-${GITHUB_RELEASE_TAG:-}}"
GITHUB_RELEASE_REPO="${GITHUB_RELEASE_REPO:-}"
GITHUB_RELEASE_TARGET="${GITHUB_RELEASE_TARGET:-}"
GITHUB_RELEASE_DRAFT="${GITHUB_RELEASE_DRAFT:-false}"
GITHUB_RELEASE_PRERELEASE="${GITHUB_RELEASE_PRERELEASE:-false}"

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

if [ "$PUBLISH_GITHUB_RELEASE" = "true" ]; then
    if ! command -v gh >/dev/null 2>&1; then
        echo "GitHub CLI not found. Install 'gh' to use --publish-release."
        exit 1
    fi

    : "${GITHUB_RELEASE_REPO:?Set GITHUB_RELEASE_REPO (example: owner/repo)}"

    if [ -z "$GITHUB_RELEASE_TAG" ]; then
        GITHUB_RELEASE_TAG="$(resolve_default_release_tag)"
    fi

    if ! gh auth status >/dev/null 2>&1; then
        echo "GitHub CLI is not authenticated. Run 'gh auth login' first."
        exit 1
    fi

    SAFE_RELEASE_TAG="${GITHUB_RELEASE_TAG//\//-}"
    CHECKSUM_FILE="$(mktemp)"
    trap 'rm -f "$CHECKSUM_FILE"' EXIT

    sha256sum "$JAR_SRC" | awk '{print $1}' > "$CHECKSUM_FILE"

    RELEASE_JAR_ASSET="farm-tracks-${SAFE_RELEASE_TAG}.jar"
    RELEASE_SHA_ASSET="${RELEASE_JAR_ASSET}.sha256"
    RELEASE_TITLE="Farm Tracks ${GITHUB_RELEASE_TAG}"
    RELEASE_NOTES="Automated release published before remote deploy."

    if gh release view "$GITHUB_RELEASE_TAG" --repo "$GITHUB_RELEASE_REPO" >/dev/null 2>&1; then
        echo "GitHub release '$GITHUB_RELEASE_TAG' already exists; uploading assets with --clobber"
        gh release upload "$GITHUB_RELEASE_TAG" \
            "$JAR_SRC#$RELEASE_JAR_ASSET" \
            "$CHECKSUM_FILE#$RELEASE_SHA_ASSET" \
            --clobber \
            --repo "$GITHUB_RELEASE_REPO"
    else
        CREATE_ARGS=(
            release create "$GITHUB_RELEASE_TAG"
            "$JAR_SRC#$RELEASE_JAR_ASSET"
            "$CHECKSUM_FILE#$RELEASE_SHA_ASSET"
            --repo "$GITHUB_RELEASE_REPO"
            --title "$RELEASE_TITLE"
            --notes "$RELEASE_NOTES"
        )

        if [ -n "$GITHUB_RELEASE_TARGET" ]; then
            CREATE_ARGS+=(--target "$GITHUB_RELEASE_TARGET")
        fi

        if [ "$GITHUB_RELEASE_DRAFT" = "true" ]; then
            CREATE_ARGS+=(--draft)
        fi
        if [ "$GITHUB_RELEASE_PRERELEASE" = "true" ]; then
            CREATE_ARGS+=(--prerelease)
        fi

        echo "Creating GitHub release '$GITHUB_RELEASE_TAG' in $GITHUB_RELEASE_REPO"
        gh "${CREATE_ARGS[@]}"
    fi

    echo "Published release asset to GitHub: $GITHUB_RELEASE_REPO ($GITHUB_RELEASE_TAG)"
fi

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
