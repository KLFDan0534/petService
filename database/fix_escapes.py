#!/usr/bin/env python3
import sys

filepath = "database/restore_dml_only.sql"

with open(filepath, 'rb') as f:
    raw = f.read()

text = raw.decode('utf-8', errors='replace')
lines = text.split('\n')
issues = []

bs = chr(92)  # backslash
sq = chr(39)  # single quote
pattern = bs + sq  # \'

for i, line in enumerate(lines):
    if not line.strip():
        continue
    if bs + sq in line:
        idx = line.find(bs + sq)
        start = max(0, idx - 20)
        end = min(len(line), idx + 60)
        snippet = line[start:end]
        issues.append((i+1, len(line), snippet))

print(f"Found {len(issues)} lines with pattern:")
for line_no, length, snippet in issues:
    print(f"  Line {line_no} (len={length}): ...{snippet}...")
