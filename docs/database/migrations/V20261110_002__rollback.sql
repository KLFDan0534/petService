-- ============================================================================
-- V20261110_002__rollback.sql
-- Rollback for V20261110_002__phase_f_pending_services.sql
-- Version : V20261110_002 (rollback)
-- Date    : 2026-11-10
-- Target  : MySQL 8.0.46 / schema `pet_service` (Docker pet-mysql:3308)
--
-- Restores ONLY the 5 authorized service rows to their pre-migration values.
-- Does NOT touch pet_order_wsh / order_snapshot_wsh / payment_wsh / refund_wsh.
-- Does NOT restore from a dump; full restore uses
--   .workbuddy-ai/tmp/pricing-migration-20261110/pet_service_before_V20261110_002.sql
--   (SHA256 6c7b907b3fe7084b8677e00bb1d902c9c7a03fc2cfc473968acbc3d094fd834b)
--   and is a separate, manually approved operation.
--
-- NO DDL. Execute segment by segment, verify each ROW_COUNT() = 1.
-- Re-run guard: SEGMENT 0 checks that all 5 rows still hold the NEW values.
-- ============================================================================

SELECT '=== ROLLBACK SEGMENT 0 GUARD ===' AS section;
SELECT id_wsh, unit_wsh, booking_mode_wsh, duration_minutes_wsh, price_wsh, status_wsh
FROM pet_service_wsh WHERE id_wsh IN (1,2,3,9,30) ORDER BY id_wsh;
-- expect 1/2 night/interval/1440 ; 3 session/slot/30 ; 9/30 session/slot/540
SELECT COUNT(*) AS rows_in_new_state FROM pet_service_wsh
WHERE (id_wsh IN (1,2) AND unit_wsh='night' AND booking_mode_wsh='interval' AND duration_minutes_wsh=1440)
   OR (id_wsh = 3 AND unit_wsh='session' AND booking_mode_wsh='slot' AND duration_minutes_wsh=30)
   OR (id_wsh IN (9,30) AND unit_wsh='session' AND booking_mode_wsh='slot' AND duration_minutes_wsh=540);
-- expect 5; if < 5 STOP and review manually

START TRANSACTION;

-- ID 1 -> original boarding / interval / 1440 (price 88.00 untouched)
UPDATE pet_service_wsh
SET unit_wsh = 'boarding', booking_mode_wsh = 'interval', duration_minutes_wsh = 1440
WHERE id_wsh = 1 AND unit_wsh = 'night' AND booking_mode_wsh = 'interval'
  AND duration_minutes_wsh = 1440 AND price_wsh = 88.00;
SELECT ROW_COUNT() AS rollback_id_1;   -- expect 1

-- ID 2 -> original boarding / interval / 1440 (price 168.00 untouched)
UPDATE pet_service_wsh
SET unit_wsh = 'boarding', booking_mode_wsh = 'interval', duration_minutes_wsh = 1440
WHERE id_wsh = 2 AND unit_wsh = 'night' AND booking_mode_wsh = 'interval'
  AND duration_minutes_wsh = 1440 AND price_wsh = 168.00;
SELECT ROW_COUNT() AS rollback_id_2;   -- expect 1

-- ID 3 -> original time / time_slot / 30 (price 39.00 untouched)
UPDATE pet_service_wsh
SET unit_wsh = 'time', booking_mode_wsh = 'time_slot', duration_minutes_wsh = 30
WHERE id_wsh = 3 AND unit_wsh = 'session' AND booking_mode_wsh = 'slot'
  AND duration_minutes_wsh = 30 AND price_wsh = 39.00;
SELECT ROW_COUNT() AS rollback_id_3;   -- expect 1

-- ID 9 -> original boarding / interval / 1440 (price 60.00 untouched)
UPDATE pet_service_wsh
SET unit_wsh = 'boarding', booking_mode_wsh = 'interval', duration_minutes_wsh = 1440
WHERE id_wsh = 9 AND unit_wsh = 'session' AND booking_mode_wsh = 'slot'
  AND duration_minutes_wsh = 540 AND price_wsh = 60.00;
SELECT ROW_COUNT() AS rollback_id_9;   -- expect 1

-- ID 30 -> original boarding / interval / 1440 (price 70.00 untouched)
UPDATE pet_service_wsh
SET unit_wsh = 'boarding', booking_mode_wsh = 'interval', duration_minutes_wsh = 1440
WHERE id_wsh = 30 AND unit_wsh = 'session' AND booking_mode_wsh = 'slot'
  AND duration_minutes_wsh = 540 AND price_wsh = 70.00;
SELECT ROW_COUNT() AS rollback_id_30;  -- expect 1

COMMIT;

SELECT '=== ROLLBACK POST-CHECK ===' AS section;
SELECT id_wsh, unit_wsh, booking_mode_wsh, duration_minutes_wsh, price_wsh, status_wsh
FROM pet_service_wsh WHERE id_wsh IN (1,2,3,9,30) ORDER BY id_wsh;
SELECT COUNT(*) AS orders, SUM(final_amount_wsh) AS sum_final FROM pet_order_wsh;  -- 59 / 20790.00
SELECT COUNT(*) AS refunds FROM refund_wsh;                                        -- 5
