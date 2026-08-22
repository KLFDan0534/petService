-- ===========================================================================
-- fix_service_media_new_services.sql
-- 为新增服务(127+)补齐 pet_service_media_wsh / file_record_wsh 关联
--
-- 背景：种子数据把服务图片写在了 pet_service_wsh.images_wsh（形如
--       /minio/pet-service/seed-online/xxx.jpg），但未建 file_record_wsh 记录，
--       也缺少 pet_service_media_wsh 关联行。公共 API(只读可信图册媒体行)
--       因此不下发图片，前端列表显示无图。
--
-- 本脚本：① 把 images_wsh 中「/minio/pet-service/ 之后」的对象名补建为
--        file_record_wsh(purpose=product)，去重幂等；
--         ② 为「完全没有媒体关联」的服务整组补齐 pet_service_media_wsh 并置首图为封面。
--
-- 幂等性：file_record 按 object_name_wsh 去重(WHERE NOT EXISTS)；
--          media 仅服务级整组插入(服务至少已有一条 media 则整组跳过)，
--          并依赖 uk_service_sort 唯一键；可重复执行。
-- 无 DELETE/TRUNCATE/DROP/ALTER，仅 INSERT + 少量 UPDATE(封面)。
-- ===========================================================================
USE `pet_service`;

-- 1) 为 images_wsh 中引用的对象补建文件记录（仅缺的，幂等）
INSERT INTO `file_record_wsh`
    (`original_name_wsh`, `object_name_wsh`, `size_wsh`, `content_type_wsh`,
     `purpose_wsh`, `merchant_id_wsh`, `user_id_wsh`, `created_at_wsh`, `updated_at_wsh`)
SELECT SUBSTRING_INDEX(oj.`object_name_wsh`, '/', -1) AS original_name_wsh,
       oj.`object_name_wsh`,
       NULL AS size_wsh,
       NULL AS content_type_wsh,
       'product' AS purpose_wsh,
       oj.`merchant_id_wsh`,
       NULL AS user_id_wsh,
       NOW() AS created_at_wsh,
       NOW() AS updated_at_wsh
FROM (
    SELECT CONCAT(SUBSTRING_INDEX(SUBSTRING_INDEX(TRIM(jt.item), '/', -2), '/', 1),
                  '/',
                  SUBSTRING_INDEX(TRIM(jt.item), '/', -1)) AS object_name_wsh,
           MIN(s.`merchant_id_wsh`) AS merchant_id_wsh
    FROM `pet_service_wsh` s
    JOIN JSON_TABLE(
        CONCAT('["', REPLACE(REPLACE(TRIM(s.images_wsh), ',', '","'), '"', '\"'), '"]'),
        '$[*]' COLUMNS (item VARCHAR(1000) PATH '$', pos FOR ORDINALITY)
    ) jt
    WHERE s.`images_wsh` IS NOT NULL
      AND TRIM(s.`images_wsh`) <> ''
      AND TRIM(jt.item) <> ''
      AND TRIM(jt.item) LIKE '/minio/pet-service/%'
    GROUP BY object_name_wsh
) oj
WHERE NOT EXISTS (
    SELECT 1 FROM `file_record_wsh` f
    WHERE f.`object_name_wsh` = oj.`object_name_wsh`
);

-- 2) 为「有图且完全没有媒体关联」的服务整组补齐媒体行
INSERT INTO `pet_service_media_wsh` (`service_id_wsh`, `file_id_wsh`, `sort_order_wsh`, `is_cover_wsh`)
SELECT ranked.`service_id_wsh`, ranked.`file_id_wsh`, ranked.`rn` - 1,
       CASE WHEN ranked.`rn` = 1 THEN 1 ELSE 0 END
FROM (
    SELECT s.`id_wsh` AS `service_id_wsh`, f.`id_wsh` AS `file_id_wsh`,
           ROW_NUMBER() OVER (PARTITION BY s.`id_wsh` ORDER BY jt.pos) AS `rn`
    FROM `pet_service_wsh` s
    JOIN JSON_TABLE(
        CONCAT('["', REPLACE(REPLACE(TRIM(s.images_wsh), ',', '","'), '"', '\"'), '"]'),
        '$[*]' COLUMNS (item VARCHAR(1000) PATH '$', pos FOR ORDINALITY)
    ) jt
    JOIN `file_record_wsh` f
      ON f.`object_name_wsh` = CONCAT(
             SUBSTRING_INDEX(SUBSTRING_INDEX(TRIM(jt.item), '/', -2), '/', 1),
             '/',
             SUBSTRING_INDEX(TRIM(jt.item), '/', -1)
         )
     AND f.`purpose_wsh` = 'product'
    WHERE s.`images_wsh` IS NOT NULL
      AND TRIM(s.`images_wsh`) <> ''
      AND TRIM(jt.item) <> ''
      AND TRIM(jt.item) LIKE '/minio/pet-service/%'
      AND NOT EXISTS (
          SELECT 1 FROM `pet_service_media_wsh` m
          WHERE m.`service_id_wsh` = s.`id_wsh`
      )
) ranked
ON DUPLICATE KEY UPDATE `id_wsh` = `id_wsh`;

-- 3) 审计输出
SELECT COUNT(*) AS file_records_product FROM `file_record_wsh` WHERE `purpose_wsh` = 'product';
SELECT COUNT(*) AS media_rows FROM `pet_service_media_wsh`;