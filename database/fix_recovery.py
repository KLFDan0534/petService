#!/usr/bin/env python3
import sys

filepath = "database/restore_dml_only.sql"

with open(filepath, 'rb') as f:
    raw = f.read()

text = raw.decode('utf-8')
lines = text.split('\n')

# Check line 28 (0-indexed = 27)
if len(lines) >= 28:
    print(f"Line 28: {lines[27][:200]}")

# Check for Chinese characters
admin_found = '\u7ba1\u7406\u5458' in text  # 管理员
sysadmin_found = '\u7cfb\u7edf\u7ba1\u7406\u5458' in text  # 系统管理员
print(f"管理员 found: {admin_found}")
print(f"系统管理员 found: {sysadmin_found}")

# Replace INSERT INTO with REPLACE INTO
content = text.replace('INSERT INTO ', 'REPLACE INTO ')
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Verify after replacement
with open(filepath, 'rb') as f:
    raw2 = f.read()
text2 = raw2.decode('utf-8')
lines2 = text2.split('\n')
print(f"\nLine 28 after replace: {lines2[27][:200]}")
print(f"INSERT INTO remaining: {text2.count('INSERT INTO ')}")
print(f"REPLACE INTO total: {text2.count('REPLACE INTO ')}")
