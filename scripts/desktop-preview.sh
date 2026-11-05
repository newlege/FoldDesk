#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UA=$(python3 "$ROOT/scripts/extract_ua.py" "$ROOT/app/src/main/java/com/folddesk/browser/DesktopUserAgent.kt")
URL="${1:-file://$ROOT/app/src/main/assets/start.html}"
# Default window ~10" tablet logical area; override with FOLDDESK_WIDTH / FOLDDESK_HEIGHT
WIDTH="${FOLDDESK_WIDTH:-1280}"
HEIGHT="${FOLDDESK_HEIGHT:-800}"

for bin in google-chrome google-chrome-stable chromium chromium-browser /opt/google/chrome/chrome; do
  if command -v "$bin" >/dev/null 2>&1 || [[ -x "$bin" ]]; then
    CHROME="$bin"
    break
  fi
done

if [[ -z "${CHROME:-}" ]]; then
  echo "No Chrome/Chromium found. Install Chrome or set CHROME=/path/to/chrome" >&2
  exit 1
fi

exec "$CHROME" \
  --user-agent="$UA" \
  --window-size="${WIDTH},${HEIGHT}" \
  --new-window \
  "$URL"
