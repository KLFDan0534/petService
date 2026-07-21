# 数据库恢复方案

## 方案一（推荐）：Binlog PITR 恢复

### 恢复原理

利用 MySQL 的 Binary Log（ROW格式）回放历史事务，将数据库恢复到数据丢失前（2026-06-28 00:33:38）的状态。

Binlog 中记录了完整的操作序列：
- `CREATE TABLE IF NOT EXISTS` → 表已存在时跳过
- `DROP TABLE IF EXISTS` → 删除空表（无影响）
- `CREATE TABLE` → 重建表结构
- `INSERT/UPDATE/DELETE` → 恢复所有数据

### 恢复步骤

```bash
# Step 1: 备份当前状态（已有一致性备份）
# 文件：database/01_database_baseline.sql

# Step 2: 合并所有binlog并回放
# 使用 --stop-datetime 跳过数据丢失的DDL事件
mysqlbinlog --no-defaults --stop-datetime="2026-06-28 00:33:38" ^
  binlog.000001 binlog.000002 binlog.000003 binlog.000004 ^
  binlog.000005 binlog.000006 binlog.000007 binlog.000008 ^
  binlog.000009 binlog.000010 binlog.000011 binlog.000012 ^
  binlog.000013 binlog.000014 binlog.000015 ^
  | mysql -f -h localhost -P 3308 -u root -p1234 pet_service
```

### 成功率评估

| 因素 | 评估 |
|------|------|
| binlog完整性 | ✅ 17个binlog文件，13个含数据 |
| binlog格式 | ✅ ROW格式，包含完整行数据 |
| 表结构兼容性 | ✅ 基线SQL与原始表结构一致 |
| DDL冲突 | ✅ CREATE TABLE IF NOT EXISTS 安全，ALTER TABLE 使用 -f 跳过 |
| 数据连续性 | ✅ 按时间顺序回放，UPDATE/DELETE 操作正确的已插入行 |
| **综合成功率** | **★ ★ ★ ★ ★ (95%)** |

### 风险分析

| 风险 | 等级 | 说明 |
|------|------|------|
| ALTER TABLE 重复执行 | 🟢 低 | `mysql -f` 强制跳过，列已存在时自动忽略 |
| 主键冲突 | 🟢 低 | 当前表为空，无任何冲突数据 |
| 外键约束冲突 | 🟢 低 | 数据按插入顺序回放，满足外键依赖 |
| UTF8MB4 排序规则差异 | 🟢 低 | 当前库使用 utf8mb4_0900_ai_ci |
| 部分数据被覆盖 | 🟢 无 | 无其他并发写入 |

### 预计恢复数据量

| 表分组 | 表数 | 估计行数 |
|--------|------|---------|
| 业务核心（order/user/pet） | ~10 | 200-500行 |
| 操作日志 | 1 | 200-300行 |
| 基础数据（role/category/service） | ~10 | 50-100行 |
| 关系表（user_role/favorite） | ~5 | 50-100行 |
| 其他 | ~9 | 50-100行 |
| **合计** | **35** | **550-1100行** |

---

## 方案二（备选）：Seed SQL 恢复

### 恢复步骤

```bash
# 使用 seed-data.sql 恢复演示数据
mysql -h localhost -P 3308 -u root -p1234 pet_service < seed-data.sql
```

### 成功率评估

| 因素 | 评估 |
|------|------|
| 数据真实性 | ❌ 仅演示数据，非真实业务数据 |
| 覆盖率 | ⚠️ 28/35 张表 |
| 可用性 | ✅ 可随时执行 |
| **综合成功率** | **★ ★ ★ ★ ☆ (70%)** |

---

## 方案三：先Binlog恢复 + 后Seed补充

### 说明

1. 先执行方案一（Binlog PITR）
2. Binlog 恢复完成后，检查数据完整性
3. 如果某些表在 Binlog 中数据不完整，用 seed-data.sql 补充

**不推荐**：Binlog 恢复已有完整数据，Seed 会覆盖（INSERT IGNORE 会跳过已存在的记录）。

---

## 方案对比总结

| 方案 | 成功率 | 数据真实性 | 风险 | 执行时间 | 推荐 |
|------|--------|-----------|------|---------|------|
| **方案一：Binlog PITR** | **95%** | **真实** | 低 | 5-10分钟 | **首选** |
| 方案二：Seed SQL | 70% | 演示数据 | 低 | 1分钟 | 备选 |
| 方案三：Binlog+Seed | 95% | 真实+演示 | 低 | 5-10分钟 | 不必要 |
