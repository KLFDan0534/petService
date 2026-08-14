-- ============================================================
-- v6_service_product_media — 服务产品图片结构化迁移
--
-- 目标版本: v6（服务产品图片结构化）
-- 适用环境: MySQL 8.0+（经 mysql 客户端或等价执行器作为 CI/CD/运维发布步骤执行；
--           应用启动自动执行 h2-schema.sql / KeeperAttendanceSchemaMigrator 类运行时迁移，
--           但不执行本脚本 —— 本脚本是显式的部署前步骤，缺失时应用启动就绪检查会失败）。
--
-- 幂等性: 迁移台账 migration_ledger_wsh 记录版本，已应用则整体跳过；
--         表/列创建均带存在性守卫；回填依赖唯一键，重复执行不产生重复行。
-- 产物:
--   * file_record_wsh 增加 purpose_wsh / merchant_id_wsh
--   * 新建 pet_service_media_wsh
--   * 按原顺序回填 legacy images_wsh 中可解析到 file_record_wsh 内部对象的记录
--     （第一张有效图片为封面），其余值仅输出审计清单，不导入外部 URL
-- ============================================================

-- 0) 迁移台账（幂等守卫）
CREATE TABLE IF NOT EXISTS `migration_ledger_wsh` (
  `version_wsh` VARCHAR(100) NOT NULL COMMENT '迁移版本号',
  `applied_at_wsh` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '应用时间',
  `summary_wsh` VARCHAR(1000) DEFAULT NULL COMMENT '迁移摘要',
  PRIMARY KEY (`version_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据库迁移台账';

DROP PROCEDURE IF EXISTS `migrate_v6_service_product_media`;

DELIMITER //
CREATE PROCEDURE `migrate_v6_service_product_media`()
BEGIN
    DECLARE v_applied INT DEFAULT 0;
    DECLARE v_has_service_table INT DEFAULT 0;
    DECLARE v_has_file_table INT DEFAULT 0;
    DECLARE v_has_purpose_col INT DEFAULT 0;
    DECLARE v_has_merchant_col INT DEFAULT 0;
    DECLARE v_media_rows BIGINT DEFAULT 0;

    SELECT COUNT(*) INTO v_applied
      FROM `migration_ledger_wsh`
     WHERE `version_wsh` = 'v6_service_product_media';

    IF v_applied > 0 THEN
        SELECT 'migration_v6_service_product_media' AS result, 'skipped' AS state,
               'v6 already recorded in migration_ledger_wsh, nothing to do' AS message;
    ELSE
        -- 1) 断言目标环境：必须存在服务表与文件记录表
        SELECT COUNT(*) INTO v_has_service_table
          FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pet_service_wsh';
        SELECT COUNT(*) INTO v_has_file_table
          FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'file_record_wsh';

        IF v_has_service_table = 0 OR v_has_file_table = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'migration v6 aborted: pet_service_wsh / file_record_wsh not found in target schema';
        END IF;

        -- 2) file_record_wsh 增加产品用途与商家归属列（幂等）
        SELECT COUNT(*) INTO v_has_purpose_col
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'file_record_wsh' AND COLUMN_NAME = 'purpose_wsh';
        IF v_has_purpose_col = 0 THEN
            ALTER TABLE `file_record_wsh`
                ADD COLUMN `purpose_wsh` VARCHAR(30) DEFAULT NULL
                COMMENT '文件用途: product-产品图片 avatar-头像 evidence-资质证明 其他'
                AFTER `content_type_wsh`;
        END IF;

        SELECT COUNT(*) INTO v_has_merchant_col
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'file_record_wsh' AND COLUMN_NAME = 'merchant_id_wsh';
        IF v_has_merchant_col = 0 THEN
            ALTER TABLE `file_record_wsh`
                ADD COLUMN `merchant_id_wsh` BIGINT DEFAULT NULL
                COMMENT '服务端归属商家ID(产品图片等受管文件)'
                AFTER `purpose_wsh`;
        END IF;

        IF v_has_purpose_col = 0 OR v_has_merchant_col = 0 THEN
            ALTER TABLE `file_record_wsh` ADD KEY `idx_purpose_merchant` (`purpose_wsh`, `merchant_id_wsh`);
        END IF;

        -- 3) 新建媒体表（幂等 + 唯一键保护）
        CREATE TABLE IF NOT EXISTS `pet_service_media_wsh` (
            `id_wsh` BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
            `service_id_wsh` BIGINT NOT NULL COMMENT '服务产品ID(pet_service_wsh.id_wsh)',
            `file_id_wsh` BIGINT NOT NULL COMMENT '文件记录ID(file_record_wsh.id_wsh)',
            `sort_order_wsh` INT NOT NULL DEFAULT '0' COMMENT '排序序号(0..N 连续)',
            `is_cover_wsh` TINYINT NOT NULL DEFAULT '0' COMMENT '是否封面: 0-否 1-是',
            `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            PRIMARY KEY (`id_wsh`),
            UNIQUE KEY `uk_service_file` (`service_id_wsh`, `file_id_wsh`) COMMENT '同一服务不重复引用同一文件',
            UNIQUE KEY `uk_service_sort` (`service_id_wsh`, `sort_order_wsh`) COMMENT '同一服务内排序唯一',
            KEY `idx_media_file` (`file_id_wsh`) COMMENT '文件索引'
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务产品图片表';

        -- 4) 回填 legacy images_wsh（逗号分隔，原顺序，仅内部可解析对象）
        --    匹配规则: 值解析为最后两级路径并与 file_record_wsh.object_name_wsh 精确相等，
        --    兼容完整URL / 相对URL / 裸对象名三类写法，外部 URL 自然不匹配。
        INSERT INTO `pet_service_media_wsh` (`service_id_wsh`, `file_id_wsh`, `sort_order_wsh`)
        SELECT ranked.`service_id_wsh`, ranked.`file_id_wsh`, ranked.`rn` - 1
        FROM (
            SELECT s.`id_wsh` AS `service_id_wsh`, f.`id_wsh` AS `file_id_wsh`,
                   ROW_NUMBER() OVER (PARTITION BY s.`id_wsh` ORDER BY jt.pos) AS `rn`
            FROM `pet_service_wsh` s
            JOIN JSON_TABLE(
                CONCAT('["', REPLACE(REPLACE(s.images_wsh, ',', '","'), '"', '\"'), '"]'),
                '$[*]' COLUMNS (item VARCHAR(1000) PATH '$', pos FOR ORDINALITY)
            ) jt
            JOIN `file_record_wsh` f
              ON f.`object_name_wsh` = CONCAT(
                     SUBSTRING_INDEX(SUBSTRING_INDEX(TRIM(jt.item), '/', -2), '/', 1),
                     '/',
                     SUBSTRING_INDEX(TRIM(jt.item), '/', -1)
                 )
            WHERE s.`images_wsh` IS NOT NULL
              AND TRIM(s.`images_wsh`) <> ''
              AND TRIM(jt.item) <> ''
        ) ranked
        ON DUPLICATE KEY UPDATE `id_wsh` = `id_wsh`;

        -- 5) 封面：每个服务的第一张有效图片（最小排序）且尚无封面时置为封面
        UPDATE `pet_service_media_wsh` m
        JOIN (
            SELECT `service_id_wsh`, MIN(`sort_order_wsh`) AS `first_sort`
            FROM `pet_service_media_wsh`
            GROUP BY `service_id_wsh`
        ) g ON g.`service_id_wsh` = m.`service_id_wsh` AND g.`first_sort` = m.`sort_order_wsh`
        LEFT JOIN (
            SELECT DISTINCT `service_id_wsh`
            FROM `pet_service_media_wsh`
            WHERE `is_cover_wsh` = 1
        ) c ON c.`service_id_wsh` = m.`service_id_wsh`
        SET m.`is_cover_wsh` = 1
        WHERE c.`service_id_wsh` IS NULL;

        -- 6) 记录台账
        INSERT INTO `migration_ledger_wsh` (`version_wsh`, `summary_wsh`)
        VALUES ('v6_service_product_media', 'pet_service_media_wsh created; file_record_wsh gained purpose_wsh/merchant_id_wsh; legacy images backfilled');

        -- 7) 审计输出: 各服务 legacy 条目数 + 实际导入数（基于已回填的媒体表，去重后）+ 未解析数
        --    （注意: MySQL 8.0.46 解析器存在 JSON_TABLE 与 GROUP BY/ORDER BY 同查询的 1064
        --    解析缺陷，因此审计不再直接展开 JSON_TABLE，而是按导入结果反推）
        SELECT s.`id_wsh` AS service_id_wsh,
               (LENGTH(TRIM(s.`images_wsh`))
                - LENGTH(REPLACE(TRIM(s.`images_wsh`), ',', ''))
                + 1) AS legacy_items,
               COUNT(m.`id_wsh`) AS resolved,
               (LENGTH(TRIM(s.`images_wsh`))
                - LENGTH(REPLACE(TRIM(s.`images_wsh`), ',', ''))
                + 1 - COUNT(m.`id_wsh`)) AS unresolved
        FROM `pet_service_wsh` s
        LEFT JOIN `pet_service_media_wsh` m
          ON m.`service_id_wsh` = s.`id_wsh`
        WHERE s.`images_wsh` IS NOT NULL AND TRIM(s.`images_wsh`) <> ''
        GROUP BY s.`id_wsh`
        ORDER BY s.`id_wsh`;

        SELECT COUNT(*) INTO v_media_rows FROM `pet_service_media_wsh`;
        SELECT 'migration_v6_service_product_media' AS result, 'applied' AS state,
               CONCAT('media rows now = ', v_media_rows) AS message;
    END IF;
END //
DELIMITER ;

CALL `migrate_v6_service_product_media`();
DROP PROCEDURE IF EXISTS `migrate_v6_service_product_media`;