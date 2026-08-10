package com.pet.order;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * U4 RED: database row-level lock (SELECT ... FOR UPDATE) must serialize the
 * capacity check + order insert so that two concurrent transactions competing
 * for the last capacity slot cannot oversell.
 *
 * <p>This replicates, at the SQL level, the exact guard OrderServiceImpl uses:
 * lock the keeper row, count overlapping booking orders, then insert inside the
 * same transaction. The test proves that with FOR UPDATE exactly one of two
 * racing transactions succeeds, which is what makes the same-JVM
 * {@code synchronized} block insufficient-proof and the DB the consistency
 * authority across instances.
 */
class KeeperCapacityRowLockConcurrencyTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:keeper_capacity;DB_CLOSE_DELAY=-1;MODE=MYSQL;NON_KEYWORDS=USER";

    private static final LocalDate START = LocalDate.now().plusDays(1);
    private static final LocalDate END = START.plusDays(2);

    @BeforeAll
    static void setUp() throws Exception {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            createTables(connection);
            insertKeeper(connection, 1L, 1);
        }
    }

    @AfterAll
    static void tearDown() throws Exception {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP ALL OBJECTS");
            }
        }
    }

    private static void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS keeper_wsh (
                        id_wsh BIGINT PRIMARY KEY,
                        max_pets_wsh INT NOT NULL
                    )""");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS pet_order_wsh (
                        id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                        order_no_wsh VARCHAR(50) NOT NULL,
                        keeper_id_wsh BIGINT NOT NULL,
                        pet_id_wsh BIGINT NOT NULL,
                        start_date_wsh DATE NOT NULL,
                        end_date_wsh DATE NOT NULL,
                        status_wsh VARCHAR(20) NOT NULL,
                        final_amount_wsh DECIMAL(10, 2) NOT NULL
                    )""");
        }
    }

    private static void insertKeeper(Connection connection, long id, int maxPets) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO keeper_wsh (id_wsh, max_pets_wsh) VALUES (?, ?)")) {
            statement.setLong(1, id);
            statement.setInt(2, maxPets);
            statement.executeUpdate();
        }
    }

    /**
     * Two concurrent transactions both attempt to book the single remaining slot
     * (max_pets=1). With FOR UPDATE, the second waits until the first commits,
     * then counts the committed order and must reject. Exactly one succeeds.
     */
    @Test
    void lastCapacitySlotIsNotOversoldUnderForUpdate() throws Exception {
        resetOrders();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            AtomicInteger success = new AtomicInteger();
            AtomicInteger rejected = new AtomicInteger();

            Future<Integer> t1 = pool.submit(bookLastSlotTask("CONC-001", ready, go, success, rejected));
            Future<Integer> t2 = pool.submit(bookLastSlotTask("CONC-002", ready, go, success, rejected));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "both threads must be ready");
            go.countDown();

            assertEquals(1, t1.get());
            assertEquals(1, t2.get());
            assertEquals(1, success.get(), "exactly one transaction must win the last slot");
            assertEquals(1, rejected.get(), "exactly one transaction must be rejected");
            assertEquals(1, countBookingOrders(), "only one committed booking may exist");
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Control experiment: the same race WITHOUT FOR UPDATE lets both transactions
     * pass the count check and oversell (2 rows committed for capacity 1).
     * This proves the test would catch overselling and that the single-JVM
     * synchronized block alone is not a cross-instance consistency guarantee.
     */
    @Test
    void withoutForUpdateRaceOversells() throws Exception {
        resetOrders();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);

            Future<Integer> t1 = pool.submit(bookNoLockTask("NO-LOCK-001", ready, go));
            Future<Integer> t2 = pool.submit(bookNoLockTask("NO-LOCK-002", ready, go));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "both threads must be ready");
            go.countDown();

            t1.get();
            t2.get();
            assertEquals(2, countBookingOrders(), "without FOR UPDATE both racers oversell the slot");
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * The booking-overlap predicate must be a half-open interval [start, end):
     * an existing order ending exactly on the new start date does NOT conflict,
     * while a one-day overlap does. Verified against a real SQL engine.
     */
    @Test
    void overlapPredicateIsHalfOpenInterval() throws Exception {
        resetOrders();
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            LocalDate day = LocalDate.now().plusDays(10);
            insertOrder(connection, "HALF-001", day, day.plusDays(2));

            long adjacentStart = countOverlap(connection, 1L, day.plusDays(2), day.plusDays(4));
            assertEquals(0, adjacentStart, "order ending exactly at new start must not conflict");

            long overlapping = countOverlap(connection, 1L, day.plusDays(1), day.plusDays(3));
            assertEquals(1, overlapping, "one-day overlap must conflict");

            long contained = countOverlap(connection, 1L, day.plusDays(2), day.plusDays(4));
            assertEquals(0, contained, "same start as existing end is a boundary, not an overlap");
        }
    }

    private Callable<Integer> bookLastSlotTask(String orderNo, CountDownLatch ready, CountDownLatch go,
                                               AtomicInteger success, AtomicInteger rejected) {
        return () -> {
            try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
                connection.setAutoCommit(false);
                ready.countDown();
                go.await(10, TimeUnit.SECONDS);
                lockKeeper(connection, 1L);
                long count = countOverlap(connection, 1L, START, END);
                if (count >= 1) {
                    connection.rollback();
                    rejected.incrementAndGet();
                    return 1;
                }
                insertOrder(connection, orderNo);
                connection.commit();
                success.incrementAndGet();
                return 1;
            }
        };
    }

    private Callable<Integer> bookNoLockTask(String orderNo, CountDownLatch ready, CountDownLatch go) {
        return () -> {
            try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
                connection.setAutoCommit(false);
                ready.countDown();
                go.await(10, TimeUnit.SECONDS);
                long count = countOverlap(connection, 1L, START, END);
                if (count >= 1) {
                    connection.rollback();
                    return 1;
                }
                insertOrder(connection, orderNo);
                connection.commit();
                return 1;
            }
        };
    }

    private static void lockKeeper(Connection connection, long keeperId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id_wsh FROM keeper_wsh WHERE id_wsh = ? FOR UPDATE")) {
            statement.setLong(1, keeperId);
            try (ResultSet ignored = statement.executeQuery()) {
                // row lock held until commit/rollback
            }
        }
    }

    private static long countOverlap(Connection connection, long keeperId, LocalDate start, LocalDate end)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM pet_order_wsh
                WHERE keeper_id_wsh = ?
                  AND status_wsh IN ('pending', 'paid', 'confirmed', 'delivered', 'received', 'in_progress')
                  AND start_date_wsh < ?
                  AND end_date_wsh > ?
                """)) {
            statement.setLong(1, keeperId);
            statement.setObject(2, end);
            statement.setObject(3, start);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static void insertOrder(Connection connection, String orderNo) throws SQLException {
        insertOrder(connection, orderNo, START, END);
    }

    private static void insertOrder(Connection connection, String orderNo, LocalDate start, LocalDate end)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO pet_order_wsh
                    (order_no_wsh, keeper_id_wsh, pet_id_wsh, start_date_wsh, end_date_wsh,
                     status_wsh, final_amount_wsh)
                VALUES (?, ?, ?, ?, ?, 'pending', 100.00)
                """)) {
            statement.setString(1, orderNo);
            statement.setLong(2, 1L);
            statement.setLong(3, 9L);
            statement.setObject(4, start);
            statement.setObject(5, end);
            statement.executeUpdate();
        }
    }

    private static long countBookingOrders() throws Exception {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM pet_order_wsh")) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static void resetOrders() throws Exception {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DELETE FROM pet_order_wsh");
            }
        }
    }
}