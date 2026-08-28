#!/usr/bin/env sh
# Bootstrap سبک Gradle؛ اگر Gradle نصب نباشد نسخه 9.5.0 را از منبع رسمی دریافت می‌کند.
set -eu
GRADLE_VERSION="9.5.0"
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
LOCAL_DIR="$SCRIPT_DIR/.gradle-local"
GRADLE_HOME="$LOCAL_DIR/gradle-$GRADLE_VERSION"
ZIP="$LOCAL_DIR/gradle-$GRADLE_VERSION-bin.zip"
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$LOCAL_DIR"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then curl -fL "$URL" -o "$ZIP"; elif command -v wget >/dev/null 2>&1; then wget -O "$ZIP" "$URL"; else echo "Gradle/curl/wget not found" >&2; exit 1; fi
  unzip -q -o "$ZIP" -d "$LOCAL_DIR"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
