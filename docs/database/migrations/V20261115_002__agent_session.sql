-- ============================================================================
-- V20261115_002__agent_session.sql
-- Project : PetService - AI Agent 多轮会话表上线
-- Version : V20261115_002
-- Date    : 2026-11-15
-- Target  : MySQL 8.0 / schema `pet_service`
--
-- 背景：
--   `agent_session_wsh` 目前只存在于 `pet-admin/src/main/resources/db/schema.sql`，
--   该文件仅在「数据目录为空」时被 MySQL 容器执行一次，因此**已有数据库不会自动建表**。
--   Agent 会话读写（AgentSessionMapper / AgentSessionStoreImpl）会在运行期直接报
--   "Table doesn't exist"，属于功能不可用而非降级。
--
-- 授权范围（勿扩大）：
--   [A] 建 `agent_session_wsh` 一张表（7 列 + 主键 + 1 个二级索引）
--   [B] 结构不一致时**硬失败**（SIGNAL SQLSTATE 45000），不允许「表存在即视为迁移成功」
--   [C] 登记迁移台账 V20261115_002_agent_session
--   [D] NO ALTER 既有表 / NO UPDATE / NO DELETE / NO DROP TABLE
--   [E] 不修改 schema.sql（它已包含该表定义，两者必须逐列一致）
--
-- 结构来源（已逐列核对）：
--   schema.sql 的 CREATE TABLE 定义
--   ↔ AgentSession.java 的 @TableField / @TableId
--   ↔ AgentSessionMapper 的三条 CAS UPDATE 语句
--   字段核对结论：7 列一一对应，无多余、无缺失。
--     session_id_wsh  VARCHAR(64) PK，由 AiChatHistoryService.newSessionId() 生成
--                     （UUID 去横线 = 32 字符，64 足够）
--     user_id_wsh     BIGINT NOT NULL
--     title_wsh       VARCHAR(200)，AgentServiceImpl.titleOf() 截断到 40 字符 + "…"
--     state_json_wsh  TEXT，AgentSessionState 的 Jackson 序列化结果
--     status_wsh      VARCHAR(20) DEFAULT 'COLLECTING'，取值 COLLECTING / AWAIT_CONFIRM /
--                     CONFIRMED，最长 13 字符，20 足够
--     created_at_wsh  DATETIME DEFAULT CURRENT_TIMESTAMP
--     updated_at_wsh  DATETIME DEFAULT CURRENT_TIMESTAMP（由 Mapper 显式 SET，无 ON UPDATE）
--   索引 idx_agent_session_user(user_id_wsh, updated_at_wsh) 服务于
--   AgentSessionStoreImpl.listByUser() 的 WHERE user_id_wsh ORDER BY updated_at_wsh DESC
--
-- 执行要求：
--   1. 必须先备份：mysqldump --single-transaction --routines --triggers pet_service \
--        > backup_$(date +%Y%m%d-%H%M).sql
--   2. 用 mysql 命令行客户端执行（脚本使用 DELIMITER，GUI 工具可能不支持）：
--        mysql -u<user> -p pet_service < V20261115_002__agent_session.sql
--   3. 本脚本可重复执行：第二次起 SEGMENT 2 的断言通过、SEGMENT 3 台账 IGNORE。
--   4. MySQL DDL 隐式提交，本脚本非原子；按 SEGMENT 顺序执行并逐段核验。
--
-- 回滚：见 V20261115_002__rollback.sql
-- ============================================================================


-- ============================================================================
-- SEGMENT 1 : 建表（幂等）
-- ============================================================================
SELECT '=== SEGMENT 1 CREATE TABLE ===' AS section;

CREATE TABLE IF NOT EXISTS `agent_session_wsh` (
    `session_id_wsh` VARCHAR(64) NOT NULL COMMENT '会话ID（复用 ai_chat_history_wsh.session_id_wsh）',
    `user_id_wsh` BIGINT NOT NULL COMMENT '用户ID',
    `title_wsh` VARCHAR(200) COMMENT '会话标题（首条用户输入截断）',
    `state_json_wsh` TEXT COMMENT '结构化会话状态 JSON',
    `status_wsh` VARCHAR(20) NOT NULL DEFAULT 'COLLECTING' COMMENT 'COLLECTING/AWAIT_CONFIRM/CONFIRMED',
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`session_id_wsh`),
    INDEX `idx_agent_session_user` (`user_id_wsh`, `updated_at_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能下单会话状态';


-- ============================================================================
-- SEGMENT 2 : 结构断言（只读诊断 + 不一致时硬失败）
--
-- 为什么需要这一段：
--   CREATE TABLE IF NOT EXISTS 在「表已存在」时静默跳过 —— 表存在 ≠ 结构正确。
--   如果某个环境手工建过一张缺列 / 主键不对的同名表，跳过会让迁移假装成功，
--   问题推迟到运行期才以 "Unknown column" 暴露。这里在迁移期就明确报错。
--
-- 判定规则（已写入文档，避免歧义）：
--   * 7 个必需列缺失任一        → 硬失败
--   * 7 个必需列类型不符        → 硬失败
--   * 主键不是 session_id_wsh   → 硬失败
--   * 存在额外列                → 只告警（不失败），由人工判断是否属于本域
--   * 缺 idx_agent_session_user → 自动补建（索引不影响数据正确性）
-- ============================================================================
SELECT '=== SEGMENT 2 PRE-CHECK (read only) ===' AS section;

SELECT DATABASE() AS db, VERSION() AS mysql_version, NOW() AS executed_at;

-- 2.1 表是否存在
SELECT COUNT(*) AS table_present
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_session_wsh';

-- 2.2 逐列诊断（应恰好 7 行，且每行 ok=1）
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT,
       CASE
           WHEN COLUMN_NAME = 'session_id_wsh'  AND COLUMN_TYPE = 'varchar(64)'  AND IS_NULLABLE = 'NO'  THEN 1
           WHEN COLUMN_NAME = 'user_id_wsh'     AND COLUMN_TYPE = 'bigint'       AND IS_NULLABLE = 'NO'  THEN 1
           WHEN COLUMN_NAME = 'title_wsh'       AND COLUMN_TYPE = 'varchar(200)'                        THEN 1
           WHEN COLUMN_NAME = 'state_json_wsh'  AND COLUMN_TYPE = 'text'                                THEN 1
           WHEN COLUMN_NAME = 'status_wsh'      AND COLUMN_TYPE = 'varchar(20)'  AND IS_NULLABLE = 'NO'  THEN 1
           WHEN COLUMN_NAME = 'created_at_wsh'  AND COLUMN_TYPE = 'datetime'                             THEN 1
           WHEN COLUMN_NAME = 'updated_at_wsh'  AND COLUMN_TYPE = 'datetime'                             THEN 1
           ELSE 0
       END AS ok
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_session_wsh'
ORDER BY ORDINAL_POSITION;

-- 2.3 主键
SELECT COLUMN_NAME AS pk_column
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_session_wsh'
  AND CONSTRAINT_NAME = 'PRIMARY';

-- 2.4 额外列（正常应为空集）
SELECT COLUMN_NAME AS unexpected_column
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_session_wsh'
  AND COLUMN_NAME NOT IN ('session_id_wsh','user_id_wsh','title_wsh','state_json_wsh',
                          'status_wsh','created_at_wsh','updated_at_wsh');

-- 2.5 硬断言：任一不满足即 SIGNAL，脚本中止，迁移不得视为成功
DROP PROCEDURE IF EXISTS `tmp_assert_agent_session_wsh`;
DELIMITER $$
CREATE PROCEDURE `tmp_assert_agent_session_wsh`()
BEGIN
    DECLARE v_ok_cols     INT DEFAULT 0;
    DECLARE v_pk          VARCHAR(64) DEFAULT NULL;

    SELECT COUNT(*) INTO v_ok_cols
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_session_wsh'
      AND (
            (COLUMN_NAME = 'session_id_wsh' AND COLUMN_TYPE = 'varchar(64)' AND IS_NULLABLE = 'NO')
         OR (COLUMN_NAME = 'user_id_wsh'    AND COLUMN_TYPE = 'bigint'      AND IS_NULLABLE = 'NO')
         OR (COLUMN_NAME = 'title_wsh'      AND COLUMN_TYPE = 'varchar(200)')
         OR (COLUMN_NAME = 'state_json_wsh' AND COLUMN_TYPE = 'text')
         OR (COLUMN_NAME = 'status_wsh'     AND COLUMN_TYPE = 'varchar(20)' AND IS_NULLABLE = 'NO')
         OR (COLUMN_NAME = 'created_at_wsh' AND COLUMN_TYPE = 'datetime')
         OR (COLUMN_NAME = 'updated_at_wsh' AND COLUMN_TYPE = 'datetime')
      );

    -- 注意：SIGNAL MESSAGE_TEXT 上限 128 字符，此处必须短，详情看 SEGMENT 2 诊断输出
    IF v_ok_cols <> 7 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'agent_session_wsh 结构不一致：7 列未全部匹配，详见 SEGMENT 2 输出';
    END IF;

    SELECT COLUMN_NAME INTO v_pk
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_session_wsh'
      AND CONSTRAINT_NAME = 'PRIMARY'
    LIMIT 1;

    IF v_pk IS NULL OR v_pk <> 'session_id_wsh' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'agent_session_wsh 主键不是 session_id_wsh，详见 SEGMENT 2 输出';
    END IF;
END$$
DELIMITER ;

CALL `tmp_assert_agent_session_wsh`();
DROP PROCEDURE `tmp_assert_agent_session_wsh`;

SELECT '=== SEGMENT 2 PASS: 结构与预期一致 ===' AS section;


-- ============================================================================
-- SEGMENT 3 : 索引补齐（幂等）+ 台账登记
-- ============================================================================
SELECT '=== SEGMENT 3 INDEX + LEDGER ===' AS section;

-- 3.1 缺 idx_agent_session_user 才补建（表由本脚本新建时已自带，此段对重跑是空操作）
SET @has_idx := (
    SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_session_wsh'
      AND INDEX_NAME = 'idx_agent_session_user'
);
SET @ddl := IF(@has_idx = 0,
    'ALTER TABLE `agent_session_wsh` ADD INDEX `idx_agent_session_user` (`user_id_wsh`, `updated_at_wsh`)',
    'SELECT ''index idx_agent_session_user already present'' AS note');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3.2 台账（幂等：不覆盖既有记录）
INSERT IGNORE INTO `migration_ledger_wsh` (`version_wsh`, `summary_wsh`)
VALUES ('V20261115_002_agent_session', 'AI Agent 多轮会话表 agent_session_wsh 上线');


-- ============================================================================
-- SEGMENT 4 : 执行后核验（期望值已写在注释里）
-- ============================================================================
SELECT '=== SEGMENT 4 POST-CHECK ===' AS section;

SELECT COUNT(*) AS table_present                       -- 期望 1
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_session_wsh';

SELECT COUNT(*) AS col_count                           -- 期望 7
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_session_wsh';

SELECT COUNT(DISTINCT INDEX_NAME) AS index_present     -- 期望 1
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_session_wsh'
  AND INDEX_NAME = 'idx_agent_session_user';

SELECT COUNT(*) AS ledger_hit                          -- 期望 1
FROM `migration_ledger_wsh`
WHERE `version_wsh` = 'V20261115_002_agent_session';

SELECT COUNT(*) AS row_count                           -- 全新库期望 0；已有库保留原值
FROM `agent_session_wsh`;
