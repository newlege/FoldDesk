#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=env.sh
source "$ROOT/scripts/env.sh"

AVD_NAME="${AVD_NAME:-FoldDesk_MagicV6_Inner}"
EMULATOR="${ANDROID_HOME}/emulator/emulator"

if [[ ! -x "$EMULATOR" ]]; then
  echo "Emulator not installed. Run: ./scripts/create-avd.sh" >&2
  exit 1
fi

exec "$EMULATOR" -avd "$AVD_NAME" -no-snapshot-save -gpu swiftshader_indirect "$@"
