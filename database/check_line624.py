import sys

with open('database/restore_dml_only.sql', 'rb') as f:
    raw = f.read()

lines = raw.split(b'\n')
print(f'Total lines: {len(lines)}')

# Find the DROP TABLE line(s)
for i, line in enumerate(lines):
    if b'DROP TABLE' in line:
        print(f'Line {i+1} (len={len(line)}):')
        text = line.decode('utf-8', errors='replace')
        print(f'  Text: {text}')
        print()

# Also show lines 620-630
print('--- Lines 620-630 ---')
for i in range(619, min(630, len(lines))):
    line = lines[i]
    text = line.decode('utf-8', errors='replace')
    if text.strip():
        print(f'{i+1}: {text[:200]}')
    else:
        print(f'{i+1}: (empty)')
