#!/bin/sh
set -eo pipefail
PKCS11_CFG="$1"
AAB_NAME="${2:-fenris-authenticator.aab}"
cp app/build/outputs/bundle/release/app-release.aab "$AAB_NAME"
jarsigner \
  -J--add-exports=jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED \
  -keystore NONE \
  -storetype PKCS11 \
  -providerClass sun.security.pkcs11.SunPKCS11 \
  -providerArg "$PKCS11_CFG" \
  "$AAB_NAME" \
  "X.509 Certificate for Retired Key 2"
