-- =====================================================================
-- Pet Service 数据库结构修复脚本 (DATABASE REPAIR)
-- 生成时间: 2026-09-10
-- 依据: DATABASE_AUDIT_REPORT.md §4/§5
-- 原则: 仅 ADD COLUMN / ADD INDEX，禁止 DROP/CLEAR/TRUNCATE/DELETE
-- 说明: 当前 MySQL 构建不支持 ADD COLUMN IF NOT EXISTS；
--       下方列已通过 DATABASE_AUDIT_REPORT 逐列确认缺失，脚本一次性执行。
--       如需重复执行请先核对 INFORMATION_SCHEMA（重复 ADD 会报列已存在，可忽略）
-- =====================================================================

USE pet_service;

-- ---------------------------------------------------------------
-- 1. user_wsh: 补实名认证驳回原因字段
-- ---------------------------------------------------------------
ALTER TABLE user_wsh
    ADD COLUMN `reject_reason_wsh` VARCHAR(500) DEFAULT NULL
    COMMENT '实名认证驳回原因' AFTER `real_name_status_wsh`;

-- ---------------------------------------------------------------
-- 2. notice_wsh: 补公告投递方式字段（popup/notification/broadcast 逗号分隔）
-- ---------------------------------------------------------------
ALTER TABLE notice_wsh
    ADD COLUMN `delivery_type_wsh` VARCHAR(50) DEFAULT NULL
    COMMENT '投递方式: popup/notification/broadcast 逗号分隔' AFTER `type_wsh`;

-- ---------------------------------------------------------------
-- 3. keeper_leave_wsh: 补请假审批状态字段
-- ---------------------------------------------------------------
ALTER TABLE keeper_leave_wsh
    ADD COLUMN `status_wsh` VARCHAR(20) NOT NULL DEFAULT 'pending'
    COMMENT '审批状态 pending/approved/rejected' AFTER `reason_wsh`;

-- ---------------------------------------------------------------
-- 4. ticket_message_wsh: 补工单消息附件与已读字段
-- ---------------------------------------------------------------
ALTER TABLE ticket_message_wsh
    ADD COLUMN `file_url_wsh` VARCHAR(500) DEFAULT NULL
    COMMENT '图片附件URL' AFTER `content_wsh`,
    ADD COLUMN `is_read_wsh` TINYINT DEFAULT 0
    COMMENT '是否已读 0-否 1-是' AFTER `file_url_wsh`;

-- ---------------------------------------------------------------
-- 5. wallet_transaction_wsh: 补管理员调账操作人字段
-- ---------------------------------------------------------------
ALTER TABLE wallet_transaction_wsh
    ADD COLUMN `operator_id_wsh` BIGINT DEFAULT NULL
    COMMENT '操作人用户ID，仅管理员调账时记录' AFTER `request_id_wsh`;

-- ---------------------------------------------------------------
-- 6. file_record_wsh: 补文件用途与归属商家字段
-- ---------------------------------------------------------------
ALTER TABLE file_record_wsh
    ADD COLUMN `purpose_wsh` VARCHAR(50) DEFAULT NULL
    COMMENT '文件用途: product-产品图片 avatar-头像 evidence-资质证明 其他' AFTER `content_type_wsh`,
    ADD COLUMN `merchant_id_wsh` BIGINT DEFAULT NULL
    COMMENT '服务端归属商家ID(产品图片等受管文件)' AFTER `purpose_wsh`;

-- ---------------------------------------------------------------
-- 7. wallet_transaction_wsh: 补幂等唯一索引与查询索引（schema.sql 已定义）
--    注意: 表当前为空，无重复冲突；仅执行一次，如重复执行会报索引已存在可忽略
-- ---------------------------------------------------------------
ALTER TABLE wallet_transaction_wsh
    ADD UNIQUE INDEX `uk_wallet_tx_request` (`request_id_wsh`),
    ADD INDEX `idx_wallet_tx_user` (`user_id_wsh`),
    ADD INDEX `idx_wallet_tx_business` (`business_type_wsh`, `business_id_wsh`);

-- 注: pet_service_wsh.idx_merchant_id 当前数据库已存在，无需补充。