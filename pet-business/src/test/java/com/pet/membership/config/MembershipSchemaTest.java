package com.pet.membership.config;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MembershipSchemaTest {

    private static final String JDBC_URL = "jdbc:h2:mem:membership_schema;DB_CLOSE_DELAY=-1;MODE=MYSQL;NON_KEYWORDS=USER";
    private static final Path H2_SCHEMA = Path.of("../pet-admin/src/main/resources/h2-schema.sql");

    @Test
    void membershipTablesAreCreatedIdempotentlyWithExpectedConstraints() throws Exception {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            runSchema(connection);
            runSchema(connection);

            assertTableColumns(connection, "member_plan_wsh", List.of(
                    "id_wsh", "code_wsh", "name_wsh", "level_wsh", "price_wsh",
                    "duration_days_wsh", "discount_rate_wsh", "benefit_config_wsh"));
            assertTableColumns(connection, "user_membership_wsh", List.of(
                    "user_id_wsh", "plan_id_wsh", "plan_code_wsh", "status_wsh",
                    "started_at_wsh", "expires_at_wsh", "benefit_snapshot_wsh"));
            assertTableColumns(connection, "membership_order_wsh", List.of(
                    "order_no_wsh", "user_id_wsh", "plan_id_wsh", "amount_wsh",
                    "pay_method_wsh", "request_id_wsh", "plan_snapshot_wsh"));
            assertTableColumns(connection, "membership_benefit_usage_wsh", List.of(
                    "user_id_wsh", "membership_id_wsh", "benefit_type_wsh",
                    "business_type_wsh", "request_id_wsh", "usage_snapshot_wsh"));
            assertTableColumns(connection, "membership_event_wsh", List.of(
                    "user_id_wsh", "membership_id_wsh", "membership_order_id_wsh",
                    "event_type_wsh", "event_status_wsh", "event_snapshot_wsh"));
            assertTableColumns(connection, "pet_order_wsh", List.of(
                    "membership_id_wsh", "membership_plan_id_wsh",
                    "membership_discount_wsh", "membership_snapshot_wsh"));

            assertDuplicatePlanCodeRejected(connection);
            assertNullableOrderRequestIdDoesNotBlockMultiplePendingOrders(connection);
        }
    }

    private void runSchema(Connection connection) throws Exception {
        try (Reader reader = Files.newBufferedReader(H2_SCHEMA, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
    }

    private void assertTableColumns(Connection connection, String tableName, List<String> columns) throws SQLException {
        for (String column : columns) {
            assertEquals(1, countColumn(connection, tableName, column), tableName + "." + column + " should exist");
        }
    }

    private int countColumn(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """)) {
            statement.setString(1, tableName.toUpperCase(Locale.ROOT));
            statement.setString(2, columnName.toUpperCase(Locale.ROOT));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private void assertDuplicatePlanCodeRejected(Connection connection) throws SQLException {
        connection.createStatement().executeUpdate("""
                INSERT INTO member_plan_wsh
                    (code_wsh, name_wsh, duration_days_wsh)
                VALUES
                    ('gold', 'Gold', 30)
                """);

        assertThrows(SQLException.class, () -> connection.createStatement().executeUpdate("""
                INSERT INTO member_plan_wsh
                    (code_wsh, name_wsh, duration_days_wsh)
                VALUES
                    ('gold', 'Gold duplicate', 30)
                """));
    }

    private void assertNullableOrderRequestIdDoesNotBlockMultiplePendingOrders(Connection connection) throws SQLException {
        connection.createStatement().executeUpdate("""
                INSERT INTO membership_order_wsh
                    (order_no_wsh, user_id_wsh, plan_id_wsh, amount_wsh, request_id_wsh)
                VALUES
                    ('MO-NULL-001', 1, 1, 9.90, NULL),
                    ('MO-NULL-002', 1, 1, 9.90, NULL)
                """);
    }
}
