#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UA=$(python3 "$ROOT/scripts/extract_ua.py" "$ROOT/app/src/main/java/com/folddesk/browser/DesktopUserAgent.kt")
echo "=== FoldDesk configured User-Agent ==="
echo "$UA"
echo ""
if command -v curl >/dev/null; then
  echo "=== httpbin.org/headers (curl with same UA) ==="
  curl -fsSL -H "User-Agent: $UA" -H "Accept-Language: en-US,en;q=0.9" https://httpbin.org/headers
  echo ""
else
  echo "(curl not available — skipped live httpbin check)"
fi
