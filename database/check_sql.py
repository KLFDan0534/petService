#!/usr/bin/env python3
import sys

filepath = "database/restore_dml_only.sql"

with open(filepath, 'rb') as f:
    raw = f.read()

# Find 'DROP TABLE' in raw bytes
idx = raw.find(b'DROP TABLE')
print(f"First DROP TABLE at byte: {idx}")
if idx >= 0:
    start = max(0, idx-60)
    end = min(len(raw), idx+90)
    chunk = raw[start:end]
    print(f"Bytes (hex): {chunk.hex()}")
    print(f"Text: {chunk.decode('utf-8', errors='replace')}")
    print()
    
    # Find the full SQL statement
    stmt_start = raw.rfind(b';', 0, idx) + 1
    stmt_end = raw.find(b';', idx) + 1
    stmt = raw[stmt_start:stmt_end]
    print(f"Full SQL ({len(stmt)} bytes):")
    print(stmt.decode('utf-8', errors='replace'))
    print()
    
    # Count single quotes and backslashes in the value area
    # Find VALUES keyword
    values_idx = raw.find(b"VALUES", stmt_start, stmt_end)
    print(f"VALUES starts at byte offset: {values_idx - stmt_start}")
    
    # Show the bytes between DROP TABLE and its surrounding quotes
    drop_idx = raw.find(b"DROP", stmt_start, stmt_end)
    for offset in range(-5, 10):
        byte_pos = drop_idx + offset
        if stmt_start <= byte_pos < stmt_end:
            b = raw[byte_pos]
            print(f"  offset {offset:+d}: byte 0x{b:02x} = {chr(b) if 32 <= b < 127 else '?'}")
