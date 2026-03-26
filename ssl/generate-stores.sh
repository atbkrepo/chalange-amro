#!/bin/bash

set -euo pipefail

# Regenerates a shared PKCS12 keystore and truststore used by compose services.
# Expected aliases:
#   - discovery (eureka TLS cert)
#   - config-server (config app TLS cert)
#   - auth (auth app TLS cert)
#   - orders (orders app TLS cert)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KEYSTORE_PATH="${SCRIPT_DIR}/keystore.p12"
TRUSTSTORE_PATH="${SCRIPT_DIR}/truststore.p12"
CERTS_DIR="${SCRIPT_DIR}/.certs"

STORE_PASSWORD="${SSL_KEY_STORE_PASSWORD:-changeit}"
KEY_ALG="${SSL_KEY_ALG:-RSA}"
KEY_SIZE="${SSL_KEY_SIZE:-2048}"
VALIDITY_DAYS="${SSL_VALIDITY_DAYS:-3650}"

mkdir -p "${CERTS_DIR}"
rm -f "${KEYSTORE_PATH}" "${TRUSTSTORE_PATH}" "${CERTS_DIR}"/*.cer

generate_alias() {
  local alias="$1"
  local cn="$2"
  local san="$3"

  keytool -genkeypair \
    -alias "${alias}" \
    -keyalg "${KEY_ALG}" \
    -keysize "${KEY_SIZE}" \
    -validity "${VALIDITY_DAYS}" \
    -storetype PKCS12 \
    -keystore "${KEYSTORE_PATH}" \
    -storepass "${STORE_PASSWORD}" \
    -keypass "${STORE_PASSWORD}" \
    -dname "CN=${cn}, OU=xyz, O=xyz, L=Amsterdam, ST=NH, C=NL" \
    -ext "SAN=${san}" \
    -noprompt

  keytool -exportcert \
    -alias "${alias}" \
    -keystore "${KEYSTORE_PATH}" \
    -storetype PKCS12 \
    -storepass "${STORE_PASSWORD}" \
    -rfc \
    -file "${CERTS_DIR}/${alias}.cer"

  keytool -importcert \
    -alias "${alias}" \
    -keystore "${TRUSTSTORE_PATH}" \
    -storetype PKCS12 \
    -storepass "${STORE_PASSWORD}" \
    -file "${CERTS_DIR}/${alias}.cer" \
    -noprompt
}

generate_alias "discovery" "discovery" "dns:discovery,dns:localhost,ip:127.0.0.1"
generate_alias "config-server" "config-server" "dns:config-server,dns:localhost,ip:127.0.0.1"
generate_alias "auth" "auth" "dns:auth-app,dns:auth,dns:localhost,ip:127.0.0.1"
generate_alias "orders" "orders" "dns:orders-app,dns:orders,dns:localhost,ip:127.0.0.1"

echo "Generated:"
echo "  ${KEYSTORE_PATH} (aliases: discovery, config-server, auth, orders)"
echo "  ${TRUSTSTORE_PATH} (trusted certs: discovery, config-server, auth, orders)"
