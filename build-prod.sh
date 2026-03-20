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
#   ./build-prod.sh            # build only
#   ./build-prod.sh --deploy   # build + deploy docker stack

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JAR_SRC="target/farm-tracks-1.0-SNAPSHOT.jar"
JAR_DST="/home/birch/appdata/farmtracks/app.jar"
COMPOSE_CMD="docker compose -f docker-compose.yml -f docker-compose.local-swag.yml"
CONTAINER="farmtracks"
DEPLOY="${1:-}"

# ── 1. Handle root-owned target/ left behind by dev-mode inside Docker ─────────
if [ -d "target" ] && [ "$(stat -c '%u' target)" = "0" ]; then
    STUCK="target.stuck.$(date +%s)"
    echo "⚠  target/ is root-owned (left by Docker dev server). Moving to $STUCK ..."
    mv target "$STUCK"
    echo "   Done. Run 'sudo rm -rf $STUCK' to clean up later."
fi

# ── 2. Step 1: build Vaadin frontend (spire hidden via vaadin-frontend-fix) ────
echo ""
echo "══════════════════════════════════════════════════════════════"
echo "  Step 1/2 — Vaadin frontend build"
echo "══════════════════════════════════════════════════════════════"
./mvnw vaadin:build-frontend -Pproduction,vaadin-frontend-fix -DskipTests

# ── 3. Step 2: compile Java + package JAR (spire on compile classpath) ─────────
echo ""
echo "══════════════════════════════════════════════════════════════"
echo "  Step 2/2 — Java compile + package (frontend already built)"
echo "══════════════════════════════════════════════════════════════"
./mvnw package -Pproduction -DskipTests -Dvaadin.skip=true

# ── 4. Verify JAR was produced ─────────────────────────────────────────────────
if [ ! -f "$JAR_SRC" ]; then
    echo ""
    echo "✗  BUILD FAILED — JAR not found at $JAR_SRC"
    exit 1
fi
JAR_SIZE=$(du -sh "$JAR_SRC" | cut -f1)
echo ""
echo "✓  BUILD SUCCESS — $JAR_SRC ($JAR_SIZE)"

# ── 5. Optional deploy ─────────────────────────────────────────────────────────
if [ "$DEPLOY" = "--deploy" ]; then
    echo ""
    echo "══════════════════════════════════════════════════════════════"
    echo "  Deploying to Docker stack"
    echo "══════════════════════════════════════════════════════════════"
    cp -f "$JAR_SRC" "$JAR_DST"
    echo "  Copied JAR → $JAR_DST"

    $COMPOSE_CMD up -d --force-recreate "$CONTAINER"
    echo ""
    echo "  Waiting for application startup (up to 60 s)..."
    for i in $(seq 1 12); do
        sleep 5
        if docker logs "${CONTAINER}-app" 2>&1 | grep -q "Started Application"; then
            echo "  ✓ Application started successfully"
            docker logs "${CONTAINER}-app" 2>&1 | grep "Started Application"
            break
        fi
        if [ "$i" -eq 12 ]; then
            echo "  ⚠  Timed out waiting — check logs:"
            echo "     docker logs ${CONTAINER}-app"
        fi
    done
fi

echo ""
echo "Done."
