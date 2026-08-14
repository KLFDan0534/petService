-- ============================================================
-- backfill_service_image_files — 为 legacy 服务图片补建文件记录与媒体关联
--
-- 背景: v6_service_product_media 迁移只回填「能解析到 file_record_wsh 内部对象」的
--       legacy images_wsh；种子数据（seed.sql / 历史写入）的服务图片从未建立
--       file_record_wsh 记录，导致 pet_service_media_wsh 为空、公共 API 按
--       KTD9「仅下发可信图册」规则不下发任何图片。
--
-- 本脚本: 把 pet_service_wsh.images_wsh 中「最后两级路径」解析出的对象名
--       补建为 file_record_wsh（purpose=product、归属商家=服务商家），
--       再按 v6 同样的匹配规则回填 pet_service_media_wsh 并设定封面。
--       外部 URL（非 /minio 内部对象）不匹配，不会导入。
--
-- 幂等性: 文件记录按 object_name_wsh 去重（WHERE NOT EXISTS）；
--         媒体关联依赖 uk_service_file/uk_service_sort 唯一键（ON DUPLICATE KEY）。
-- 适用: MySQL 8.0+，mysql 客户端执行；应用启动就绪检查已通过后亦可随时重放。
-- ============================================================

-- 0) 修复历史重复（自愈段）：同一 object_name 仅保留最小 id 的文件记录
DELETE m FROM `pet_service_media_wsh` m
JOIN `file_record_wsh` f ON f.`id_wsh` = m.`file_id_wsh`
JOIN (
    SELECT `object_name_wsh`, MIN(`id_wsh`) AS `keep_id`
    FROM `file_record_wsh`
    WHERE `purpose_wsh` = 'product'
    GROUP BY `object_name_wsh`
) k ON k.`object_name_wsh` = f.`object_name_wsh`
WHERE f.`id_wsh` <> k.`keep_id`;

DELETE f FROM `file_record_wsh` f
JOIN (
    SELECT `object_name_wsh`, MIN(`id_wsh`) AS `keep_id`
    FROM `file_record_wsh`
    WHERE `purpose_wsh` = 'product'
    GROUP BY `object_name_wsh`
) k ON k.`object_name_wsh` = f.`object_name_wsh`
WHERE f.`purpose_wsh` = 'product' AND f.`id_wsh` <> k.`keep_id`;

-- 1) 补建文件记录（仅 legacy 服务图，purpose=product；对象名去重，商家取最小 id 服务归属）
INSERT INTO `file_record_wsh` (`object_name_wsh`, `original_name_wsh`, `size_wsh`, `content_type_wsh`, `purpose_wsh`, `merchant_id_wsh`)
SELECT obj.`object_name_wsh`,
       SUBSTRING_INDEX(obj.`object_name_wsh`, '/', -1) AS original_name_wsh,
       NULL AS size_wsh,
       NULL AS content_type_wsh,
       'product' AS purpose_wsh,
       obj.`merchant_id_wsh`
FROM (
    SELECT CONCAT(
               SUBSTRING_INDEX(SUBSTRING_INDEX(TRIM(jt.item), '/', -2), '/', 1),
               '/',
               SUBSTRING_INDEX(TRIM(jt.item), '/', -1)
           ) AS object_name_wsh,
           MIN(s.`merchant_id_wsh`) AS merchant_id_wsh
    FROM `pet_service_wsh` s
    JOIN JSON_TABLE(
        CONCAT('["', REPLACE(REPLACE(s.images_wsh, ',', '","'), '"', '\"'), '"]'),
        '$[*]' COLUMNS (item VARCHAR(1000) PATH '$', pos FOR ORDINALITY)
    ) jt
    WHERE s.`images_wsh` IS NOT NULL
      AND TRIM(s.`images_wsh`) <> ''
      AND TRIM(jt.item) <> ''
      AND TRIM(jt.item) NOT LIKE 'http://%'
      AND TRIM(jt.item) NOT LIKE 'https://%'
      AND TRIM(jt.item) NOT LIKE '//%'
    GROUP BY object_name_wsh
) obj
WHERE NOT EXISTS (
    SELECT 1 FROM `file_record_wsh` f
    WHERE f.`object_name_wsh` = obj.`object_name_wsh`
);

-- 2) 回填媒体关联（原顺序，与 v6 迁移同一匹配规则）
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

-- 3) 封面：每个服务的第一张有效图片（最小排序）且尚无封面时置为封面
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

-- 4) 审计输出
SELECT COUNT(*) AS file_records_created FROM `file_record_wsh` WHERE `purpose_wsh` = 'product';
SELECT COUNT(*) AS media_rows FROM `pet_service_media_wsh`;
