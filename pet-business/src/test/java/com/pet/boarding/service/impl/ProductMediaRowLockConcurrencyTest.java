package com.pet.boarding.service.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * U1: 产品图册并发替换 —— SELECT ... FOR UPDATE 锁住聚合根(pet_service_wsh)后
 * 整体删除+重插媒体行。两个并发替换必须串行化，最终恰好是一个完整版本，
 * 绝不出现混合图册（旧行残留或双封面）。
 *
 * <p>与 OrderServiceImpl 使用同一锁模式，在 SQL 层复刻 ServiceMediaServiceImpl
 * 的替换序列，证明数据库行锁是跨实例一致的最终权威。
 */
class ProductMediaRowLockConcurrencyTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:product_media_lock;DB_CLOSE_DELAY=-1;MODE=MYSQL;NON_KEYWORDS=USER";

    @BeforeAll
    static void setUp() throws Exception {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            createTables(connection);
            insertService(connection, 1L);
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
                    CREATE TABLE IF NOT EXISTS pet_service_wsh (
                        id_wsh BIGINT PRIMARY KEY,
                        merchant_id_wsh BIGINT NOT NULL
                    )""");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS pet_service_media_wsh (
                        id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                        service_id_wsh BIGINT NOT NULL,
                        file_id_wsh BIGINT NOT NULL,
                        sort_order_wsh INT NOT NULL,
                        is_cover_wsh TINYINT NOT NULL DEFAULT 0,
                        UNIQUE (service_id_wsh, file_id_wsh),
                        UNIQUE (service_id_wsh, sort_order_wsh)
                    )""");
        }
    }

    private static void insertService(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO pet_service_wsh (id_wsh, merchant_id_wsh) VALUES (?, ?)")) {
            statement.setLong(1, id);
            statement.setLong(2, 100L);
            statement.executeUpdate();
        }
    }

    /**
     * 复刻 ServiceMediaServiceImpl.replaceMedia 的 SQL 序列：
     * 锁父行 → 校验 → 删除旧行 → 插入新行，在单事务内执行。
     */
    private static void replace(Connection connection, long serviceId, List<long[]> rows) throws SQLException {
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement lock = connection.prepareStatement(
                    "SELECT id_wsh FROM pet_service_wsh WHERE id_wsh = ? FOR UPDATE")) {
                lock.setLong(1, serviceId);
                try (ResultSet rs = lock.executeQuery()) {
                    assertTrue(rs.next(), "service must exist");
                }
            }
            try (PreparedStatement del = connection.prepareStatement(
                    "DELETE FROM pet_service_media_wsh WHERE service_id_wsh = ?")) {
                del.setLong(1, serviceId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = connection.prepareStatement(
                    "INSERT INTO pet_service_media_wsh (service_id_wsh, file_id_wsh, sort_order_wsh, is_cover_wsh) "
                            + "VALUES (?, ?, ?, ?)")) {
                for (long[] row : rows) {
                    ins.setLong(1, serviceId);
                    ins.setLong(2, row[0]);
                    ins.setLong(3, row[1]);
                    ins.setLong(4, row[2]);
                    ins.executeUpdate();
                }
            }
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static List<long[]> readMedia(Connection connection) throws SQLException {
        List<long[]> rows = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT file_id_wsh, sort_order_wsh, is_cover_wsh "
                        + "FROM pet_service_media_wsh WHERE service_id_wsh = 1 ORDER BY sort_order_wsh")) {
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    rows.add(new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3)});
                }
            }
        }
        return rows;
    }

    private static boolean isCompleteSet(List<long[]> rows, List<long[]> expected) {
        if (rows.size() != expected.size()) {
            return false;
        }
        for (int i = 0; i < rows.size(); i++) {
            long[] r = rows.get(i);
            long[] e = expected.get(i);
            if (r[0] != e[0] || r[1] != e[1] || r[2] != e[2]) {
                return false;
            }
        }
        return true;
    }

    @Test
    void concurrentReplacementsYieldExactlyOneCompleteSet() throws Exception {
        List<long[]> setA = List.of(
                new long[]{11L, 0, 1},
                new long[]{12L, 1, 0},
                new long[]{13L, 2, 0});
        List<long[]> setB = List.of(
                new long[]{21L, 0, 1},
                new long[]{22L, 1, 0},
                new long[]{23L, 2, 0},
                new long[]{24L, 3, 0});

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            Future<Integer> t1 = pool.submit(() -> replaceTask(setA, ready, go));
            Future<Integer> t2 = pool.submit(() -> replaceTask(setB, ready, go));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "both threads must be ready");
            go.countDown();

            assertEquals(0, t1.get());
            assertEquals(0, t2.get());

            try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
                List<long[]> rows = readMedia(connection);
                boolean completeA = isCompleteSet(rows, setA);
                boolean completeB = isCompleteSet(rows, setB);
                assertTrue(completeA || completeB, "final gallery must be exactly one complete set, got " + rows.size());
                int covers = 0;
                for (long[] row : rows) {
                    covers += (int) row[2];
                }
                assertEquals(1, covers, "exactly one cover must survive");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private Integer replaceTask(List<long[]> rows, CountDownLatch ready, CountDownLatch go) {
        try {
            try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
                ready.countDown();
                assertTrue(go.await(10, TimeUnit.SECONDS));
                replace(connection, 1L, rows);
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}