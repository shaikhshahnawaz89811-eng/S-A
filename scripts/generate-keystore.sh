#!/bin/bash
# SA Companion — Generate a release keystore for signing APKs
# Run this once, then store the keystore securely.
# For GitHub Actions CI, base64-encode it and add to secrets.

set -e

KEYSTORE_PATH="keystore/release.jks"
KEY_ALIAS="sa-companion"
KEY_PASSWORD="change_me_secure_password"
STORE_PASSWORD="change_me_secure_password"
VALIDITY_DAYS=10000

mkdir -p keystore

echo "=== Generating SA Companion Release Keystore ==="
keytool -genkeypair \
  -v \
  -storetype PKCS12 \
  -keystore "$KEYSTORE_PATH" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -dname "CN=SA Companion, OU=Mobile, O=SA Systems, L=India, ST=India, C=IN"

echo ""
echo "=== Keystore created at: $KEYSTORE_PATH ==="
echo ""
echo "=== Base64 encode for GitHub Secrets: ==="
base64 -i "$KEYSTORE_PATH"
echo ""
echo "Add the following to GitHub Secrets:"
echo "  KEYSTORE_BASE64  = (above base64 output)"
echo "  KEYSTORE_PASSWORD = $STORE_PASSWORD"
echo "  KEY_ALIAS         = $KEY_ALIAS"
echo "  KEY_PASSWORD      = $KEY_PASSWORD"
echo "  GROQ_API_KEY      = your_groq_api_key_here"
echo ""
echo "IMPORTANT: Change the passwords above before using in production!"
