#!/usr/bin/env python3
"""
Binlog to DML-only SQL converter.
Reads mysqlbinlog -v output, extracts INSERT/UPDATE/DELETE events,
maps @N positional values to column names, and generates clean SQL.

Usage: python binlog_to_dml.py
"""

import subprocess
import re
import sys
import os
from collections import OrderedDict

BINLOG_DIR = r"D:\code\ideaProject\petService\database"
OUTPUT_FILE = r"D:\code\ideaProject\petService\database\restore_dml_only.sql"
MYSQL_BIN = r"D:\Other\MySQL\mysql-9.3.0-winx64\bin\mysqlbinlog.exe"
MYSQL_CLI = r"D:\Other\MySQL\mysql-9.3.0-winx64\bin\mysql.exe"
STOP_DATETIME = "2026-06-28 00:33:38"

# Tables that need data first (parent tables with FK dependencies)
FK_ORDER = [
    "role_wsh", "user_wsh", "category_wsh", "merchant_wsh",
    "user_role_wsh", "keeper_wsh", "pet_wsh", "pet_service_wsh",
    "address_wsh", "business_hours_wsh",
    "pet_order_wsh", "payment_wsh", "refund_wsh", "tip_wsh",
    "wallet_wsh", "wallet_transaction_wsh", "withdrawal_wsh",
    "notice_wsh", "notice_read_wsh", "notification_wsh",
    "chat_message_wsh", "complaint_wsh", "rating_wsh",
    "ticket_wsh", "ticket_message_wsh",
    "care_record_wsh",
    "favorite_wsh", "file_record_wsh", "operation_log_wsh",
    "content_review_wsh", "ai_report_wsh",
    "knowledge_document_wsh", "document_embedding_wsh",
]

def get_column_mapping():
    """Get column names and positions from information_schema."""
    cmd = [
        MYSQL_CLI, "-h", "localhost", "-P", "3308",
        "-u", "root", "-p1234", "--default-character-set=utf8mb4", "-N",
        "-e",
        "SELECT TABLE_NAME, COLUMN_NAME, ORDINAL_POSITION "
        "FROM information_schema.COLUMNS "
        "WHERE TABLE_SCHEMA = 'pet_service' "
        "ORDER BY TABLE_NAME, ORDINAL_POSITION"
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    
    col_map = {}
    for line in result.stdout.strip().split('\n'):
        parts = line.split('\t')
        if len(parts) >= 3:
            tbl = parts[0].strip()
            col = parts[1].strip()
            pos = int(parts[2].strip())
            if tbl not in col_map:
                col_map[tbl] = {}
            col_map[tbl][pos] = col
    
    print(f"Loaded {len(col_map)} tables from information_schema")
    return col_map


def run_mysqlbinlog():
    """Run mysqlbinlog and return the output line by line."""
    binlog_files = []
    for i in range(2, 16):
        f = os.path.join(BINLOG_DIR, f"binlog.{i:06d}")
        if os.path.exists(f):
            binlog_files.append(f)
    
    cmd = [
        MYSQL_BIN, "--no-defaults",
        "--base64-output=DECODE-ROWS", "-v",
        f"--stop-datetime={STOP_DATETIME}"
    ] + binlog_files
    
    print(f"Running: {' '.join(cmd)}")
    # Use binary capture to avoid encoding issues
    result = subprocess.run(cmd, capture_output=True, timeout=300)
    
    # Decode as utf-8, ignoring errors
    try:
        output = result.stdout.decode('utf-8', errors='replace')
    except:
        output = result.stdout.decode('gbk', errors='replace')
    
    # Filter out warnings
    lines = []
    for line in output.split('\n'):
        line_clean = line.strip()
        if ('Warning' not in line_clean and 'password' not in line_clean 
            and 'insecure' not in line_clean and len(line_clean) > 0):
            lines.append(line.rstrip('\r'))
    
    print(f"mysqlbinlog output: {len(lines)} lines")
    return lines


def parse_value(val):
    """Parse a @N=value string into (position, raw_value)."""
    val = val.strip()
    if val == 'NULL':
        return 'NULL'
    # Check if it's a quoted string
    if val.startswith("'") and val.endswith("'"):
        inner = val[1:-1]
        # Escape single quotes for SQL
        inner = inner.replace("'", "''")
        return f"'{inner}'"
    # Check if it's a number
    try:
        if '.' in val:
            float(val)
            return val
        else:
            int(val)
            return val
    except ValueError:
        pass
    # Default: treat as string
    return f"'{val.replace(chr(39), chr(39)+chr(39))}'"


def generate_insert(table, values, col_map):
    """Generate INSERT INTO statement from @N=value pairs."""
    if table not in col_map:
        print(f"  WARNING: Unknown table '{table}', skipping INSERT")
        return None
    
    cols = col_map[table]
    col_names = []
    col_vals = []
    
    for item in values:
        pos = item['pos']
        val = item['val']
        if pos in cols:
            col_names.append(f"`{cols[pos]}`")
            col_vals.append(parse_value(val))
    
    if not col_names:
        return None
    
    sql = f"INSERT INTO `{table}` ({', '.join(col_names)}) VALUES ({', '.join(col_vals)});"
    return sql


def generate_update(table, old_values, new_values, col_map):
    """Generate UPDATE statement from old and new @N=value pairs."""
    if table not in col_map:
        print(f"  WARNING: Unknown table '{table}', skipping UPDATE")
        return None
    
    cols = col_map[table]
    
    # Build SET clause from new values
    set_parts = []
    for item in new_values:
        pos = item['pos']
        val = item['val']
        if pos in cols:
            set_parts.append(f"`{cols[pos]}` = {parse_value(val)}")
    
    # Build WHERE clause from old values (use first column as PK, or all columns)
    where_parts = []
    for item in old_values:
        pos = item['pos']
        val = item['val']
        if pos in cols:
            where_parts.append(f"`{cols[pos]}` = {parse_value(val)}")
    
    if not set_parts or not where_parts:
        return None
    
    sql = f"UPDATE `{table}` SET {', '.join(set_parts)} WHERE {' AND '.join(where_parts)};"
    return sql


def generate_delete(table, values, col_map):
    """Generate DELETE FROM statement from @N=value pairs."""
    if table not in col_map:
        print(f"  WARNING: Unknown table '{table}', skipping DELETE")
        return None
    
    cols = col_map[table]
    where_parts = []
    for item in values:
        pos = item['pos']
        val = item['val']
        if pos in cols:
            where_parts.append(f"`{cols[pos]}` = {parse_value(val)}")
    
    if not where_parts:
        return None
    
    sql = f"DELETE FROM `{table}` WHERE {' AND '.join(where_parts)};"
    return sql


def sort_by_fk_order(sql_statements):
    """Sort SQL statements by foreign key dependency order."""
    # Group SQL by table name
    table_sqls = {}
    for stmt in sql_statements:
        m = re.match(r'(INSERT INTO|UPDATE|DELETE FROM) `(\w+)`', stmt)
        if m:
            tbl = m.group(2)
            if tbl not in table_sqls:
                table_sqls[tbl] = []
            table_sqls[tbl].append(stmt)
    
    # Reorder by FK_ORDER, keeping tables not in FK_ORDER at the end
    ordered = []
    for tbl in FK_ORDER:
        if tbl in table_sqls:
            ordered.extend(table_sqls[tbl])
    
    # Add any tables not in FK_ORDER
    for tbl in table_sqls:
        if tbl not in FK_ORDER:
            ordered.extend(table_sqls[tbl])
    
    return ordered


def main():
    print("=" * 60)
    print("PetService Binlog to DML-Only Converter")
    print("=" * 60)
    
    # Step 1: Get column mapping
    print("\n[1/4] Loading column mapping from database...")
    col_map = get_column_mapping()
    
    # Step 2: Run mysqlbinlog
    print("\n[2/4] Running mysqlbinlog (this may take a minute)...")
    lines = run_mysqlbinlog()
    
    # Step 3: Parse events
    print("\n[3/4] Parsing events and generating DML SQL...")
    
    sql_statements = []
    counters = {'INSERT': 0, 'UPDATE': 0, 'DELETE': 0}
    tables_seen = set()
    
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # Detect INSERT
        m = re.match(r'^### INSERT INTO `(.+?)`\.`(.+?)`$', line)
        if m:
            db = m.group(1)
            table = m.group(2)
            tables_seen.add(table)
            
            # Collect @N=value pairs
            values = []
            i += 1
            # Skip optional ### SET line
            if i < len(lines) and lines[i].strip() == '### SET':
                i += 1
            # Collect @N values
            while i < len(lines):
                m2 = re.match(r'^###\s+@(\d+)=(.+)$', lines[i])
                if m2:
                    values.append({'pos': int(m2.group(1)), 'val': m2.group(2).strip()})
                    i += 1
                else:
                    break
            
            if values:
                sql = generate_insert(table, values, col_map)
                if sql:
                    sql_statements.append(sql)
                    counters['INSERT'] += 1
            continue
        
        # Detect UPDATE
        m = re.match(r'^### UPDATE `(.+?)`\.`(.+?)`$', line)
        if m:
            table = m.group(2)
            tables_seen.add(table)
            
            # Collect old values (after ### WHERE or right after ### UPDATE line)
            old_values = []
            new_values = []
            i += 1
            
            # Skip ### WHERE line if present
            if i < len(lines) and lines[i].strip() == '### WHERE':
                i += 1
            
            # Collect old @N values until ### SET
            while i < len(lines):
                line2 = lines[i].strip()
                if line2 == '### SET':
                    i += 1
                    break
                m2 = re.match(r'^###\s+@(\d+)=(.+)$', lines[i])
                if m2:
                    old_values.append({'pos': int(m2.group(1)), 'val': m2.group(2).strip()})
                    i += 1
                else:
                    break
            
            # Collect new @N values
            while i < len(lines):
                m2 = re.match(r'^###\s+@(\d+)=(.+)$', lines[i])
                if m2:
                    new_values.append({'pos': int(m2.group(1)), 'val': m2.group(2).strip()})
                    i += 1
                else:
                    break
            
            if old_values and new_values:
                sql = generate_update(table, old_values, new_values, col_map)
                if sql:
                    sql_statements.append(sql)
                    counters['UPDATE'] += 1
            continue
        
        # Detect DELETE FROM
        m = re.match(r'^### DELETE FROM `(.+?)`\.`(.+?)`$', line)
        if m:
            table = m.group(2)
            tables_seen.add(table)
            
            values = []
            i += 1
            # Skip ### WHERE line if present
            if i < len(lines) and lines[i].strip() == '### WHERE':
                i += 1
            # Collect @N values
            while i < len(lines):
                m2 = re.match(r'^###\s+@(\d+)=(.+)$', lines[i])
                if m2:
                    values.append({'pos': int(m2.group(1)), 'val': m2.group(2).strip()})
                    i += 1
                else:
                    break
            
            if values:
                sql = generate_delete(table, values, col_map)
                if sql:
                    sql_statements.append(sql)
                    counters['DELETE'] += 1
            continue
        
        i += 1
    
    # Step 4: Sort by FK dependency order
    print(f"\n[4/4] Sorting statements by FK order ({len(sql_statements)} statements)...")
    ordered_sql = sort_by_fk_order(sql_statements)
    
    # Write output
    print(f"\nWriting {OUTPUT_FILE}...")
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        f.write("-- ============================================================\n")
        f.write("-- PetService DML-Only Recovery Script\n")
        f.write("-- Generated from binlog.000002 ~ binlog.000015\n")
        f.write(f"-- Stop before: {STOP_DATETIME}\n")
        f.write("-- Contains only: INSERT / UPDATE / DELETE (no DDL)\n")
        f.write("-- ============================================================\n\n")
        f.write("SET NAMES utf8mb4;\n")
        f.write("SET FOREIGN_KEY_CHECKS = 0;\n\n")
        
        # Group by table for readability
        current_table = None
        for stmt in ordered_sql:
            m = re.match(r'(INSERT INTO|UPDATE|DELETE FROM) `(\w+)`', stmt)
            if m:
                tbl = m.group(2)
                if tbl != current_table:
                    current_table = tbl
                    f.write(f"\n-- === {tbl} ===\n")
            f.write(stmt + "\n")
        
        f.write("\nSET FOREIGN_KEY_CHECKS = 1;\n")
        f.write("\n-- ============================================================\n")
        f.write("-- Recovery Complete\n")
        f.write("-- ============================================================\n")
    
    print(f"\n{'=' * 60}")
    print(f"SUMMARY")
    print(f"{'=' * 60}")
    print(f"  INSERT: {counters['INSERT']}")
    print(f"  UPDATE: {counters['UPDATE']}")
    print(f"  DELETE: {counters['DELETE']}")
    print(f"  Tables: {len(tables_seen)}")
    print(f"  Output: {OUTPUT_FILE}")
    print(f"{'=' * 60}")
    
    # Print table list
    print(f"\nTables affected ({len(tables_seen)}):")
    for t in sorted(tables_seen):
        count = sum(1 for s in ordered_sql if f'`{t}`' in s)
        print(f"  - {t}: {count} operations")


if __name__ == '__main__':
    main()
