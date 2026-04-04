#!/usr/bin/env bash
# build-prod.sh — Repeatable production build for farm-tracks
#
# WHY TWO STEPS:
#   spire.doc.free v5.3.2 contains a class (com.spire.doc.packages.sprxyl) that
#   tries to extend PNGImageWriter, which is final in Java 21. This crashes
#   Vaadin's build-frontend goal when spire is on the compile classpath.
#
#   Work-around: run vaadin:build-frontend with the vaadin-frontend-fix profile
#   (which moves spire to test scope), then package with the production profile
#   but skip the frontend build (already done in step 1).
#
# USAGE:
#   ./build-prod.sh              # build only (reuses cached bundle if available)
#   ./build-prod.sh --clean      # build with fresh frontend (deletes cached bundle)
#   ./build-prod.sh --deploy     # build + deploy local Portainer-style container
#   ./build-prod.sh --clean --deploy  # fresh build + local deploy
#   ./build-prod.sh --skip-build --deploy  # deploy existing JAR only

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JAR_SRC="target/farm-tracks-1.0-SNAPSHOT.jar"
LOCAL_DEPLOY_SCRIPT="./deploy/local/deploy-local-jar.sh"

# Parse command-line arguments
CLEAN=false
DEPLOY=false
SKIP_BUILD=false
for arg in "$@"; do
    case "$arg" in
        --clean)  CLEAN=true ;;
        --deploy) DEPLOY=true ;;
        --skip-build) SKIP_BUILD=true ;;
        *)        echo "Unknown argument: $arg"; exit 1 ;;
    esac
done

if [ "$SKIP_BUILD" = true ] && [ "$CLEAN" = true ]; then
    echo "--clean cannot be used with --skip-build"
    exit 1
fi

# ── 1. Handle root-owned target/ left behind by dev-mode inside Docker ─────────
if [ -d "target" ] && [ "$(stat -c '%u' target)" = "0" ]; then
    STUCK="target.stuck.$(date +%s)"
    echo "⚠  target/ is root-owned (left by Docker dev server). Moving to $STUCK ..."
    mv target "$STUCK"
    echo "   Done. Run 'sudo rm -rf $STUCK' to clean up later."
fi

# ── 2. Handle --clean flag: delete stale prod.bundle to force fresh Vite build ─
if [ "$CLEAN" = true ]; then
    BUNDLE="src/main/bundles/prod.bundle"
    if [ -f "$BUNDLE" ]; then
        echo "🗑  Deleting stale bundle → $BUNDLE"
        rm "$BUNDLE"
        echo "   Will do a full frontend rebuild (slower)"
    fi
fi

if [ "$SKIP_BUILD" = false ]; then
    # ── 3. Step 1: build Vaadin frontend (spire hidden via vaadin-frontend-fix) ─
    echo ""
    echo "══════════════════════════════════════════════════════════════"
    echo "  Step 1/2 — Vaadin frontend build"
    echo "══════════════════════════════════════════════════════════════"
    ./mvnw vaadin:build-frontend -Pproduction,vaadin-frontend-fix -DskipTests

    # ── 4. Step 2: compile Java + package JAR (frontend already built) ─────────
    echo ""
    echo "══════════════════════════════════════════════════════════════"
    echo "  Step 2/2 — Java compile + package (frontend already built)"
    echo "══════════════════════════════════════════════════════════════"
    ./mvnw package -Pproduction -DskipTests -Dvaadin.skip=true
else
    echo ""
    echo "══════════════════════════════════════════════════════════════"
    echo "  Skipping build steps (--skip-build)"
    echo "══════════════════════════════════════════════════════════════"
fi

# ── 5. Verify JAR was produced ─────────────────────────────────────────────────
if [ ! -f "$JAR_SRC" ]; then
    echo ""
    echo "✗  BUILD FAILED — JAR not found at $JAR_SRC"
    exit 1
fi
JAR_SIZE=$(du -sh "$JAR_SRC" | cut -f1)
echo ""
echo "✓  BUILD SUCCESS — $JAR_SRC ($JAR_SIZE)"

# ── 6. Optional deploy ─────────────────────────────────────────────────────────
if [ "$DEPLOY" = true ]; then
    echo ""
    echo "══════════════════════════════════════════════════════════════"
    echo "  Deploying to local container"
    echo "══════════════════════════════════════════════════════════════"

    "$LOCAL_DEPLOY_SCRIPT" "$JAR_SRC"
fi

echo ""
echo "Done."
