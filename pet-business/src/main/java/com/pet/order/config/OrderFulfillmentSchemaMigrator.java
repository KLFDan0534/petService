package com.pet.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * 订单履约模式数据库迁移器，自动补充缺失的数据库字段和交接码
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Component
@Slf4j
public class OrderFulfillmentSchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private static final Random HANDOVER_CODE_RANDOM = new Random();

    public OrderFulfillmentSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 应用启动时执行数据库字段补充和交接码回填
     * @param args 应用启动参数
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("delivery_address_wsh", "VARCHAR(500)");
        addColumnIfMissing("delivery_time_wsh", "DATETIME");
        addColumnIfMissing("receiver_available_start_wsh", "DATETIME");
        addColumnIfMissing("receiver_available_end_wsh", "DATETIME");
        addColumnIfMissing("pickup_address_wsh", "VARCHAR(500)");
        addColumnIfMissing("pickup_time_wsh", "DATETIME");
        addColumnIfMissing("handover_code_wsh", "VARCHAR(4)");
        addColumnIfMissing("delivered_at_wsh", "DATETIME");
        addColumnIfMissing("received_at_wsh", "DATETIME");
        addColumnIfMissing("started_at_wsh", "DATETIME");
        addColumnIfMissing("start_photo_wsh", "VARCHAR(1000)");
        addColumnIfMissing("completed_at_wsh", "DATETIME");
        addColumnIfMissing("final_report_generated_wsh", "TINYINT DEFAULT 0");
        addColumnIfMissing("tip_wsh", "deleted_wsh", "TINYINT DEFAULT 0");
        backfillMissingHandoverCodes();
    }

    private void addColumnIfMissing(String column, String definition) {
        addColumnIfMissing("pet_order_wsh", column, definition);
    }

    private void addColumnIfMissing(String table, String column, String definition) {
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
        String[] tablePatterns = {
                table,
                table.toUpperCase(Locale.ROOT),
                table.toLowerCase(Locale.ROOT)
        };
        String[] columnPatterns = {
                column,
                column.toUpperCase(Locale.ROOT),
                column.toLowerCase(Locale.ROOT)
        };
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

    private void backfillMissingHandoverCodes() {
        if (!hasColumn("pet_order_wsh", "handover_code_wsh")) {
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id_wsh FROM pet_order_wsh WHERE handover_code_wsh IS NULL OR handover_code_wsh = ''");
        for (Map<String, Object> row : rows) {
            Object id = row.get("id_wsh");
            jdbcTemplate.update("UPDATE pet_order_wsh SET handover_code_wsh = ? WHERE id_wsh = ?",
                    generateHandoverCode(), id);
        }
        if (!rows.isEmpty()) {
            log.info("Backfilled handover codes for {} orders", rows.size());
        }
    }

    private String generateHandoverCode() {
        synchronized (HANDOVER_CODE_RANDOM) {
            return String.format("%04d", HANDOVER_CODE_RANDOM.nextInt(10000));
        }
    }
}
