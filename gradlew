#!/usr/bin/env sh
set -eu

GRADLE_VERSION="8.9"
GRADLE_SHA256="d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab"
BASE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/lunara-bootstrap"
GRADLE_HOME="$BASE_DIR/gradle-$GRADLE_VERSION"
ZIP_FILE="$BASE_DIR/gradle-$GRADLE_VERSION-bin.zip"
URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

checksum() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  else
    echo "Install coreutils so the Gradle download can be verified." >&2
    exit 1
  fi
}

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$BASE_DIR"
  if [ ! -f "$ZIP_FILE" ]; then
    PART_FILE="$ZIP_FILE.part"
    rm -f "$PART_FILE"
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 --connect-timeout 20 "$URL" -o "$PART_FILE"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$PART_FILE" "$URL"
    else
      echo "Install curl or wget, then run this command again." >&2
      exit 1
    fi
    mv "$PART_FILE" "$ZIP_FILE"
  fi

  ACTUAL_SHA256="$(checksum "$ZIP_FILE")"
  if [ "$ACTUAL_SHA256" != "$GRADLE_SHA256" ]; then
    rm -f "$ZIP_FILE"
    echo "Gradle download checksum mismatch. The file was removed; run again." >&2
    exit 1
  fi

  command -v unzip >/dev/null 2>&1 || {
    echo "Install unzip, then run this command again." >&2
    exit 1
  }
  unzip -tq "$ZIP_FILE" >/dev/null
  rm -rf "$GRADLE_HOME"
  unzip -q "$ZIP_FILE" -d "$BASE_DIR"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
