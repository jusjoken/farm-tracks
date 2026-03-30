#!/usr/bin/env bash
set -euo pipefail

CERT_DIR="${1:-/home/birch/appdata/farmtracks/local-certs}"
LAN_IP="${2:-}"
KEYSTORE_PASSWORD="${3:-}"
mkdir -p "$CERT_DIR"

SAN="DNS:localhost,IP:127.0.0.1"
if [[ -n "$LAN_IP" ]]; then
  SAN="$SAN,IP:$LAN_IP"
fi

# Generate PEM keypair for SWAG/nginx TLS termination.
openssl req -x509 -nodes -newkey rsa:2048 \
  -keyout "$CERT_DIR/privkey.pem" \
  -out "$CERT_DIR/fullchain.pem" \
  -days 365 \
  -subj "/C=CA/ST=Local/L=Local/O=FarmTracks/OU=Dev/CN=localhost" \
  -addext "subjectAltName=$SAN"

# Generate PKCS12 keystore for direct Spring Boot HTTPS runs.
openssl pkcs12 -export \
  -in "$CERT_DIR/fullchain.pem" \
  -inkey "$CERT_DIR/privkey.pem" \
  -name tomcat \
  -out "$CERT_DIR/farmtracks.p12" \
  -passout "pass:$KEYSTORE_PASSWORD"

echo "Generated:"
echo "  $CERT_DIR/fullchain.pem"
echo "  $CERT_DIR/privkey.pem"
echo "  $CERT_DIR/farmtracks.p12"
echo "  SAN=$SAN"
if [[ -n "$KEYSTORE_PASSWORD" ]]; then
  echo "  PKCS12 password=$KEYSTORE_PASSWORD"
else
  echo "  PKCS12 password=<empty>"
fi
