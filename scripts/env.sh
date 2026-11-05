#!/usr/bin/env bash
# Resolve Android SDK for local / Cloud Agent builds.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -z "${ANDROID_HOME:-}" ]]; then
  if [[ -d "$ROOT/android-sdk" ]]; then
    export ANDROID_HOME="$ROOT/android-sdk"
  elif [[ -d "$HOME/Android/Sdk" ]]; then
    export ANDROID_HOME="$HOME/Android/Sdk"
  fi
fi
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
export PATH="$PATH:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"
echo "ANDROID_HOME=$ANDROID_HOME"
