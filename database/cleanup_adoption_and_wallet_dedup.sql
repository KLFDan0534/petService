-- ===========================================================================
-- 清理领养数据 + 钱包去重 + 建立钱包唯一索引
-- 执行说明：本脚本仅清理领养相关数据，清理钱包重复记录（合并余额），
--           并为 wallet_wsh 建立 uk_wallet_user 唯一索引。
--           所有 DELETE 均带范围限定，不影响其他业务数据。
-- ===========================================================================

-- 1) 清理领养宠物表数据（仅此表，不影响其他数据）
DELETE FROM adoption_pet_wsh;

-- 2) 清理领养申请表数据（仅此表）
DELETE FROM adoption_application_wsh;

-- 3) 钱包去重：将重复钱包的余额/冻结金额合并到最早创建的主钱包
--    主钱包取每个 user_id_wsh 下 id_wsh 最小的一条。
--    3.1 先把重复钱包的金额累加进主钱包
UPDATE wallet_wsh w
JOIN (
    SELECT user_id_wsh, MIN(id_wsh) AS keep_id
    FROM wallet_wsh
    WHERE deleted_wsh = 0
    GROUP BY user_id_wsh
    HAVING COUNT(*) > 1
) dup ON dup.user_id_wsh = w.user_id_wsh
   AND w.id_wsh <> dup.keep_id
JOIN wallet_wsh main ON main.id_wsh = dup.keep_id
SET main.balance_wsh = main.balance_wsh + w.balance_wsh,
    main.frozen_amount_wsh = ROUND(main.frozen_amount_wsh + w.frozen_amount_wsh, 2);

-- 3.2 删除已被合并的重复钱包记录（仅重复钱包，主钱包保留）
DELETE w
FROM wallet_wsh w
JOIN (
    SELECT user_id_wsh, MIN(id_wsh) AS keep_id
    FROM wallet_wsh
    WHERE deleted_wsh = 0
    GROUP BY user_id_wsh
    HAVING COUNT(*) > 1
) dup ON dup.user_id_wsh = w.user_id_wsh
   AND w.id_wsh <> dup.keep_id;

-- 4) 为钱包表建立 user_id_wsh 唯一索引（确保每个用户只有一个钱包）
ALTER TABLE wallet_wsh
  ADD UNIQUE KEY `uk_wallet_user` (`user_id_wsh`);