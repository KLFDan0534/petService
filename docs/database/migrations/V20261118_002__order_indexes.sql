-- =============================================================================
-- V20261118_002 · pet_order_wsh 业务查询索引
--
-- 背景（实测）：
--   pet_order_wsh 共 68 列、3 个 TEXT（membership_snapshot_wsh /
--   promotion_snapshot_wsh / remark_wsh），除 PRIMARY(id_wsh) 与
--   UNIQUE(order_no_wsh) 外**没有任何业务索引**。
--   所有按 owner / merchant / keeper / status / 日期区间的查询都退化为
--   全表扫描 + filesort。
--
-- 设计依据（每条索引逐条对应真实查询，不做"可能有用"的索引）：
--
--   ① listByOwner / 游标分页
--      WHERE owner_id_wsh=? [AND status_wsh=?] ORDER BY created_at_wsh DESC
--      → (owner_id_wsh, created_at_wsh)
--      status_wsh 不做前导列：只有 10 个取值，选择性过低，放中间会让
--      created_at 的排序失去索引支持，反而触发 filesort。
--
--   ② listByMerchant / 后台按商家筛选
--      WHERE merchant_id_wsh=? ORDER BY created_at_wsh DESC
--      → (merchant_id_wsh, created_at_wsh)
--
--   ③ listByKeeper / 看护者任务列表
--      WHERE keeper_id_wsh=? AND status_wsh NOT IN (...) ORDER BY created_at_wsh DESC
--      → (keeper_id_wsh, created_at_wsh)
--      注：refreshKeeperCurrentPets 的
--          COUNT WHERE keeper_id_wsh=? AND status_wsh IN (...)
--      同样走这条索引（前导列等值 + 残余过滤），单个看护者名下订单量在
--      百级，不值得再加一条 (keeper_id, status) 索引承担额外写入成本。
--
--   ④ 支付超时兜底任务（每 60 秒）
--      WHERE status_wsh='pending' AND created_at_wsh<=? ORDER BY created_at_wsh ASC LIMIT 100
--      → (status_wsh, created_at_wsh)
--      没有这条索引时，即使只要 100 条也必须扫全表再排序。
--
--   ⑤ 接单超时兜底任务（每 60 秒）
--      WHERE status_wsh='paid' AND updated_at_wsh<=? ORDER BY updated_at_wsh ASC LIMIT 100
--      → (status_wsh, updated_at_wsh)
--      与 ④ 排序键不同，不能合并。
--
--   ⑥ 宠物排班冲突校验（下单 / 预览高频）
--      WHERE pet_id_wsh=? AND status_wsh IN (...) AND 日期区间重叠
--      → (pet_id_wsh, start_date_wsh, end_date_wsh)
--
--   ⑦ 看护者区间容量校验（下单 / 可用性查询高频）
--      WHERE keeper_id_wsh=? AND status_wsh IN (...) AND 日期区间重叠
--      → (keeper_id_wsh, start_date_wsh, end_date_wsh)
--      与 ③ 不能合并：③ 排 created_at，⑦ 排日期区间，排序键完全不同。
--
--   不建 deleted_wsh 索引：@TableLogic 让每条查询都带 deleted_wsh=0，
--   但该列 99% 为 0，没有过滤价值，放进索引只会增宽索引页。
--
--   不建 (owner_id, status, created_at) 这类"全都要"索引：
--   一次只能命中一个复合索引，把 status 塞进中间会牺牲排序，得不偿失。
--
-- 幂等：全部通过 information_schema 判存在再建，重复执行安全。
-- 回滚：V20261118_002__rollback.sql
-- 执行纪律：MySQL DDL 隐式提交，非事务原子；执行前必须备份。
-- =============================================================================

DROP PROCEDURE IF EXISTS `tmp_order_add_index`;

DELIMITER $$
CREATE PROCEDURE `tmp_order_add_index`(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols  VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND INDEX_NAME = p_index
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('created index ', p_index, ' on ', p_table) AS step_result;
    ELSE
        SELECT CONCAT('index ', p_index, ' already exists on ', p_table) AS step_result;
    END IF;
END$$
DELIMITER ;

-- ① 用户端「我的订单」列表 + 游标分页
CALL `tmp_order_add_index`('pet_order_wsh', 'idx_order_owner_created', '`owner_id_wsh`,`created_at_wsh`');

-- ② 商家端订单列表
CALL `tmp_order_add_index`('pet_order_wsh', 'idx_order_merchant_created', '`merchant_id_wsh`,`created_at_wsh`');

-- ③ 看护者任务列表 + 在养数量统计
CALL `tmp_order_add_index`('pet_order_wsh', 'idx_order_keeper_created', '`keeper_id_wsh`,`created_at_wsh`');

-- ④ 支付超时兜底任务（status=pending，按创建时间取最早）
CALL `tmp_order_add_index`('pet_order_wsh', 'idx_order_status_created', '`status_wsh`,`created_at_wsh`');

-- ⑤ 接单超时兜底任务（status=paid，按更新时间取最早）
CALL `tmp_order_add_index`('pet_order_wsh', 'idx_order_status_updated', '`status_wsh`,`updated_at_wsh`');

-- ⑥ 宠物排班冲突校验
CALL `tmp_order_add_index`('pet_order_wsh', 'idx_order_pet_date', '`pet_id_wsh`,`start_date_wsh`,`end_date_wsh`');

-- ⑦ 看护者区间容量校验
CALL `tmp_order_add_index`('pet_order_wsh', 'idx_order_keeper_date', '`keeper_id_wsh`,`start_date_wsh`,`end_date_wsh`');

DROP PROCEDURE IF EXISTS `tmp_order_add_index`;

-- =============================================================================
-- 执行后自检：应看到 7 个新索引（加上 PRIMARY 与 order_no_wsh 共 9 行）
-- =============================================================================
SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns_in_order
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'pet_order_wsh'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY INDEX_NAME;
