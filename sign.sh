#!/bin/sh
set -eo pipefail
read -p "Enter Yubikey PIN: " -s PIN
apksigner \
    "-J-add-exports jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED" \
    sign --ks NONE \
    --ks-pass "pass:$PIN" \
    --provider-class sun.security.pkcs11.SunPKCS11 \
    --provider-arg ./signing-pkcs11-provider.cfg \
    --ks-type PKCS11 \
    --min-sdk-version 31 \
    --max-sdk-version 36 \
    --in app/build/outputs/apk/release/app-release-unsigned.apk \
    --out fenris-authenticator.apk
