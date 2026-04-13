#!/bin/sh
set -eo pipefail
APP_NAME="fenris-authenticator"
VERSION="$(./gradlew -q printVersionName)"
APK_NAME="${APP_NAME}_${VERSION}.apk"
AAB_NAME="${APP_NAME}_${VERSION}.aab"

if [ "$1" == "--no-clean" ] ; then
  echo "NOTE: not cleaning before build; build is NOT suitable for release!"
  CLEAN=""
else
  CLEAN="clean"
fi

./gradlew $CLEAN assembleRelease bundleRelease

# apksigner breaks if there's more than one key per PKCS11 slot,
# so we make sure to only use one "standard" yubikey slot and the opensc pkcs11
# provider, which can only see the standard slots, to hide the other
# signing key from apksigner.
./sign-apk.sh ./sign-apk.cfg "$APK_NAME"

# jarsigner, meanwhile, is not broken like apksigner, so we use the ykcs11
# provider and stick the aab upload signing key in a non-standard (i.e. 0x83)
# yubikey slot.
./sign-aab.sh ./sign-aab.cfg "$AAB_NAME"
