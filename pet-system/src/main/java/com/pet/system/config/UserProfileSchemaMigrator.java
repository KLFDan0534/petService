package com.pet.system.config;

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
public class UserProfileSchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public UserProfileSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("gender_wsh", "TINYINT DEFAULT 0");
        addColumnIfMissing("real_name_wsh", "VARCHAR(50)");
        addColumnIfMissing("id_card_no_wsh", "VARCHAR(32)");
        addColumnIfMissing("real_name_status_wsh", "TINYINT DEFAULT 0");
        addColumnIfMissing("reject_reason_wsh", "VARCHAR(500)");
        addColumnIfMissing("payment_password_wsh", "VARCHAR(255)");
    }

    private void addColumnIfMissing(String column, String definition) {
        if (hasColumn("user_wsh", column)) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE user_wsh ADD COLUMN " + column + " " + definition);
            log.info("Added missing user_wsh.{}", column);
        } catch (Exception e) {
            if (!hasColumn("user_wsh", column)) {
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
