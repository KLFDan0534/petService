import sys

with open('database/restore_dml_only.sql', 'rb') as f:
    lines = f.read().split(b'\n')

# Line 624 = index 623
line = lines[623]
print(f'Line 624 length: {len(line)}')

# Find DROP TABLE
idx = line.find(b'DROP')
if idx >= 0:
    # Show 20 bytes before DROP
    start = max(0, idx - 20)
    chunk = line[start:idx+10]
    print(f'Bytes before DROP ({start}-{idx+10}):')
    for i in range(0, len(chunk), 8):
        end = min(i+8, len(chunk))
        hex_part = ' '.join(f'{b:02x}' for b in chunk[i:end])
        ascii_part = ''.join(chr(b) if 32 <= b < 127 else '.' for b in chunk[i:end])
        print(f'  {start+i:04x}: {hex_part}  {ascii_part}')

    # Count consecutive quotes before DROP
    quote_count = 0
    pos = idx - 1
    while pos >= 0 and line[pos] == 39:  # 39 = single quote
        quote_count += 1
        pos -= 1
    print(f'\nConsecutive quotes before DROP: {quote_count}')
    print(f'Text before DROP: {line[max(0,idx-15):idx]}')

# Also show end of line to check closing quote
print(f'\nEnd of line (last 20 bytes):')
end_chunk = line[-20:]
for i in range(0, len(end_chunk), 8):
    end_i = min(i+8, len(end_chunk))
    hex_part = ' '.join(f'{b:02x}' for b in end_chunk[i:end_i])
    ascii_part = ''.join(chr(b) if 32 <= b < 127 else '.' for b in end_chunk[i:end_i])
    idx_in_line = len(line) - 20 + i
    print(f'  {idx_in_line:04x}: {hex_part}  {ascii_part}')
