-- ============================================================================
-- V20261110_001__pricing_unit_unify.sql
-- Project : PetService - pricing unit unification (Phase D, conditional approval)
-- Version : V20261110_001
-- Date    : 2026-11-10
-- Target  : MySQL 8.0.46 / schema `pet_service` (Docker container pet-mysql, host port 3308)
-- Backup  : .workbuddy-ai/tmp/pricing-migration-20261110/pet_service_full_backup.sql
--           SHA256 ad10bb0e80277b9201146d55a2a0e343bbd8e66a48b75a749b1892859431b653
--           Verified: 306149 bytes / 52 CREATE TABLE / 51 INSERT INTO / "-- Dump completed"
-- Baseline: .workbuddy-ai/tmp/pricing-migration-20261110/before_*.tsv (36/59/8/46 rows)
--
-- APPROVED SCOPE (do not extend):
--   [A] Add 5 columns to pet_order_wsh : pickup_code_wsh, checkout_confirmed_at_wsh,
--       overtime_hours_wsh, overtime_amount_wsh, overtime_payment_id_wsh
--   [B] Add purpose_wsh to payment_wsh (EXPLICIT table name; file_record_wsh has a
--       DIFFERENT purpose_wsh column which must NOT be touched)
--   [C] Remove DEFAULT 'day' from unit_wsh / billing_unit_wsh and DEFAULT
--       'date_range' from booking_mode_wsh; fix their COMMENTs and quantity_wsh COMMENT.
--       No default remains -> business code must set the unit explicitly.
--   [D] Migrate ONLY service IDs 7,8,20,21,25,26,35 : boarding -> night
--       (each UPDATE carries an explicit id AND the expected old value)
--   [E] NO UPDATE on pet_order_wsh : all 59 historical orders keep every value
--       (billing_unit_wsh, quantities, prices, discounts, amounts, status, times)
--   [F] NO UPDATE on unconfirmed services 1,2,3,9,29,30,36
--   [G] NO UPDATE on payment_wsh : DEFAULT 'order' is the CORRECT classification for
--       all 46 historical payments (verified: every amount_wsh == order.final_amount_wsh,
--       no duplicate payments per order, no small overtime amounts)
--   [H] NO CHECK constraint : existing illegal units (boarding/day/time) must NOT be
--       tampered with to satisfy a constraint. "Illegal units must not be sellable" is
--       enforced by BookingUnit.validateUnitSupported() in application code.
--   [I] NO DROP DATABASE / TRUNCATE / volume rebuild / DROP TABLE / DROP of columns.
--
-- VERSION GUARD / RE-RUN PROTECTION:
--   SEGMENT 0 is the version guard. If `order_cols_present` > 0 or
--   `purpose_present` > 0, this migration was already applied -> STOP, do not re-run.
--   Every DDL below is idempotent (information_schema check + PREPARE/EXECUTE).
--
-- !! MySQL DDL IMPLICITLY COMMITS. THIS SCRIPT IS NOT ATOMIC. !!
--   Execute SEGMENT BY SEGMENT and verify the CHECK query after each segment.
--   See V20261110_001__execution-log.md for the actual segment-by-segment evidence.
--   The DML in SEGMENT 4 is wrapped in an explicit transaction; DDL is not.
--
-- ROLLBACK: see V20261110_001__rollback.sql (DDL rollback is NOT safely automatic).
-- ============================================================================


-- ============================================================================
-- SEGMENT 0 : PRE-CHECK (read only) - verify EVERY row before any change
-- ============================================================================
SELECT '=== SEGMENT 0 PRE-CHECK ===' AS section;
SELECT DATABASE() AS db, VERSION() AS mysql_version, NOW() AS executed_at;

SELECT COUNT(*) AS base_tables
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE';                    -- expect 52

SELECT COUNT(*) AS order_cols_present FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pet_order_wsh'
  AND COLUMN_NAME IN ('pickup_code_wsh','checkout_confirmed_at_wsh',
                      'overtime_hours_wsh','overtime_amount_wsh','overtime_payment_id_wsh');  -- expect 0

SELECT COUNT(*) AS purpose_present FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payment_wsh'
  AND COLUMN_NAME = 'purpose_wsh';                                              -- expect 0

SELECT id_wsh, name_wsh, unit_wsh, duration_minutes_wsh, booking_mode_wsh, status_wsh
FROM pet_service_wsh WHERE id_wsh IN (7,8,20,21,25,26,35) ORDER BY id_wsh;     -- expect all boarding / 1440 / interval

SELECT id_wsh, name_wsh, unit_wsh, booking_mode_wsh, status_wsh
FROM pet_service_wsh WHERE id_wsh IN (1,2,3,9,29,30,36) ORDER BY id_wsh;       -- reference only, must stay unchanged

SELECT COUNT(*) AS orders, SUM(total_amount_wsh) AS sum_total,
       SUM(discount_wsh) AS sum_discount, SUM(IFNULL(coupon_discount_wsh,0)) AS sum_coupon,
       SUM(final_amount_wsh) AS sum_final, SUM(IFNULL(settlement_amount_wsh,0)) AS sum_settle
FROM pet_order_wsh;                                                             -- expect 59 / 22120.00 / 1140.00 / 190.00 / 20790.00 / 240.00

SELECT billing_unit_wsh, COUNT(*) AS cnt FROM pet_order_wsh GROUP BY billing_unit_wsh;  -- expect day 35 / session 21 / boarding 3

SELECT COUNT(*) AS payment_rows FROM payment_wsh;                              -- expect 46
SELECT COUNT(*) AS mismatched_payments
FROM payment_wsh p JOIN pet_order_wsh o ON o.id_wsh = p.order_id_wsh
WHERE p.amount_wsh <> o.final_amount_wsh;                                      -- expect 0 (no historical overtime payment)

SELECT unit_wsh, COUNT(*) AS cnt FROM pet_service_wsh GROUP BY unit_wsh;       -- expect session 22 / boarding 11 / day 2 / time 1


-- ============================================================================
-- SEGMENT 1 : pet_order_wsh - add 5 missing columns (idempotent)
--   Types confirmed against Entity/DTO/Service (see execution log):
--     PetOrder:139 String          -> VARCHAR(4)   NULL
--     PetOrder:143 LocalDateTime   -> DATETIME     NULL   (NULL = "not confirmed", CAS uses isNull)
--     PetOrder:147 Integer         -> INT          NOT NULL DEFAULT 0
--     PetOrder:151 BigDecimal      -> DECIMAL(10,2) NOT NULL DEFAULT 0.00
--     PetOrder:155 Long            -> BIGINT       NULL  (no full-table unique index)
--   Historical rows receive NULL / 0 from column defaults. This is DDL, not an
--   UPDATE of historical order data; plan.md:192 requires these columns to exist
--   for non-night orders too, with overtime = 0.
-- ============================================================================
SELECT '=== SEGMENT 1 : add pet_order_wsh columns ===' AS section;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE pet_order_wsh ADD COLUMN pickup_code_wsh VARCHAR(4) NULL COMMENT ''Independent pickup handover code, distinct from delivery code''',
  'SELECT ''[SKIP] pickup_code_wsh already exists'' AS msg')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pet_order_wsh' AND COLUMN_NAME='pickup_code_wsh');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE pet_order_wsh ADD COLUMN checkout_confirmed_at_wsh DATETIME NULL COMMENT ''Server time of first valid pickup confirmation, frozen afterwards''',
  'SELECT ''[SKIP] checkout_confirmed_at_wsh already exists'' AS msg')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pet_order_wsh' AND COLUMN_NAME='checkout_confirmed_at_wsh');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE pet_order_wsh ADD COLUMN overtime_hours_wsh INT NOT NULL DEFAULT 0 COMMENT ''Actual late pickup hours H, not booked nights''',
  'SELECT ''[SKIP] overtime_hours_wsh already exists'' AS msg')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pet_order_wsh' AND COLUMN_NAME='overtime_hours_wsh');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE pet_order_wsh ADD COLUMN overtime_amount_wsh DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT ''Frozen late pickup surcharge''',
  'SELECT ''[SKIP] overtime_amount_wsh already exists'' AS msg')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pet_order_wsh' AND COLUMN_NAME='overtime_amount_wsh');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE pet_order_wsh ADD COLUMN overtime_payment_id_wsh BIGINT NULL COMMENT ''Unique overtime payment id; NULL when surcharge is zero''',
  'SELECT ''[SKIP] overtime_payment_id_wsh already exists'' AS msg')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pet_order_wsh' AND COLUMN_NAME='overtime_payment_id_wsh');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- SEGMENT 1 CHECK
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, IFNULL(COLUMN_DEFAULT,'<NO DEFAULT>') AS def, COLUMN_COMMENT
FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pet_order_wsh'
  AND COLUMN_NAME IN ('pickup_code_wsh','checkout_confirmed_at_wsh','overtime_hours_wsh','overtime_amount_wsh','overtime_payment_id_wsh')
ORDER BY ORDINAL_POSITION;                                                     -- expect exactly 5 rows
SELECT COUNT(*) AS order_row_count FROM pet_order_wsh;                          -- expect 59 (unchanged)


-- ============================================================================
-- SEGMENT 2 : payment_wsh - add purpose_wsh (EXPLICIT table name)
--   Confirmed: Payment:41 String, PaymentDTO:28, PaymentPurpose.ORDER='order' (7 chars),
--   NIGHT_OVERTIME='night_overtime' (14 chars) -> VARCHAR(32) is sufficient.
--   Both creation entry points set purpose explicitly:
--     PaymentServiceImpl:248      setPurpose_wsh(PaymentPurpose.ORDER)
--     OrderServiceImpl:1778       setPurpose_wsh(PaymentPurpose.NIGHT_OVERTIME)
--   All 46 historical payments are ordinary prepayments (verified in SEGMENT 0),
--   so DEFAULT 'order' classifies them correctly. NO UPDATE on payment_wsh.
--   NOTE: file_record_wsh.purpose_wsh is a DIFFERENT column (VARCHAR(50)) - untouched.
-- ============================================================================
SELECT '=== SEGMENT 2 : add payment_wsh.purpose_wsh ===' AS section;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE payment_wsh ADD COLUMN purpose_wsh VARCHAR(32) NOT NULL DEFAULT ''order'' COMMENT ''order = original prepayment; night_overtime = late pickup surcharge''',
  'SELECT ''[SKIP] payment_wsh.purpose_wsh already exists'' AS msg')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payment_wsh' AND COLUMN_NAME='purpose_wsh');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- SEGMENT 2 CHECK
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payment_wsh' AND COLUMN_NAME='purpose_wsh';
SELECT IFNULL(purpose_wsh,'<NULL>') AS purpose_wsh, COUNT(*) AS cnt FROM payment_wsh GROUP BY purpose_wsh;  -- expect order 46
SELECT COUNT(*) AS payment_rows FROM payment_wsh;                              -- expect 46 (unchanged)
SELECT COLUMN_TYPE, COLUMN_COMMENT FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='file_record_wsh' AND COLUMN_NAME='purpose_wsh';  -- must stay varchar(50)


-- ============================================================================
-- SEGMENT 3 : remove DEFAULT 'day' / 'date_range' and fix COMMENTs
--   MODIFY COLUMN without DEFAULT leaves the column with NO default; business code
--   (BookingUnit.validateUnitSupported / ServiceItemServiceImpl.applyUnitContract)
--   must set the unit explicitly. Historical stored values are NOT changed.
--   NO CHECK constraint is added (see header [H]).
-- ============================================================================
SELECT '=== SEGMENT 3 : defaults and comments ===' AS section;

ALTER TABLE pet_service_wsh
  MODIFY COLUMN unit_wsh VARCHAR(20) NULL
  COMMENT 'Billing unit night/session/hour; set explicitly by application code';

ALTER TABLE pet_order_wsh
  MODIFY COLUMN billing_unit_wsh VARCHAR(20) NULL
  COMMENT 'Billing unit frozen at order creation; historical orders may keep confirmed legacy snapshot values';

ALTER TABLE pet_service_wsh
  MODIFY COLUMN booking_mode_wsh VARCHAR(20) NULL
  COMMENT 'interval(night) / slot(session, hour); derived from BookingUnit.bookingMode()';

ALTER TABLE pet_order_wsh
  MODIFY COLUMN quantity_wsh INT NULL DEFAULT 1
  COMMENT 'Billing quantity: night = nights / session = 1 / hour = slot count';

-- SEGMENT 3 CHECK
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, IFNULL(COLUMN_DEFAULT,'<NO DEFAULT>') AS def, COLUMN_COMMENT
FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
  AND ((TABLE_NAME='pet_service_wsh' AND COLUMN_NAME IN ('unit_wsh','booking_mode_wsh'))
    OR (TABLE_NAME='pet_order_wsh'  AND COLUMN_NAME IN ('billing_unit_wsh','quantity_wsh')))
ORDER BY TABLE_NAME, ORDINAL_POSITION;                                        -- both unit columns must show <NO DEFAULT>
SELECT COUNT(*) AS unit_check_constraints FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('pet_service_wsh','pet_order_wsh') AND CONSTRAINT_TYPE='CHECK';  -- expect 0
SELECT COUNT(*) AS order_rows, SUM(total_amount_wsh) AS sum_total, SUM(final_amount_wsh) AS sum_final FROM pet_order_wsh;  -- amounts unchanged


-- ============================================================================
-- SEGMENT 4 : WHITELIST service unit migration (EXPLICIT IDs ONLY)
--   Approved mapping: 7,8,20,21,25,26,35 : boarding -> night
--   Each statement is guarded by BOTH the exact id AND the expected old value.
--   duration_minutes_wsh (1440) and booking_mode_wsh (interval) already match
--   night semantics, so they are NOT modified.
--   Unconfirmed IDs 1,2,3,9,29,30,36 are NOT touched.
--   DML only -> wrapped in an explicit transaction (unlike the DDL segments).
-- ============================================================================
SELECT '=== SEGMENT 4 : whitelist service migration ===' AS section;
START TRANSACTION;

UPDATE pet_service_wsh SET unit_wsh = 'night' WHERE id_wsh = 7  AND unit_wsh = 'boarding';
UPDATE pet_service_wsh SET unit_wsh = 'night' WHERE id_wsh = 8  AND unit_wsh = 'boarding';
UPDATE pet_service_wsh SET unit_wsh = 'night' WHERE id_wsh = 20 AND unit_wsh = 'boarding';
UPDATE pet_service_wsh SET unit_wsh = 'night' WHERE id_wsh = 21 AND unit_wsh = 'boarding';
UPDATE pet_service_wsh SET unit_wsh = 'night' WHERE id_wsh = 25 AND unit_wsh = 'boarding';
UPDATE pet_service_wsh SET unit_wsh = 'night' WHERE id_wsh = 26 AND unit_wsh = 'boarding';
UPDATE pet_service_wsh SET unit_wsh = 'night' WHERE id_wsh = 35 AND unit_wsh = 'boarding';

SELECT COUNT(*) AS whitelist_not_migrated FROM pet_service_wsh
WHERE id_wsh IN (7,8,20,21,25,26,35) AND unit_wsh <> 'night';                 -- expect 0
SELECT COUNT(*) AS unexpected_night_services FROM pet_service_wsh
WHERE unit_wsh = 'night' AND id_wsh NOT IN (7,8,20,21,25,26,35);              -- expect 0
COMMIT;

-- SEGMENT 4 CHECK (post-commit)
SELECT id_wsh, name_wsh, unit_wsh, duration_minutes_wsh, booking_mode_wsh, status_wsh
FROM pet_service_wsh WHERE id_wsh IN (7,8,20,21,25,26,35) ORDER BY id_wsh;    -- expect night / 1440 / interval
SELECT id_wsh, name_wsh, unit_wsh, booking_mode_wsh, status_wsh
FROM pet_service_wsh WHERE id_wsh IN (1,2,3,9,29,30,36) ORDER BY id_wsh;      -- must be byte-identical to SEGMENT 0
SELECT unit_wsh, COUNT(*) AS cnt FROM pet_service_wsh GROUP BY unit_wsh;       -- expect night 7 / session 22 / boarding 4 / day 2 / time 1


-- ============================================================================
-- SEGMENT 5 : POST-CHECK (read only)
--   Full before/after row comparison is performed outside SQL by diffing
--   before_*.tsv against after_*.tsv (see execution log / final report).
-- ============================================================================
SELECT '=== SEGMENT 5 POST-CHECK ===' AS section;
SELECT COUNT(*) AS pet_order_columns FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pet_order_wsh';                 -- expect 68
SELECT COUNT(*) AS pet_service_columns FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pet_service_wsh';               -- expect 15
SELECT COUNT(*) AS payment_columns FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payment_wsh';                   -- expect 12

SELECT IFNULL(unit_wsh,'<NULL>') AS unit_wsh, COUNT(*) AS cnt FROM pet_service_wsh GROUP BY unit_wsh;
SELECT IFNULL(billing_unit_wsh,'<NULL>') AS billing_unit_wsh, COUNT(*) AS cnt FROM pet_order_wsh GROUP BY billing_unit_wsh;
SELECT COUNT(*) AS orders, SUM(total_amount_wsh) AS sum_total, SUM(discount_wsh) AS sum_discount,
       SUM(IFNULL(coupon_discount_wsh,0)) AS sum_coupon, SUM(final_amount_wsh) AS sum_final,
       SUM(IFNULL(settlement_amount_wsh,0)) AS sum_settle FROM pet_order_wsh;  -- identical to SEGMENT 0

-- Historical orders: new columns must be NULL / 0, never invented values.
SELECT COUNT(*) AS hist_orders_with_pickup_code FROM pet_order_wsh WHERE pickup_code_wsh IS NOT NULL;            -- expect 0
SELECT COUNT(*) AS hist_orders_with_checkout_ts FROM pet_order_wsh WHERE checkout_confirmed_at_wsh IS NOT NULL;  -- expect 0
SELECT COUNT(*) AS hist_orders_with_overtime FROM pet_order_wsh WHERE overtime_hours_wsh <> 0 OR overtime_amount_wsh <> 0;  -- expect 0
SELECT COUNT(*) AS hist_orders_with_overtime_pay FROM pet_order_wsh WHERE overtime_payment_id_wsh IS NOT NULL;  -- expect 0

-- Special historical orders must be byte-identical to the baseline.
SELECT id_wsh, billing_unit_wsh, quantity_wsh, unit_price_wsh, duration_minutes_wsh,
       total_amount_wsh, discount_wsh, final_amount_wsh, status_wsh
FROM pet_order_wsh WHERE id_wsh IN (60,61,62,16,42,56) ORDER BY id_wsh;

-- ============================================================================
-- FAILURE HANDLING
--   * If any SEGMENT CHECK fails: stop. DDL segments already committed cannot be
--     rolled back by ROLLBACK; use V20261110_001__rollback.sql after manual review.
--   * SEGMENT 4 DML is transactional: on failure issue ROLLBACK before retrying.
--   * Never "fix" unexpected rows by editing historical data; report them instead.
-- ============================================================================
