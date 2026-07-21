package com.pet.marketing.config;

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
public class CouponSchemaMigrator implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public CouponSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        createCouponTables();
        addOrderColumnIfMissing("coupon_id_wsh", "BIGINT");
        addOrderColumnIfMissing("coupon_template_id_wsh", "BIGINT");
        addOrderColumnIfMissing("coupon_discount_wsh", "DECIMAL(10,2) DEFAULT 0");
        addOrderColumnIfMissing("platform_subsidy_wsh", "DECIMAL(10,2) DEFAULT 0");
        addOrderColumnIfMissing("settlement_amount_wsh", "DECIMAL(10,2)");
        addOrderColumnIfMissing("promotion_snapshot_wsh", "TEXT");
    }

    private void createCouponTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS coupon_template_wsh (
                    id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name_wsh VARCHAR(100) NOT NULL,
                    type_wsh VARCHAR(20) NOT NULL,
                    threshold_amount_wsh DECIMAL(10,2) DEFAULT 0,
                    discount_amount_wsh DECIMAL(10,2) DEFAULT 0,
                    discount_rate_wsh DECIMAL(5,2),
                    max_discount_amount_wsh DECIMAL(10,2),
                    total_quantity_wsh INT,
                    issued_quantity_wsh INT DEFAULT 0,
                    per_user_limit_wsh INT DEFAULT 1,
                    valid_from_wsh DATETIME NOT NULL,
                    valid_to_wsh DATETIME NOT NULL,
                    status_wsh TINYINT DEFAULT 1,
                    scope_type_wsh VARCHAR(20) DEFAULT 'platform',
                    merchant_id_wsh BIGINT,
                    created_by_wsh BIGINT,
                    remark_wsh VARCHAR(500),
                    deleted_wsh TINYINT DEFAULT 0,
                    created_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_coupon_template_status (status_wsh, valid_from_wsh, valid_to_wsh),
                    INDEX idx_coupon_template_scope (scope_type_wsh, merchant_id_wsh)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_coupon_wsh (
                    id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                    template_id_wsh BIGINT NOT NULL,
                    user_id_wsh BIGINT NOT NULL,
                    status_wsh VARCHAR(20) DEFAULT 'available',
                    source_wsh VARCHAR(20) DEFAULT 'claim',
                    order_id_wsh BIGINT,
                    order_no_wsh VARCHAR(50),
                    discount_amount_wsh DECIMAL(10,2),
                    locked_at_wsh DATETIME,
                    used_at_wsh DATETIME,
                    expire_at_wsh DATETIME,
                    deleted_wsh TINYINT DEFAULT 0,
                    created_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_user_coupon_user_status (user_id_wsh, status_wsh),
                    INDEX idx_user_coupon_template_user (template_id_wsh, user_id_wsh),
                    INDEX idx_user_coupon_order (order_id_wsh)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS coupon_usage_wsh (
                    id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_coupon_id_wsh BIGINT NOT NULL,
                    template_id_wsh BIGINT NOT NULL,
                    user_id_wsh BIGINT NOT NULL,
                    order_id_wsh BIGINT NOT NULL,
                    order_no_wsh VARCHAR(50) NOT NULL,
                    discount_amount_wsh DECIMAL(10,2) NOT NULL,
                    funding_party_wsh VARCHAR(20) DEFAULT 'platform',
                    status_wsh VARCHAR(20) DEFAULT 'used',
                    deleted_wsh TINYINT DEFAULT 0,
                    created_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_coupon_usage_user_coupon (user_coupon_id_wsh),
                    INDEX idx_coupon_usage_order (order_id_wsh),
                    INDEX idx_coupon_usage_user (user_id_wsh)
                )
                """);
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
