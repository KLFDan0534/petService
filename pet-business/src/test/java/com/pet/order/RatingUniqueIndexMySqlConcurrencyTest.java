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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * U4 MySQL 真机并发验证：uk_rating_order_user_type 唯一索引必须让两个并发插入
 * 相同 (order_id, user_id, target_type) 评价的事务恰好一个成功、一个唯一键冲突。
 *
 * <p>使用独立临时库 rating_uk_concurrency_test，验证后 DROP，不污染生产数据。
 * 跳过条件：本机无 MySQL（如 CI 环境）时自动跳过。
 */
class RatingUniqueIndexMySqlConcurrencyTest {

    private static final String MYSQL_HOST =
            System.getenv().getOrDefault("MYSQL_CONC_HOST", "127.0.0.1");
    private static final String MYSQL_PORT =
            System.getenv().getOrDefault("MYSQL_CONC_PORT", "3308");
    private static final String MYSQL_USER =
            System.getenv().getOrDefault("MYSQL_CONC_USER", "root");
    private static final String MYSQL_PASSWORD =
            System.getenv().getOrDefault("MYSQL_CONC_PASSWORD", "1234");
    private static final String DB = "rating_uk_concurrency_test";

    private static String jdbcUrl(String db) {
        return "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/" + db
                + "?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
    }

    private static boolean mysqlAvailable = false;

    @BeforeAll
    static void setUp() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(
                jdbcUrl(""), MYSQL_USER, MYSQL_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS " + DB);
            stmt.execute("CREATE DATABASE " + DB + " CHARACTER SET utf8mb4");
            mysqlAvailable = true;
        } catch (SQLException e) {
            mysqlAvailable = false;
        }
        if (mysqlAvailable) {
            try (Connection conn = DriverManager.getConnection(jdbcUrl(DB), MYSQL_USER, MYSQL_PASSWORD);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                        CREATE TABLE rating_wsh (
                            id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                            order_id_wsh BIGINT,
                            user_id_wsh BIGINT NOT NULL,
                            target_type_wsh VARCHAR(20) NOT NULL,
                            UNIQUE KEY uk_rating_order_user_type
                                (order_id_wsh, user_id_wsh, target_type_wsh)
                        )""");
            }
        }
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (!mysqlAvailable) return;
        try (Connection conn = DriverManager.getConnection(jdbcUrl(""), MYSQL_USER, MYSQL_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS " + DB);
        }
    }

    @Test
    void concurrentDuplicateRatingInsertSucceedsExactlyOnce() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(mysqlAvailable,
                "MySQL 不可用（本机未监听或凭据错误），跳过真机并发验证");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            AtomicInteger success = new AtomicInteger();
            AtomicInteger conflict = new AtomicInteger();

            Future<Integer> t1 = pool.submit(insertRatingTask("TXN-001", ready, go, success, conflict));
            Future<Integer> t2 = pool.submit(insertRatingTask("TXN-002", ready, go, success, conflict));
            assertTrue(ready.await(15, TimeUnit.SECONDS), "both threads must be ready");
            go.countDown();

            t1.get();
            t2.get();
            assertEquals(1, success.get(), "exactly one insert must win the unique key");
            assertEquals(1, conflict.get(), "exactly one insert must hit the unique key conflict");
            assertEquals(1, countRows(), "only one rating row may exist");
        } finally {
            pool.shutdownNow();
        }
    }

    private static java.util.concurrent.Callable<Integer> insertRatingTask(
            String orderNo, CountDownLatch ready, CountDownLatch go,
            AtomicInteger success, AtomicInteger conflict) {
        return () -> {
            try (Connection conn = DriverManager.getConnection(jdbcUrl(DB), MYSQL_USER, MYSQL_PASSWORD)) {
                conn.setAutoCommit(false);
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO rating_wsh (order_id_wsh, user_id_wsh, target_type_wsh) VALUES (?, ?, ?)")) {
                    insert.setLong(1, 9001L);
                    insert.setLong(2, 9002L);
                    insert.setString(3, "keeper");
                    ready.countDown();
                    go.await(15, TimeUnit.SECONDS);
                    try {
                        insert.executeUpdate();
                        conn.commit();
                        success.incrementAndGet();
                        return 1;
                    } catch (SQLException e) {
                        if (e.getErrorCode() == 1062) { // ER_DUP_ENTRY
                            conflict.incrementAndGet();
                            return 2;
                        }
                        throw e;
                    }
                }
            }
        };
    }

    private static int countRows() throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl(DB), MYSQL_USER, MYSQL_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rating_wsh")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
