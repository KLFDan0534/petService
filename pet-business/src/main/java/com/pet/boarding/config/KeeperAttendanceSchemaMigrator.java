package com.pet.boarding.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KeeperAttendanceSchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public KeeperAttendanceSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        createAttendanceTable();
        createLeaveTable();
    }

    private void createAttendanceTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `keeper_attendance_wsh` (
                    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
                    `keeper_id_wsh` BIGINT NOT NULL,
                    `merchant_id_wsh` BIGINT NOT NULL,
                    `check_in_at_wsh` DATETIME NOT NULL,
                    `check_in_latitude_wsh` DECIMAL(10,7) NOT NULL,
                    `check_in_longitude_wsh` DECIMAL(10,7) NOT NULL,
                    `check_in_address_wsh` VARCHAR(500),
                    `check_in_accuracy_wsh` DECIMAL(10,2),
                    `check_in_distance_wsh` DECIMAL(10,2) NOT NULL,
                    `check_out_at_wsh` DATETIME,
                    `check_out_latitude_wsh` DECIMAL(10,7),
                    `check_out_longitude_wsh` DECIMAL(10,7),
                    `check_out_address_wsh` VARCHAR(500),
                    `check_out_accuracy_wsh` DECIMAL(10,2),
                    `check_out_distance_wsh` DECIMAL(10,2),
                    `radius_meters_wsh` INT NOT NULL,
                    `deleted_wsh` TINYINT DEFAULT 0,
                    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
                    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX `idx_keeper_day` (`keeper_id_wsh`, `check_in_at_wsh`),
                    INDEX `idx_merchant_day` (`merchant_id_wsh`, `check_in_at_wsh`),
                    INDEX `idx_open_shift` (`keeper_id_wsh`, `check_out_at_wsh`)
                )
                """);
        log.info("Ensured keeper_attendance_wsh table exists");
    }

    private void createLeaveTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `keeper_leave_wsh` (
                    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
                    `keeper_id_wsh` BIGINT NOT NULL,
                    `merchant_id_wsh` BIGINT NOT NULL,
                    `start_date_wsh` DATE NOT NULL,
                    `end_date_wsh` DATE NOT NULL,
                    `reason_wsh` VARCHAR(500),
                    `created_by_wsh` BIGINT,
                    `deleted_wsh` TINYINT DEFAULT 0,
                    `created_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP,
                    `updated_at_wsh` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX `idx_keeper_range` (`keeper_id_wsh`, `start_date_wsh`, `end_date_wsh`),
                    INDEX `idx_merchant_range` (`merchant_id_wsh`, `start_date_wsh`, `end_date_wsh`)
                )
                """);
        log.info("Ensured keeper_leave_wsh table exists");
    }
}
