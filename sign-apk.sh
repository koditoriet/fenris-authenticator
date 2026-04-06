#!/bin/sh
set -eo pipefail
PKCS11_CFG="$1"
APK_NAME="${2:-fenris-authenticator.apk}"
apksigner \
    "-J-add-exports jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED" \
    sign --ks NONE \
    --provider-class sun.security.pkcs11.SunPKCS11 \
    --provider-arg "$PKCS11_CFG" \
    --ks-type PKCS11 \
    --min-sdk-version 31 \
    --max-sdk-version 36 \
    --in app/build/outputs/apk/release/app-release-unsigned.apk \
    --out "$APK_NAME"
