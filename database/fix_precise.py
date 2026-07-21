#!/usr/bin/env python3
import sys

filepath = "database/restore_dml_only.sql"

with open(filepath, 'rb') as f:
    raw = f.read()
text = raw.decode('utf-8')

print(f"Before:")
print(f"  INSERT INTO: {text.count('INSERT INTO ')}")
print(f"  REPLACE INTO: {text.count('REPLACE INTO ')}")

# Fix 1: INSERT -> REPLACE
text = text.replace('INSERT INTO ', 'REPLACE INTO ')

print(f"After Fix 1:")
print(f"  INSERT INTO: {text.count('INSERT INTO ')}")
print(f"  REPLACE INTO: {text.count('REPLACE INTO ')}")

# Fix 2: Replace \''; DROP TABLE user_wsh; -- with \\''; DROP TABLE user_wsh; --
# The \' (backslash+quote) in the original parser output for this value
# causes MySQL to interpret \' as an escaped quote, then the next ' closes the string.
# We need \\'' (two backslashes + two quotes) which MySQL sees as:
# \\ = escaped backslash (literal \), '' = escaped quote (literal ')
# Result: \' inside the string, which is the actual data value.

# In Python: "\\''; DROP TABLE user_wsh; --" = \''; DROP TABLE user_wsh; --
# In Python: "\\\\''; DROP TABLE user_wsh; --" = \\''; DROP TABLE user_wsh; --

old_val = "\\''; DROP TABLE user_wsh; --"
new_val = "\\\\''; DROP TABLE user_wsh; --"

count_before = text.count(old_val)
print(f"\nFix 2: {count_before} occurrences of backslash DROP TABLE pattern")

text = text.replace(old_val, new_val)

count_after = text.count(new_val)
print(f"Fixed: {count_after} occurrences")

# Verify
bs_sq = chr(92) + chr(39)
print(f"\nRemaining \\' patterns in file: {text.count(bs_sq)}")

# Check DROP TABLE line
idx = text.find('DROP TABLE user_wsh')
if idx >= 0:
    ctx = text[max(0,idx-25):idx+40]
    print(f"DROP TABLE context: ...{ctx}...")

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(text)
print(f"\nFile written!")
