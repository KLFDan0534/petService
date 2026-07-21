-- ============================================================
-- migration_v2_category_id_wsh.sql
-- 目的：添加 category_id_wsh 列 + 创建 service_category_wsh 表
-- 特性：幂等（可重复执行）
-- 执行：mysql -u root -p pet_service < migration_v2_category_id_wsh.sql
-- ============================================================

-- ============================================================
-- 1. 创建服务分类表
-- ============================================================
CREATE TABLE IF NOT EXISTS `service_category_wsh` (
    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `parent_id_wsh` BIGINT DEFAULT NULL COMMENT '父级分类ID，NULL为顶级分类',
    `name_wsh` VARCHAR(50) NOT NULL COMMENT '分类名称（中文）',
    `code_wsh` VARCHAR(50) NOT NULL UNIQUE COMMENT '分类编码，全大写下划线风格',
    `sort_wsh` INT DEFAULT 0 COMMENT '排序号',
    `status_wsh` TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    `deleted_wsh` TINYINT DEFAULT 0,
    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_parent_id` (`parent_id_wsh`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务分类表';

-- ============================================================
-- 2. 给 pet_service_wsh 加 category_id_wsh 列（幂等）
-- ============================================================
SET @dbname = DATABASE();
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = @dbname
                 AND TABLE_NAME = 'pet_service_wsh'
                 AND COLUMN_NAME = 'category_id_wsh');
SET @sql = IF(@exists = 0,
    'ALTER TABLE `pet_service_wsh` ADD COLUMN `category_id_wsh` BIGINT DEFAULT NULL COMMENT ''关联service_category_wsh.id_wsh'' AFTER `type_wsh`',
    'SELECT ''column category_id_wsh already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 3. 给 category_id_wsh 加索引（幂等）
-- ============================================================
SET @exists_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = @dbname
                     AND TABLE_NAME = 'pet_service_wsh'
                     AND INDEX_NAME = 'idx_category_id');
SET @sql_idx = IF(@exists_idx = 0,
    'ALTER TABLE `pet_service_wsh` ADD INDEX `idx_category_id` (`category_id_wsh`)',
    'SELECT ''index idx_category_id already exists''');
PREPARE stmt_idx FROM @sql_idx;
EXECUTE stmt_idx;
DEALLOCATE PREPARE stmt_idx;

-- ============================================================
-- 4. 插入服务分类种子数据（幂等 INSERT IGNORE）
-- ============================================================
INSERT IGNORE INTO `service_category_wsh` (`id_wsh`, `parent_id_wsh`, `name_wsh`, `code_wsh`, `sort_wsh`, `status_wsh`) VALUES
-- Level 1
(1, NULL, '寄养服务', 'BOARDING', 1, 1),
(2, NULL, '美容护理', 'GROOMING', 2, 1),
(3, NULL, '训练服务', 'TRAINING', 3, 1),
(4, NULL, '遛宠服务', 'WALK', 4, 1),
(5, NULL, '医疗保健', 'MEDICAL', 5, 1),
-- Level 2 - 寄养服务
(11, 1, '标准寄养', 'BOARDING_STANDARD', 1, 1),
(12, 1, '豪华寄养', 'BOARDING_VIP', 2, 1),
(13, 1, '日间寄养', 'BOARDING_DAYCARE', 3, 1),
-- Level 2 - 美容护理
(21, 2, '基础美容', 'GROOMING_BASIC', 1, 1),
(22, 2, '全效美容', 'GROOMING_FULL', 2, 1),
(23, 2, 'SPA护理', 'GROOMING_SPA', 3, 1),
-- Level 2 - 训练服务
(31, 3, '基础训练', 'TRAINING_BASIC', 1, 1),
(32, 3, '进阶训练', 'TRAINING_ADVANCED', 2, 1),
(33, 3, '行为矫正', 'TRAINING_BEHAVIOR', 3, 1),
-- Level 2 - 遛宠服务
(41, 4, '标准遛弯', 'WALK_STANDARD', 1, 1),
(42, 4, '长时间遛弯', 'WALK_EXTENDED', 2, 1),
-- Level 2 - 医疗保健
(51, 5, '常规体检', 'MEDICAL_CHECKUP', 1, 1),
(52, 5, '疫苗接种', 'MEDICAL_VACCINE', 2, 1),
(53, 5, '驱虫护理', 'MEDICAL_DEWORM', 3, 1),
(54, 5, '术后护理', 'MEDICAL_POSTOP', 4, 1);

-- ============================================================
-- 5. 历史数据迁移：type_wsh → category_id_wsh
-- ============================================================
-- 映射规则（大小写不敏感、前缀匹配）：
--   boarding / BOARDING_STANDARD / BOARDING_VIP / BOARDING_DAYCARE → 11（标准寄养）
--   grooming / GROOMING_BASIC / GROOMING_FULL / GROOMING_SPA → 21（基础美容）
--   training / TRAINING_BASIC / TRAINING_ADVANCED / TRAINING_BEHAVIOR → 31（基础训练）
--   walk / WALK_STANDARD / WALK_EXTENDED → 41（标准遛弯）
--   medical / MEDICAL_CHECKUP / MEDICAL_VACCINE / MEDICAL_DEWORM / MEDICAL_POSTOP → 51（常规体检）

UPDATE `pet_service_wsh`
SET `category_id_wsh` = 11
WHERE `category_id_wsh` IS NULL
  AND (LOWER(`type_wsh`) IN ('boarding', 'boarding_standard')
    OR LOWER(`type_wsh`) LIKE 'boarding_%');

UPDATE `pet_service_wsh`
SET `category_id_wsh` = 21
WHERE `category_id_wsh` IS NULL
  AND (LOWER(`type_wsh`) IN ('grooming', 'grooming_basic')
    OR LOWER(`type_wsh`) LIKE 'grooming_%');

UPDATE `pet_service_wsh`
SET `category_id_wsh` = 31
WHERE `category_id_wsh` IS NULL
  AND (LOWER(`type_wsh`) IN ('training', 'training_basic')
    OR LOWER(`type_wsh`) LIKE 'training_%');

UPDATE `pet_service_wsh`
SET `category_id_wsh` = 41
WHERE `category_id_wsh` IS NULL
  AND (LOWER(`type_wsh`) IN ('walk', 'walk_standard')
    OR LOWER(`type_wsh`) LIKE 'walk_%');

UPDATE `pet_service_wsh`
SET `category_id_wsh` = 51
WHERE `category_id_wsh` IS NULL
  AND (LOWER(`type_wsh`) IN ('medical', 'medical_checkup')
    OR LOWER(`type_wsh`) LIKE 'medical_%');

-- ============================================================
-- 6. 报告无法映射的历史数据
-- ============================================================
SELECT 'UNMAPPED pet_service_wsh records (category_id_wsh still NULL):' AS report;
SELECT `id_wsh`, `name_wsh`, `type_wsh`, `merchant_id_wsh`
FROM `pet_service_wsh`
WHERE `category_id_wsh` IS NULL AND `deleted_wsh` = 0;
