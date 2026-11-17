-- ============================================================================
-- V20261110_002__phase_f_pending_services.sql
-- Project : PetService - Phase F / incremental pricing unit migration
-- Version : V20261110_002
-- Date    : 2026-11-10
-- Target  : MySQL 8.0.46 / schema `pet_service` (Docker pet-mysql, host port 3308)
-- Baseline: V20261110_001 (completed, do NOT modify its files)
-- Backup  : .workbuddy-ai/tmp/pricing-migration-20261110/pet_service_before_V20261110_002.sql
--           308579 bytes, SHA256 6c7b907b3fe7084b8677e00bb1d902c9c7a03fc2cfc473968acbc3d094fd834b
--           Verified: 52 CREATE TABLE / 51 INSERT INTO / "-- Dump completed"
--           Stage-D backup pet_service_full_backup.sql (SHA256 ad10bb0e...) NOT overwritten
--
-- APPROVED SCOPE (Phase F implementation authorization):
--   * ONLY pet_service_wsh, ONLY ids 1,2,3,9,30
--   * ONLY columns: unit_wsh, booking_mode_wsh, duration_minutes_wsh
--   * price_wsh / status_wsh / category_id_wsh / type_wsh / any other column
--     MUST NOT appear in any SET clause
--   * Target:
--       1  night   / interval / 1440   (price 88.00 unchanged)
--       2  night   / interval / 1440   (price 168.00 unchanged)
--       3  session / slot    / 30     (price 39.00 unchanged, time_slot -> slot)
--       9  session / slot    / 540    (price 60.00 unchanged, interval -> slot)
--       30 session / slot    / 540    (price 70.00 unchanged, interval -> slot)
--   * Effective booking mode values come ONLY from BookingUnit.bookingMode():
--       night -> "interval"; session/hour -> "slot". No other string is invented.
--   * FORBIDDEN records (zero changes): 29 (frozen: duration unconfirmed),
--       36 (keep off-shelf status=0), and already migrated 7,8,20,21,25,26,35.
--   * FORBIDDEN tables (zero writes): pet_order_wsh, order_snapshot_wsh,
--       payment_wsh, refund_wsh, settlement / finance / transaction tables.
--
-- NO DDL IN THIS SCRIPT. The 5 DML statements are wrapped in an explicit
-- transaction. MySQL DDL implicit-commit caveat does not apply here, but the
-- script is still executed SEGMENT BY SEGMENT with verification after each
-- statement; do NOT claim automatic whole-script atomicity beyond the DML txn.
--
-- VERSION GUARD / RE-RUN PROTECTION:
--   SEGMENT 0 `guard_already_migrated` must be 0. If > 0, the migration was
--   already applied -> STOP, do not re-run. Each UPDATE also carries the
--   expected old unit + old duration + old price in WHERE (naturally idempotent:
--   a second run yields ROW_COUNT()=0).
--
-- IMPACT-ROW CHECK:
--   Every UPDATE is followed by SELECT ROW_COUNT(); expected exactly 1.
--   If any value != 1, issue ROLLBACK immediately, stop, and report. Do not
--   "fix" the row and continue. Pure SQL cannot self-abort (no stored
--   procedure is introduced), so the executor verifies each value.
--
-- ROLLBACK: see V20261110_002__rollback.sql (also summarized at the end here).
-- ============================================================================


-- ============================================================================
-- SEGMENT 0 : PRE-CHECK (read only) - verify EVERY row before any change
-- ============================================================================
SELECT '=== SEGMENT 0 PRE-CHECK ===' AS section;
SELECT DATABASE() AS db, VERSION() AS mysql_version, NOW() AS executed_at;

-- 5 target rows, must match the authorization exactly:
-- 1 boarding/1440/interval/88.00/1 ; 2 boarding/1440/interval/168.00/1
-- 3 time/30/time_slot/39.00/1 ; 9 boarding/1440/interval/60.00/1
-- 30 boarding/1440/interval/70.00/1
SELECT id_wsh, name_wsh, unit_wsh, duration_minutes_wsh,
       IFNULL(booking_mode_wsh,'<NULL>') AS booking_mode_wsh,
       price_wsh, status_wsh, deleted_wsh
FROM pet_service_wsh WHERE id_wsh IN (1,2,3,9,30) ORDER BY id_wsh;

SELECT COUNT(*) AS guard_already_migrated
FROM pet_service_wsh
WHERE id_wsh IN (1,2,3,9,30) AND unit_wsh IN ('night','session');               -- expect 0

-- Forbidden / frozen records
-- expect 29: day/1440/date_range/120.00/1 ; 36: day/480/date_range/150.00/0
SELECT id_wsh, unit_wsh, duration_minutes_wsh, IFNULL(booking_mode_wsh,'<NULL>') AS mode,
       price_wsh, status_wsh
FROM pet_service_wsh WHERE id_wsh IN (29,36) ORDER BY id_wsh;

SELECT COUNT(*) AS already_migrated_night FROM pet_service_wsh
WHERE id_wsh IN (7,8,20,21,25,26,35) AND unit_wsh = 'night';                    -- expect 7

-- Historical finance baseline (must be identical after migration)
SELECT COUNT(*) AS orders, SUM(total_amount_wsh) AS sum_total,
       SUM(final_amount_wsh) AS sum_final, SUM(discount_wsh) AS sum_discount
FROM pet_order_wsh;                                                             -- 59 / 22120.00 / 20790.00 / 1140.00
SELECT COUNT(*) AS refunds FROM refund_wsh;                                     -- 5

-- Service distribution BEFORE
SELECT unit_wsh, IFNULL(booking_mode_wsh,'<NULL>') AS mode, COUNT(*) AS cnt
FROM pet_service_wsh GROUP BY unit_wsh, booking_mode_wsh ORDER BY unit_wsh, mode;


-- ============================================================================
-- SEGMENT 1 : THE 5 PROTECTED DML (explicit transaction)
--   price_wsh appears ONLY in WHERE as a read-only consistency assertion;
--   it is NEVER in a SET clause. status_wsh is never touched.
-- ============================================================================
SELECT '=== SEGMENT 1 : 5 protected UPDATEs ===' AS section;
START TRANSACTION;

-- ID 1 基础寄养 : boarding -> night (one 24h-metadata night, 88.00 unchanged)
UPDATE pet_service_wsh
SET unit_wsh = 'night', booking_mode_wsh = 'interval', duration_minutes_wsh = 1440
WHERE id_wsh = 1
  AND unit_wsh = 'boarding' AND duration_minutes_wsh = 1440
  AND booking_mode_wsh = 'interval' AND price_wsh = 88.00;
SELECT ROW_COUNT() AS affected_id_1;                                            -- expect 1

-- ID 2 豪华寄养 : boarding -> night (168.00 unchanged)
UPDATE pet_service_wsh
SET unit_wsh = 'night', booking_mode_wsh = 'interval', duration_minutes_wsh = 1440
WHERE id_wsh = 2
  AND unit_wsh = 'boarding' AND duration_minutes_wsh = 1440
  AND booking_mode_wsh = 'interval' AND price_wsh = 168.00;
SELECT ROW_COUNT() AS affected_id_2;                                            -- expect 1

-- ID 3 上门遛狗 : time -> session (one 30-min service, 39.00 unchanged)
-- booking_mode_wsh time_slot -> slot (BookingUnit.bookingMode(session) = "slot")
UPDATE pet_service_wsh
SET unit_wsh = 'session', booking_mode_wsh = 'slot', duration_minutes_wsh = 30
WHERE id_wsh = 3
  AND unit_wsh = 'time' AND duration_minutes_wsh = 30
  AND booking_mode_wsh = 'time_slot' AND price_wsh = 39.00;
SELECT ROW_COUNT() AS affected_id_3;                                            -- expect 1

-- ID 9 日间寄养 : boarding -> session (one 540-min daycare, 60.00 unchanged)
-- booking_mode_wsh interval -> slot ; duration 1440 -> 540
UPDATE pet_service_wsh
SET unit_wsh = 'session', booking_mode_wsh = 'slot', duration_minutes_wsh = 540
WHERE id_wsh = 9
  AND unit_wsh = 'boarding' AND duration_minutes_wsh = 1440
  AND booking_mode_wsh = 'interval' AND price_wsh = 60.00;
SELECT ROW_COUNT() AS affected_id_9;                                            -- expect 1

-- ID 30 日间寄养(小型犬) : boarding -> session (one 540-min daycare, 70.00 unchanged)
UPDATE pet_service_wsh
SET unit_wsh = 'session', booking_mode_wsh = 'slot', duration_minutes_wsh = 540
WHERE id_wsh = 30
  AND unit_wsh = 'boarding' AND duration_minutes_wsh = 1440
  AND booking_mode_wsh = 'interval' AND price_wsh = 70.00;
SELECT ROW_COUNT() AS affected_id_30;                                           -- expect 1

-- In-transaction aggregate checks
SELECT COUNT(*) AS migrated_rows FROM pet_service_wsh
WHERE (id_wsh IN (1,2) AND unit_wsh = 'night')
   OR (id_wsh IN (3,9,30) AND unit_wsh = 'session');                           -- expect 5
SELECT COUNT(*) AS price_or_status_changed FROM pet_service_wsh
WHERE id_wsh IN (1,2,3,9,30)
  AND (price_wsh NOT IN (88.00,168.00,39.00,60.00,70.00) OR status_wsh <> 1); -- expect 0
SELECT COUNT(*) AS already_migrated_intact FROM pet_service_wsh
WHERE id_wsh IN (7,8,20,21,25,26,35) AND unit_wsh = 'night' AND status_wsh = 1; -- expect 7

COMMIT;


-- ============================================================================
-- SEGMENT 2 : POST-CHECK (read only)
-- ============================================================================
SELECT '=== SEGMENT 2 POST-CHECK ===' AS section;

-- 1 night/interval/1440/88.00/1 ; 2 night/interval/1440/168.00/1
-- 3 session/slot/30/39.00/1 ; 9 session/slot/540/60.00/1 ; 30 session/slot/540/70.00/1
SELECT id_wsh, unit_wsh, booking_mode_wsh, duration_minutes_wsh, price_wsh,
       status_wsh, deleted_wsh, IFNULL(CAST(category_id_wsh AS CHAR),'<NULL>') AS cat
FROM pet_service_wsh WHERE id_wsh IN (1,2,3,9,30) ORDER BY id_wsh;

-- Frozen / off-shelf / already migrated records must be byte-identical to SEGMENT 0
SELECT id_wsh, unit_wsh, booking_mode_wsh, duration_minutes_wsh, price_wsh, status_wsh
FROM pet_service_wsh WHERE id_wsh IN (29,36,7,8,20,21,25,26,35) ORDER BY id_wsh;

-- Unit + mode consistency for every on-shelf standard-unit service
SELECT COUNT(*) AS mode_mismatch FROM pet_service_wsh
WHERE unit_wsh IN ('night','session','hour')
  AND booking_mode_wsh <> IF(unit_wsh = 'night', 'interval', 'slot');          -- expect 0
SELECT COUNT(*) AS illegal_units_onshelf FROM pet_service_wsh
WHERE status_wsh = 1 AND unit_wsh NOT IN ('night','session','hour');           -- expect 1 (only id 29, frozen)

SELECT unit_wsh, IFNULL(booking_mode_wsh,'<NULL>') AS mode, COUNT(*) AS cnt
FROM pet_service_wsh GROUP BY unit_wsh, booking_mode_wsh ORDER BY unit_wsh, mode;
-- expect: night/interval 9 ; session/slot 25 ; day/date_range 2 (29,36)

-- Historical finance must be unchanged
SELECT COUNT(*) AS orders, SUM(total_amount_wsh) AS sum_total,
       SUM(final_amount_wsh) AS sum_final FROM pet_order_wsh;                  -- 59 / 22120.00 / 20790.00
SELECT COUNT(*) AS refunds FROM refund_wsh;                                    -- 5


-- ============================================================================
-- FAILURE HANDLING
--   * Any ROW_COUNT() != 1 or aggregate check mismatch -> ROLLBACK, STOP,
--     report. Do not repair rows and continue.
--   * If a single record fails its pre-condition, skip ONLY that record,
--     keep the other conforming records' progress, and record the reason.
--   * Never UPDATE pet_order_wsh / order_snapshot_wsh / payment_wsh /
--     refund_wsh or any finance table.
--   * Full rollback SQL (each guarded by the NEW values):
--       UPDATE pet_service_wsh SET unit_wsh='boarding', booking_mode_wsh='interval',
--              duration_minutes_wsh=1440 WHERE id_wsh=1 AND unit_wsh='night' ...;
--       UPDATE pet_service_wsh SET unit_wsh='boarding', booking_mode_wsh='interval',
--              duration_minutes_wsh=1440 WHERE id_wsh=2 AND unit_wsh='night' ...;
--       UPDATE pet_service_wsh SET unit_wsh='time', booking_mode_wsh='time_slot',
--              duration_minutes_wsh=30 WHERE id_wsh=3 AND unit_wsh='session' ...;
--       UPDATE pet_service_wsh SET unit_wsh='boarding', booking_mode_wsh='interval',
--              duration_minutes_wsh=1440 WHERE id_wsh=9 AND unit_wsh='session' ...;
--       UPDATE pet_service_wsh SET unit_wsh='boarding', booking_mode_wsh='interval',
--              duration_minutes_wsh=1440 WHERE id_wsh=30 AND unit_wsh='session' ...;
--     See V20261110_002__rollback.sql for the executable version.
-- ============================================================================
