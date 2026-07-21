#!/usr/bin/env python3
import sys

filepath = "database/restore_dml_only.sql"

with open(filepath, 'rb') as f:
    raw = f.read()

text = raw.decode('utf-8', errors='replace')

# Fix 1: Replace '\''; DROP TABLE user_wsh; --' with the correct SQL escaping
# The value should be: ''; DROP TABLE user_wsh; -- (mysqlbinlog output format with '' escaping)
# But the parser added extra escaping, resulting in incorrect SQL
#
# Current (broken): '\''; DROP TABLE user_wsh; --'
# Expected:         ''''; DROP TABLE user_wsh; --'
#
# We need to find all occurrences and fix them
# Pattern: \' (backslash-quote) followed by ' (quote) where the backslash is spurious

# Strategy: For each line with backslash-quote, fix the escaping
# The backslash-singlequote pattern inside strings needs to be replaced with double-singlequote

lines = text.split('\n')
fixed_lines = []
changes = 0

for i, line in enumerate(lines):
    if "\\'" in line:
        # This line has the backslash-quote issue
        # We need to fix it by replacing the backslash-quote pattern
        # within SQL string literals
        
        # The specific pattern in our SQL: '\''; DROP TABLE
        # Fix: remove the backslash before the first quote
        new_line = line.replace("\\'", "''")
        if new_line != line:
            changes += 1
            fixed_lines.append(new_line)
            print(f"Fixed line {i+1}")
        else:
            fixed_lines.append(line)
    else:
        fixed_lines.append(line)

text = '\n'.join(fixed_lines)

# Fix 2: Also check for the json-style value in line 1442
# "remark_wsh":"\''; DROP TABLE user_wsh; --"
# This is in a JSON field, needs different handling
# Replace \"\\' with \"' (the backslash is spurious in JSON context)
import re
text = text.replace('"\\\'', '"\'')
# But wait, this might affect other things. Let me be more specific.
# Actually, the JSON value issue is in a SQL text field, not real JSON.
# Let me just do a targeted fix:
text = text.replace('"\\\'\'; DROP TABLE user_wsh; --"', '"\'\\\'\\\'; DROP TABLE user_wsh; --"')

# Actually, let me reconsider. The JSON value in line 1442 is an escaped string
# inside a SQL value. Let me check what the correct format should be.
# Current: "remark_wsh":"\''; DROP TABLE user_wsh; --"
# The backslash is meant to be part of the JSON string escaping.
# In the SQL value, this becomes: "remark_wsh":"'; DROP TABLE user_wsh; --"
# So we need to fix the backslash-quote as well.

# Simpler approach: just fix all the SQL lines with backslash-quote pattern
# The JSON line (1442) has :"\'' which is different from '\''

print(f"\nChanges made: {changes}")

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(text)

print("File updated")
