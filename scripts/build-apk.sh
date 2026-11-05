#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=env.sh
source "$ROOT/scripts/env.sh"
cd "$ROOT"
./gradlew assembleDebug --no-daemon "$@"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "Debug APK: $APK"
ls -la "$APK"
