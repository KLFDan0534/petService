package com.pet.config;

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
public class CustomerFinanceSchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public CustomerFinanceSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        createMerchantCustomerServiceTable();
        addColumnIfMissing("wallet_wsh", "version_wsh",
                "INT NOT NULL DEFAULT 0 COMMENT 'optimistic lock version' AFTER frozen_amount_wsh");
        addColumnIfMissing("ticket_wsh", "merchant_id_wsh",
                "BIGINT COMMENT 'Merchant ID' AFTER id_wsh");
        addColumnIfMissing("ticket_wsh", "order_id_wsh",
                "BIGINT COMMENT 'Order ID' AFTER merchant_id_wsh");
        addColumnIfMissing("complaint_wsh", "merchant_id_wsh",
                "BIGINT COMMENT 'Merchant ID' AFTER order_id_wsh");
        log.info("Customer and finance schema migration checked");
    }

    private void createMerchantCustomerServiceTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS merchant_customer_service_wsh (
                    id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                    merchant_id_wsh BIGINT NOT NULL COMMENT 'Merchant ID',
                    user_id_wsh BIGINT NOT NULL COMMENT 'Applicant/customer service user ID',
                    applicant_note_wsh VARCHAR(500) COMMENT 'Application note',
                    review_note_wsh VARCHAR(500) COMMENT 'Merchant review note',
                    status_wsh VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected/resigned/terminated',
                    reviewer_id_wsh BIGINT COMMENT 'Merchant reviewer user ID',
                    reviewed_at_wsh DATETIME COMMENT 'Review time',
                    deleted_wsh TINYINT DEFAULT 0,
                    created_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_mcs_merchant_user (merchant_id_wsh, user_id_wsh),
                    INDEX idx_mcs_user_status (user_id_wsh, status_wsh),
                    INDEX idx_mcs_merchant_status (merchant_id_wsh, status_wsh)
                )
                """);
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        if (!hasTable(table)) {
            log.warn("Skipped adding {}.{} because table does not exist", table, column);
            return;
        }
        if (hasColumn(table, column)) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("Added missing {}.{}", table, column);
        } catch (Exception e) {
            if (!hasColumn(table, column)) {
                throw e;
            }
        }
    }

    private boolean hasTable(String table) {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource != null) {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                String catalog = connection.getCatalog();
                String schema = connection.getSchema();
                if (hasTable(metaData, catalog, schema, table)
                        || hasTable(metaData, null, schema, table)
                        || hasTable(metaData, catalog, null, table)
                        || hasTable(metaData, null, null, table)) {
                    return true;
                }
            } catch (Exception e) {
                log.debug("Could not inspect database metadata for table {}", table, e);
            }
        }
        try {
            String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                    + "WHERE UPPER(TABLE_NAME) = UPPER(?)";
            return !jdbcTemplate.queryForList(sql, table).isEmpty();
        } catch (Exception e) {
            return false;
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

    private boolean hasTable(DatabaseMetaData metaData, String catalog, String schema, String table) {
        String[] tablePatterns = {table, table.toUpperCase(Locale.ROOT), table.toLowerCase(Locale.ROOT)};
        String[] schemaPatterns = schema == null
                ? new String[]{null}
                : new String[]{schema, schema.toUpperCase(Locale.ROOT), schema.toLowerCase(Locale.ROOT)};
        try {
            for (String schemaPattern : schemaPatterns) {
                for (String tablePattern : tablePatterns) {
                    try (ResultSet rs = metaData.getTables(catalog, schemaPattern, tablePattern, null)) {
                        if (rs.next()) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not inspect database metadata table {}", table, e);
        }
        return false;
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
