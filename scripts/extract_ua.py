#!/usr/bin/env python3
"""Extract FoldDesk USER_AGENT_STRING from Kotlin source."""
import re
import sys
from pathlib import Path

text = Path(sys.argv[1]).read_text()
block = re.search(
    r"USER_AGENT_STRING: String =\s*(.+?)\n\s*const val ACCEPT_LANGUAGE",
    text,
    re.DOTALL,
)
if not block:
    sys.exit(1)
parts = re.findall(r'"([^"]*)"', block.group(1))
if not parts:
    sys.exit(1)
print("".join(parts))
