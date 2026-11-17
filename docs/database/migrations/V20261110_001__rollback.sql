-- ============================================================================
-- V20261110_001__rollback.sql
-- Rollback for V20261110_001__pricing_unit_unify.sql
-- Version : V20261110_001 (rollback)
-- Date    : 2026-11-10
-- Target  : MySQL 8.0.46 / schema `pet_service` (Docker pet-mysql:3308)
--
-- !! READ BEFORE EXECUTING !!
--   1. MySQL DDL implicitly commits. The forward migration's DDL segments were
--      NOT atomic and CANNOT be rolled back by ROLLBACK. Structural changes
--      (DROP COLUMN / MODIFY COLUMN) are NOT safely automatically reversible.
--   2. This script does NOT restore a database dump. Full restore uses
--      .workbuddy-ai/tmp/pricing-migration-20261110/pet_service_full_backup.sql
--      and is a separate, manually approved operation.
--   3. Historical orders are NEVER updated by this rollback. All 59 orders keep
--      their original amounts, units, quantities, prices, statuses and times.
--   4. NO DROP DATABASE / TRUNCATE / volume rebuild / DROP TABLE.
--   5. Execute segment by segment and verify each CHECK, exactly like the forward
--      migration. Re-run protection: SEGMENT 0 guards against double rollback.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- SEGMENT 0 : PRE-CHECK (read only)
-- ----------------------------------------------------------------------------
SELECT '=== ROLLBACK SEGMENT 0 PRE-CHECK ===' AS section;
SELECT DATABASE() AS db, VERSION() AS mysql_version, NOW() AS executed_at;
SELECT COUNT(*) AS order_cols_present FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pet_order_wsh'
  AND COLUMN_NAME IN ('pickup_code_wsh','checkout_confirmed_at_wsh','overtime_hours_wsh',
                      'overtime_amount_wsh','overtime_payment_id_wsh');         -- expect 5 if forward applied
SELECT COUNT(*) AS purpose_present FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payment_wsh' AND COLUMN_NAME='purpose_wsh';  -- expect 1
SELECT id_wsh, unit_wsh FROM pet_service_wsh WHERE id_wsh IN (7,8,20,21,25,26,35) ORDER BY id_wsh;  -- expect night
SELECT COUNT(*) AS orders, SUM(total_amount_wsh) AS sum_total, SUM(final_amount_wsh) AS sum_final
FROM pet_order_wsh;                                                            -- expect 59 / 22120.00 / 20790.00


-- ----------------------------------------------------------------------------
-- SEGMENT 1 : revert the 7 whitelisted services (transactional DML)
--   Each statement is guarded by exact id AND the value set by the forward
--   migration. Unconfirmed services 1,2,3,9,29,30,36 are NOT touched.
-- ----------------------------------------------------------------------------
SELECT '=== ROLLBACK SEGMENT 1 : whitelist DML ===' AS section;
START TRANSACTION;

UPDATE pet_service_wsh SET unit_wsh = 'boarding' WHERE id_wsh = 7  AND unit_wsh = 'night';
UPDATE pet_service_wsh SET unit_wsh = 'boarding' WHERE id_wsh = 8  AND unit_wsh = 'night';
UPDATE pet_service_wsh SET unit_wsh = 'boarding' WHERE id_wsh = 20 AND unit_wsh = 'night';
UPDATE pet_service_wsh SET unit_wsh = 'boarding' WHERE id_wsh = 21 AND unit_wsh = 'night';
UPDATE pet_service_wsh SET unit_wsh = 'boarding' WHERE id_wsh = 25 AND unit_wsh = 'night';
UPDATE pet_service_wsh SET unit_wsh = 'boarding' WHERE id_wsh = 26 AND unit_wsh = 'night';
UPDATE pet_service_wsh SET unit_wsh = 'boarding' WHERE id_wsh = 35 AND unit_wsh = 'night';

SELECT COUNT(*) AS whitelist_still_night FROM pet_service_wsh
WHERE id_wsh IN (7,8,20,21,25,26,35) AND unit_wsh = 'night';                  -- expect 0
COMMIT;

SELECT unit_wsh, COUNT(*) AS cnt FROM pet_service_wsh GROUP BY unit_wsh;       -- expect session22/boarding11/day2/time1


-- ----------------------------------------------------------------------------
-- SEGMENT 2 : restore original column defaults / comments (DDL - implicit commit)
--   Restores the pre-migration metadata exactly. Does NOT change any stored row.
-- ----------------------------------------------------------------------------
SELECT '=== ROLLBACK SEGMENT 2 : metadata ===' AS section;

ALTER TABLE pet_service_wsh
  MODIFY COLUMN unit_wsh VARCHAR(20) NULL DEFAULT 'day' COMMENT '计费单位 day/session/hour';

ALTER TABLE pet_order_wsh
  MODIFY COLUMN billing_unit_wsh VARCHAR(20) NULL DEFAULT 'day' COMMENT '计费单位快照 day/session/hour';

ALTER TABLE pet_service_wsh
  MODIFY COLUMN booking_mode_wsh VARCHAR(20) NULL DEFAULT 'date_range' COMMENT 'date_range(day)/slot(session, hour)';

ALTER TABLE pet_order_wsh
  MODIFY COLUMN quantity_wsh INT NULL DEFAULT 1 COMMENT '计费数量：day=天数，session/hour=槽位数';

SELECT TABLE_NAME, COLUMN_NAME, IFNULL(COLUMN_DEFAULT,'<NO DEFAULT>') AS def
FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
  AND ((TABLE_NAME='pet_service_wsh' AND COLUMN_NAME IN ('unit_wsh','booking_mode_wsh'))
    OR (TABLE_NAME='pet_order_wsh'  AND COLUMN_NAME IN ('billing_unit_wsh','quantity_wsh')))
ORDER BY TABLE_NAME, ORDINAL_POSITION;


-- ----------------------------------------------------------------------------
-- SEGMENT 3 : drop the added columns (DDL - NOT safely automatically reversible)
--   !! MANUAL APPROVAL REQUIRED BEFORE THIS SEGMENT !!
--   Dropping these columns destroys any data written to them AFTER the forward
--   migration (e.g. pickup codes / overtime amounts of NEW orders created after
--   the migration). Verify SEGMENT 3 CHECK below first. If any new order has
--   used these columns, DO NOT drop them - restore from the backup instead.
-- ----------------------------------------------------------------------------
SELECT '=== ROLLBACK SEGMENT 3 GUARD (read only) ===' AS section;
SELECT COUNT(*) AS new_orders_with_data FROM pet_order_wsh
WHERE pickup_code_wsh IS NOT NULL OR checkout_confirmed_at_wsh IS NOT NULL
   OR overtime_hours_wsh <> 0 OR overtime_amount_wsh <> 0
   OR overtime_payment_id_wsh IS NOT NULL;                                     -- must be 0 before DROP
SELECT COUNT(*) AS payments_not_default_purpose FROM payment_wsh WHERE purpose_wsh <> 'order';  -- must be 0 before DROP

-- Uncomment ONLY after the guard above returns 0 AND manual approval:
-- ALTER TABLE pet_order_wsh DROP COLUMN pickup_code_wsh;
-- ALTER TABLE pet_order_wsh DROP COLUMN checkout_confirmed_at_wsh;
-- ALTER TABLE pet_order_wsh DROP COLUMN overtime_hours_wsh;
-- ALTER TABLE pet_order_wsh DROP COLUMN overtime_amount_wsh;
-- ALTER TABLE pet_order_wsh DROP COLUMN overtime_payment_id_wsh;
-- ALTER TABLE payment_wsh    DROP COLUMN purpose_wsh;


-- ----------------------------------------------------------------------------
-- SEGMENT 4 : POST-CHECK
-- ----------------------------------------------------------------------------
SELECT '=== ROLLBACK SEGMENT 4 POST-CHECK ===' AS section;
SELECT COUNT(*) AS orders, SUM(total_amount_wsh) AS sum_total, SUM(final_amount_wsh) AS sum_final
FROM pet_order_wsh;                                                            -- must equal SEGMENT 0
SELECT billing_unit_wsh, COUNT(*) AS cnt FROM pet_order_wsh GROUP BY billing_unit_wsh;  -- unchanged
SELECT COUNT(*) AS payment_rows FROM payment_wsh;                              -- expect 46

-- ============================================================================
-- NOT PROVIDED (by design): DROP DATABASE, TRUNCATE, data-volume rebuild,
-- full-schema restore, or any UPDATE of historical order/payment rows.
-- ============================================================================
