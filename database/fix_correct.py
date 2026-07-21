#!/usr/bin/env python3
import sys

filepath = "database/restore_dml_only.sql"

with open(filepath, 'rb') as f:
    raw = f.read()

text = raw.decode('utf-8')
print(f"Read {len(text)} chars")

# Step 1: INSERT INTO -> REPLACE INTO
text = text.replace('INSERT INTO ', 'REPLACE INTO ')
print(f"REPLACE INTO: {text.count('REPLACE INTO ')}")
print(f"INSERT INTO: {text.count('INSERT INTO ')}")

# Step 2: Fix the DROP TABLE value
# The actual value is: \; DROP TABLE user_wsh; --
# Current SQL: '\'''\'''\''; DROP TABLE user_wsh; --' (broken)
# Correct SQL: '\\''; DROP TABLE user_wsh; --'
#
# Find patterns where the SQL has incorrect escaping for this value
# Pattern in generated SQL: <some quote+escape sequence>'; DROP TABLE user_wsh; 
# 
# The mysqlbinlog output: @16='\'; DROP TABLE user_wsh; --'
# The parser generates: '\''''; DROP TABLE user_wsh; --'
#   (open quote + escaped quote + two escaped quotes + close quote -> then rest)
#
# We need to change to: '\\''; DROP TABLE user_wsh; --'
#   (open quote + escaped backslash + escaped quote + rest + close quote)

# The specific problematic substring that needs fixing appears in these contexts:
# 1. 'pending', '\''''; DROP TABLE user_wsh; --', 0, ... (VALUES)
# 2. `remark_wsh` = '\''''; DROP TABLE user_wsh; --', ... (UPDATE SET)
# 3. `remark_wsh` = '\''''; DROP TABLE user_wsh; --' AND ... (UPDATE WHERE)
# 4. ..."remark_wsh":"\'; DROP TABLE user_wsh; --"... (JSON in operation_log)

# Fix Type 1-3: SQL string values with the DROP TABLE text
# Replace '\''''; DROP TABLE user_wsh; --'  with  '\\''; DROP TABLE user_wsh; --'
# But this is in SQL context, so we need to handle the outer quotes

# Actually, simpler approach: since we know the exact byte sequences,
# let's find each occurrence and fix it

# In the SQL VALUES/WHERE/SET clauses:
# Current: 'pending', '\''''; DROP TABLE user_wsh; --', 
# The '\'' starts a string, \' is an escaped quote, then '''; closes and escapes

# Correct: 'pending', '\\''; DROP TABLE user_wsh; --', 
# The '\\'' starts with \\ (escaped backslash) then '' (escaped quote)

# So the fix: In each SQL occurrence, change:
#   \''''; DROP TABLE user_wsh; --
# to:
#   \\''; DROP TABLE user_wsh; --

print("\nFixing DROP TABLE value escaping...")
count_before = text.count("\\''''; DROP TABLE user_wsh; --")
print(f"  Before: {count_before} occurrences of \\'''' pattern")

text = text.replace("\\''''; DROP TABLE user_wsh; --", "\\\\''; DROP TABLE user_wsh; --")

count_after = text.count("\\\\''; DROP TABLE user_wsh; --")
print(f"  After: {count_after} occurrences of \\\\'' pattern")
print(f"  Remaining \\' pattern: {text.count(chr(92)+chr(39))}")

# Also fix the JSON occurrence (different pattern)
# Current: "remark_wsh":"\'; DROP TABLE user_wsh; --"
# The \' inside a SQL string needs escaping: the JSON should have \\' 
# But in this case the \' in JSON is actually the correct representation
# Since the actual value starts with \; 
# In JSON: "remark_wsh":"\\'; DROP TABLE user_wsh; --" (backslash escaped)
# In SQL string containing this JSON: '...\\''...' (double escaping)
#
# Let me check the actual bytes and fix properly

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(text)

print("\nFile updated!")
