#!/usr/bin/env bash
# Create an AVD for a representative tablet or foldable-inner profile.
# Usage: ./scripts/create-avd.sh [profile]
#   ./scripts/create-avd.sh --list
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=env.sh
source "$ROOT/scripts/env.sh"

SYSTEM_IMAGE="${SYSTEM_IMAGE:-system-images;android-35;google_apis;x86_64}"

list_profiles() {
  cat <<'EOF'
Available emulator profiles (pass as first argument):

  tablet_7_1280x800       7" class, 1280×800 @ 213 dpi (WXGA)
  tablet_10_1920x1200     10" class, 1920×1200 @ 240 dpi (WUXGA)
  tablet_11_2000x1200     11" class, 2000×1200 @ 240 dpi
  tablet_12_2560x1600     12–13" class, 2560×1600 @ 320 dpi (WQXGA)
  fold_magic_v6_inner     Honor Magic V6 inner, 2352×2172 @ 560 dpi (near-square)

Environment overrides: AVD_NAME, SYSTEM_IMAGE
After create: ./scripts/run-emulator.sh [AVD_NAME]
EOF
}

resolve_profile() {
  local profile="$1"
  case "$profile" in
    tablet_7_1280x800)
      AVD_NAME="${AVD_NAME:-FoldDesk_Tablet_7_1280x800}"
      LCD_W=1280 LCD_H=800 LCD_D=213 AVD_DEVICE="Nexus 7"
      ;;
    tablet_10_1920x1200)
      AVD_NAME="${AVD_NAME:-FoldDesk_Tablet_10_1920x1200}"
      LCD_W=1920 LCD_H=1200 LCD_D=240 AVD_DEVICE="pixel_c"
      ;;
    tablet_11_2000x1200)
      AVD_NAME="${AVD_NAME:-FoldDesk_Tablet_11_2000x1200}"
      LCD_W=2000 LCD_H=1200 LCD_D=240 AVD_DEVICE="pixel_c"
      ;;
    tablet_12_2560x1600)
      AVD_NAME="${AVD_NAME:-FoldDesk_Tablet_12_2560x1600}"
      LCD_W=2560 LCD_H=1600 LCD_D=320 AVD_DEVICE="pixel_c"
      ;;
    fold_magic_v6_inner)
      AVD_NAME="${AVD_NAME:-FoldDesk_Fold_MagicV6_Inner}"
      LCD_W=2352 LCD_H=2172 LCD_D=560 AVD_DEVICE="pixel_fold"
      ;;
    *)
      echo "Unknown profile: $profile" >&2
      list_profiles >&2
      exit 1
      ;;
  esac
}

if [[ "${1:-}" == "--list" || "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  list_profiles
  exit 0
fi

PROFILE="${1:-tablet_10_1920x1200}"
resolve_profile "$PROFILE"

sdkmanager "$SYSTEM_IMAGE" "emulator" 2>/dev/null || true

if avdmanager list avd | grep -q "Name: $AVD_NAME"; then
  echo "AVD already exists: $AVD_NAME"
  exit 0
fi

echo "Creating AVD $AVD_NAME — profile=$PROFILE (${LCD_W}×${LCD_H} @ ${LCD_D} dpi)"
echo "no" | avdmanager create avd \
  -n "$AVD_NAME" \
  -k "$SYSTEM_IMAGE" \
  -d "$AVD_DEVICE" \
  --force

AVD_DIR="$HOME/.android/avd/${AVD_NAME}.avd"
CONFIG="$AVD_DIR/config.ini"
if [[ -f "$CONFIG" ]]; then
  sed -i '/^hw\.lcd/d' "$CONFIG" || true
  cat >> "$CONFIG" <<EOF
hw.lcd.width=${LCD_W}
hw.lcd.height=${LCD_H}
hw.lcd.density=${LCD_D}
hw.keyboard=yes
hw.gpu.mode=auto
EOF
fi

echo "Created $AVD_NAME"
grep -E '^hw\.lcd' "$CONFIG" || true
