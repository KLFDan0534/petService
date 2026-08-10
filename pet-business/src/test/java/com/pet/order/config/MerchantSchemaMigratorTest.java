package com.pet.order.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.DefaultApplicationArguments;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MerchantSchemaMigratorTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:merchant_migrator;DB_CLOSE_DELAY=-1;MODE=MYSQL;NON_KEYWORDS=USER";

    @Test
    void migrationAddsFutureBookingColumnIdempotently() throws Exception {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            org.h2.tools.RunScript.execute(connection,
                    Files.newBufferedReader(legacySchema(), StandardCharsets.UTF_8));

            JdbcTemplate jdbcTemplate = new JdbcTemplate(
                    new org.springframework.jdbc.datasource.SimpleDriverDataSource(
                            new org.h2.Driver(), JDBC_URL, "sa", ""));
            MerchantSchemaMigrator migrator = new MerchantSchemaMigrator(jdbcTemplate);

            migrator.run(new DefaultApplicationArguments());
            migrator.run(new DefaultApplicationArguments());

            assertEquals(1, countColumns(connection, "merchant_wsh", "future_booking_enabled_wsh"));

            String columnType = jdbcTemplate.queryForObject("""
                    SELECT DATA_TYPE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE UPPER(TABLE_NAME) = UPPER('merchant_wsh')
                      AND UPPER(COLUMN_NAME) = UPPER('future_booking_enabled_wsh')
                    """, String.class);
            assertEquals("TINYINT", columnType.toUpperCase());
        }
    }

    private Path legacySchema() throws Exception {
        Path legacy = Files.createTempFile("merchant_legacy", ".sql");
        Files.writeString(legacy, """
                CREATE TABLE IF NOT EXISTS merchant_wsh (
                    `id_wsh` BIGINT AUTO_INCREMENT PRIMARY KEY,
                    `user_id_wsh` BIGINT NOT NULL,
                    `name_wsh` VARCHAR(100) NOT NULL,
                    `deleted_wsh` TINYINT DEFAULT 0
                );
                """);
        return legacy;
    }

    private int countColumns(Connection connection, String table, String column) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """)) {
            statement.setString(1, table.toUpperCase());
            statement.setString(2, column.toUpperCase());
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}