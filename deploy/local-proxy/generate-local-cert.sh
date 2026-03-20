#!/usr/bin/env bash
set -euo pipefail

CERT_DIR="${1:-/home/birch/appdata/farmtracks/local-certs}"
mkdir -p "$CERT_DIR"

openssl req -x509 -nodes -newkey rsa:2048 \
  -keyout "$CERT_DIR/privkey.pem" \
  -out "$CERT_DIR/fullchain.pem" \
  -days 365 \
  -subj "/C=CA/ST=Local/L=Local/O=FarmTracks/OU=Dev/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

echo "Generated:"
echo "  $CERT_DIR/fullchain.pem"
echo "  $CERT_DIR/privkey.pem"
