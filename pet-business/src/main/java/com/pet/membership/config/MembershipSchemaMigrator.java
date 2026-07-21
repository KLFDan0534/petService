package com.pet.membership.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Locale;

@Component
@Slf4j
public class MembershipSchemaMigrator implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public MembershipSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        createMemberPlanTable();
        createUserMembershipTable();
        createMembershipOrderTable();
        createMembershipBenefitUsageTable();
        createMembershipEventTable();
        addOrderMembershipColumns();
        log.info("Membership schema migration checked");
    }

    private void createMemberPlanTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS member_plan_wsh (
                    id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                    code_wsh VARCHAR(50) NOT NULL,
                    name_wsh VARCHAR(100) NOT NULL,
                    level_wsh INT NOT NULL DEFAULT 1,
                    price_wsh DECIMAL(10,2) NOT NULL DEFAULT 0,
                    duration_days_wsh INT NOT NULL,
                    discount_rate_wsh DECIMAL(5,2) DEFAULT 1.00,
                    monthly_coupon_config_wsh TEXT,
                    benefit_config_wsh TEXT,
                    status_wsh TINYINT DEFAULT 1,
                    sort_order_wsh INT DEFAULT 0,
                    remark_wsh VARCHAR(500),
                    deleted_wsh TINYINT DEFAULT 0,
                    created_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_member_plan_code (code_wsh),
                    INDEX idx_member_plan_status (status_wsh, level_wsh)
                )
                """);
    }

    private void createUserMembershipTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_membership_wsh (
                    id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id_wsh BIGINT NOT NULL,
                    plan_id_wsh BIGINT,
                    plan_code_wsh VARCHAR(50),
                    level_wsh INT DEFAULT 0,
                    status_wsh VARCHAR(20) DEFAULT 'inactive',
                    started_at_wsh DATETIME,
                    expires_at_wsh DATETIME,
                    auto_renew_wsh TINYINT DEFAULT 0,
                    source_wsh VARCHAR(30) DEFAULT 'purchase',
                    last_order_id_wsh BIGINT,
                    benefit_snapshot_wsh TEXT,
                    deleted_wsh TINYINT DEFAULT 0,
                    created_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_user_membership_user (user_id_wsh),
                    INDEX idx_user_membership_status (status_wsh, expires_at_wsh),
                    INDEX idx_user_membership_plan (plan_id_wsh, status_wsh)
                )
                """);
    }

    private void createMembershipOrderTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS membership_order_wsh (
                    id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                    order_no_wsh VARCHAR(50) NOT NULL,
                    user_id_wsh BIGINT NOT NULL,
                    plan_id_wsh BIGINT NOT NULL,
                    plan_code_wsh VARCHAR(50),
                    amount_wsh DECIMAL(10,2) NOT NULL,
                    pay_method_wsh VARCHAR(20) DEFAULT 'balance',
                    status_wsh VARCHAR(20) DEFAULT 'pending',
                    paid_at_wsh DATETIME,
                    membership_start_at_wsh DATETIME,
                    membership_end_at_wsh DATETIME,
                    request_id_wsh VARCHAR(120),
                    plan_snapshot_wsh TEXT,
                    remark_wsh VARCHAR(500),
                    deleted_wsh TINYINT DEFAULT 0,
                    created_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_membership_order_no (order_no_wsh),
                    UNIQUE KEY uk_membership_order_request (request_id_wsh),
                    INDEX idx_membership_order_user_status (user_id_wsh, status_wsh),
                    INDEX idx_membership_order_plan (plan_id_wsh)
                )
                """);
    }

    private void createMembershipBenefitUsageTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS membership_benefit_usage_wsh (
                    id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id_wsh BIGINT NOT NULL,
                    membership_id_wsh BIGINT,
                    plan_id_wsh BIGINT,
                    benefit_type_wsh VARCHAR(30) NOT NULL,
                    benefit_code_wsh VARCHAR(50),
                    business_type_wsh VARCHAR(50),
                    business_id_wsh VARCHAR(100),
                    amount_wsh DECIMAL(10,2) DEFAULT 0,
                    quantity_wsh INT DEFAULT 1,
                    usage_status_wsh VARCHAR(20) DEFAULT 'used',
                    request_id_wsh VARCHAR(120),
                    usage_snapshot_wsh TEXT,
                    used_at_wsh DATETIME,
                    deleted_wsh TINYINT DEFAULT 0,
                    created_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_membership_usage_request (request_id_wsh),
                    INDEX idx_membership_usage_user_type (user_id_wsh, benefit_type_wsh),
                    INDEX idx_membership_usage_business (business_type_wsh, business_id_wsh)
                )
                """);
    }

    private void createMembershipEventTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS membership_event_wsh (
                    id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id_wsh BIGINT NOT NULL,
                    membership_id_wsh BIGINT,
                    membership_order_id_wsh BIGINT,
                    event_type_wsh VARCHAR(50) NOT NULL,
                    event_status_wsh VARCHAR(20) DEFAULT 'success',
                    operator_id_wsh BIGINT,
                    message_wsh VARCHAR(500),
                    event_snapshot_wsh TEXT,
                    created_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_membership_event_user (user_id_wsh, created_at_wsh),
                    INDEX idx_membership_event_membership (membership_id_wsh),
                    INDEX idx_membership_event_order (membership_order_id_wsh)
                )
                """);
    }

    private void addOrderMembershipColumns() {
        addOrderColumnIfMissing("membership_id_wsh", "BIGINT");
        addOrderColumnIfMissing("membership_plan_id_wsh", "BIGINT");
        addOrderColumnIfMissing("membership_discount_wsh", "DECIMAL(10,2) DEFAULT 0");
        addOrderColumnIfMissing("membership_snapshot_wsh", "TEXT");
    }

    private void addOrderColumnIfMissing(String column, String definition) {
        if (hasColumn("pet_order_wsh", column)) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE pet_order_wsh ADD COLUMN " + column + " " + definition);
            log.info("Added missing pet_order_wsh.{}", column);
        } catch (Exception e) {
            if (!hasColumn("pet_order_wsh", column)) {
                throw e;
            }
        }
    }

    private boolean hasColumn(String table, String column) {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource != null) {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                String catalog = connection.getCatalog();
                String schema = connection.getSchema();
                if (hasColumn(metaData, catalog, schema, table, column)
                        || hasColumn(metaData, null, schema, table, column)
                        || hasColumn(metaData, catalog, null, table, column)
                        || hasColumn(metaData, null, null, table, column)) {
                    return true;
                }
            } catch (Exception e) {
                log.debug("Could not inspect database metadata for {}.{}", table, column, e);
            }
        }
        try {
            String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)";
            return !jdbcTemplate.queryForList(sql, table, column).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasColumn(DatabaseMetaData metaData, String catalog, String schema, String table, String column) {
        String[] tablePatterns = {table, table.toUpperCase(Locale.ROOT), table.toLowerCase(Locale.ROOT)};
        String[] columnPatterns = {column, column.toUpperCase(Locale.ROOT), column.toLowerCase(Locale.ROOT)};
        String[] schemaPatterns = schema == null
                ? new String[]{null}
                : new String[]{schema, schema.toUpperCase(Locale.ROOT), schema.toLowerCase(Locale.ROOT)};
        try {
            for (String schemaPattern : schemaPatterns) {
                for (String tablePattern : tablePatterns) {
                    for (String columnPattern : columnPatterns) {
                        try (ResultSet rs = metaData.getColumns(catalog, schemaPattern, tablePattern, columnPattern)) {
                            if (rs.next()) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not inspect database metadata columns for {}.{}", table, column, e);
        }
        return false;
    }
}
