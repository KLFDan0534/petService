#!/usr/bin/env python3
import sys

filepath = "database/restore_dml_only.sql"

with open(filepath, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\n')
if len(lines) >= 624:
    l624 = lines[623]
    print('Line 624 bytes (hex):')
    for i in range(0, min(len(l624), 300), 16):
        hex_part = ' '.join(f'{b:02x}' for b in l624[i:i+16])
        ascii_part = ''.join(chr(b) if 32 <= b < 127 else '.' for b in l624[i:i+16])
        print(f'  {i:04x}: {hex_part}  {ascii_part}')
    print()
    print(f'Line 624 text: {l624.decode("utf-8", errors="replace")}')

# Also check if there are other similar bytes
print()
print('Searching for all lines with backslash-singlequote pattern...')
for i, line in enumerate(lines):
    if b"\\'" in line:
        bslash_idx = line.find(b'\\')
        sq_idx = bslash_idx + 1
        if sq_idx < len(line) and line[sq_idx] == ord("'"):
            # Show bytes around the pattern
            start = max(0, bslash_idx - 10)
            end = min(len(line), bslash_idx + 20)
            chunk = line[start:end]
            print(f'  Line {i+1}: ({chunk.hex()}) -> {chunk.decode("utf-8", errors="replace")}')
