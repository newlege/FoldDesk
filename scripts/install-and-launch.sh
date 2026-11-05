#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=env.sh
source "$ROOT/scripts/env.sh"

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
PKG=com.folddesk.browser
ACTIVITY=com.folddesk.browser/.MainActivity

if [[ ! -f "$APK" ]]; then
  "$ROOT/scripts/build-apk.sh"
fi

adb wait-for-device
adb install -r "$APK"
adb shell am start -n "$ACTIVITY"
echo "Installed and launched. Open httpbin from the start page or run:"
echo "  adb shell am start -a android.intent.action.VIEW -d https://httpbin.org/headers $PKG"
