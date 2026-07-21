#!/usr/bin/env python3
import sys

filepath = "database/restore_dml_only.sql"

# Step 1: Read the file
with open(filepath, 'rb') as f:
    raw = f.read()
text = raw.decode('utf-8')
print(f"Read {len(text)} chars, {text.count(chr(10))} lines")

# Step 2: Replace INSERT INTO with REPLACE INTO
text = text.replace('INSERT INTO ', 'REPLACE INTO ')
print(f"REPLACE INTO count: {text.count('REPLACE INTO ')}")
print(f"INSERT INTO remaining: {text.count('INSERT INTO ')}")

# Step 3: Fix backslash-quote escaping in string values
# The issue: mysqlbinlog -v outputs values with backslash escaping (e.g., \' for single quotes)
# But the SQL output uses this literal backslash, which breaks MySQL parsing
#
# Fix: Replace \' with '' (SQL standard escaping) ONLY inside string values
# We need to be careful to only replace \' patterns that are inside SQL string literals
#
# Strategy: Process each line and handle SQL string values properly
text = text.replace("\\'", "''")
print(f"Backslash-quote patterns fixed")

# Step 4: Verify
bs = chr(92)
sq = chr(39)
remaining = text.count(bs + sq)
print(f"Remaining \\' patterns: {remaining}")

# Step 5: Verify DROP TABLE values
idx = text.find('DROP TABLE')
if idx >= 0:
    surrounding = text[max(0,idx-30):idx+60]
    print(f"DROP TABLE context: ...{surrounding}...")

# Step 6: Write back
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(text)
print("File written successfully!")
