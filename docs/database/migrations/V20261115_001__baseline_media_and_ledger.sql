-- ===========================================================================
-- V20261115_001 — 基线补齐：迁移台账表 + 服务产品媒体表
--
-- 背景：
--   ProductMediaMigrationReadiness 在 MySQL 环境下启动即断言两件事：
--     1) migration_ledger_wsh 中记录了 v6_service_product_media
--     2) pet_service_media_wsh 表存在
--   任一不满足即抛 IllegalStateException 阻止启动（有意的 fail-fast）。
--
--   但 pet-admin/src/main/resources/db/schema.sql（compose 唯一挂载的初始化脚本）
--   长期缺少这两个对象，导致「用旧版 schema.sql 建过的库」与「全新初始化的库」
--   都无法通过启动检查。
--
-- 适用对象：
--   * 生产库：**不需要执行**。线上实库（2026-11-09 只读快照）已同时存在两表。
--   * 开发/测试库：凡是用 2026-11-15 之前的 schema.sql 初始化过、且启动报
--     "Product media migration is missing on MySQL" 的环境，执行本脚本修复。
--
-- 执行要求（沿用本目录纪律）：
--   1. 执行前备份：mysqldump --single-transaction --routines --triggers \
--        pet_service > backup_$(date +%Y%m%d-%H%M).sql
--   2. 验证备份可读取：head -50 backup_*.sql
--   3. 本脚本幂等：CREATE TABLE IF NOT EXISTS + INSERT IGNORE，
--      重复执行不报错、不覆盖已有台账记录。
--   4. 只新建两张表 + 一行台账，不修改任何既有表结构与数据。
--
-- 回滚：见 V20261115_001__rollback.sql
-- ===========================================================================

-- ---------- 1) 迁移台账 ----------
CREATE TABLE IF NOT EXISTS `migration_ledger_wsh` (
    `version_wsh` VARCHAR(100) NOT NULL COMMENT '迁移版本号',
    `applied_at_wsh` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '应用时间',
    `summary_wsh` VARCHAR(1000) DEFAULT NULL COMMENT '迁移摘要',
    PRIMARY KEY (`version_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库迁移台账';

-- ---------- 2) 服务产品图片表（v6 迁移的结构结果） ----------
CREATE TABLE IF NOT EXISTS `pet_service_media_wsh` (
    `id_wsh` BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `service_id_wsh` BIGINT NOT NULL COMMENT '服务产品ID(pet_service_wsh.id_wsh)',
    `file_id_wsh` BIGINT NOT NULL COMMENT '文件记录ID(file_record_wsh.id_wsh)',
    `sort_order_wsh` INT NOT NULL DEFAULT 0 COMMENT '排序序号(0..N 连续)',
    `is_cover_wsh` TINYINT NOT NULL DEFAULT 0 COMMENT '是否封面: 0-否 1-是',
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id_wsh`),
    UNIQUE KEY `uk_service_file` (`service_id_wsh`, `file_id_wsh`) COMMENT '同一服务不重复引用同一文件',
    UNIQUE KEY `uk_service_sort` (`service_id_wsh`, `sort_order_wsh`) COMMENT '同一服务内排序唯一',
    KEY `idx_media_file` (`file_id_wsh`) COMMENT '文件索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务产品图片表';

-- ---------- 3) 台账基线 ----------
-- 只登记代码真正断言的版本，不虚构其他迁移已应用。
INSERT IGNORE INTO `migration_ledger_wsh` (`version_wsh`, `summary_wsh`)
VALUES ('v6_service_product_media', '服务产品媒体表与迁移台账基线（基线补齐）');

-- ---------- 4) 逐段核验（执行后手工确认） ----------
-- SELECT COUNT(*) FROM migration_ledger_wsh WHERE version_wsh = 'v6_service_product_media';  -- 期望 1
-- SELECT COUNT(*) FROM information_schema.TABLES
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pet_service_media_wsh';                -- 期望 1
