package com.pet.customer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * U1 RED: 评分聚合 SQL 必须在真实 SQL 引擎上按 target 分组返回平均分和数量，
 * 多目标一次 IN 查询完成（服务评分与商家评分按 (target_type, target_id) 分别聚合）。
 */
class RatingAggregationSqlTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:rating_agg;DB_CLOSE_DELAY=-1;MODE=MYSQL;NON_KEYWORDS=USER";

    @BeforeAll
    static void setUp() throws Exception {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS rating_wsh (
                            id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                            order_id_wsh BIGINT,
                            user_id_wsh BIGINT NOT NULL,
                            target_id_wsh BIGINT NOT NULL,
                            target_type_wsh VARCHAR(20) NOT NULL,
                            score_wsh INT NOT NULL,
                            deleted_wsh TINYINT DEFAULT 0,
                            created_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP
                        )""");
            }
        }
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            String sql = "INSERT INTO rating_wsh (order_id_wsh, user_id_wsh, target_id_wsh, target_type_wsh, score_wsh, deleted_wsh) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, 7001L); statement.setLong(2, 10L); statement.setLong(3, 301L);
                statement.setString(4, "service"); statement.setInt(5, 5); statement.setInt(6, 0);
                statement.executeUpdate();
                statement.setLong(1, 7001L); statement.setLong(2, 11L); statement.setLong(3, 301L);
                statement.setString(4, "service"); statement.setInt(5, 4); statement.setInt(6, 0);
                statement.executeUpdate();
                statement.setLong(1, 7002L); statement.setLong(2, 12L); statement.setLong(3, 302L);
                statement.setString(4, "service"); statement.setInt(5, 3); statement.setInt(6, 0);
                statement.executeUpdate();
                statement.setLong(1, 7001L); statement.setLong(2, 10L); statement.setLong(3, 101L);
                statement.setString(4, "merchant"); statement.setInt(5, 4); statement.setInt(6, 0);
                statement.executeUpdate();
                statement.setLong(1, 7002L); statement.setLong(2, 12L); statement.setLong(3, 102L);
                statement.setString(4, "merchant"); statement.setInt(5, 5); statement.setInt(6, 0);
                statement.executeUpdate();
                // 软删除的评价不计入聚合
                statement.setLong(1, 7003L); statement.setLong(2, 13L); statement.setLong(3, 301L);
                statement.setString(4, "service"); statement.setInt(5, 1); statement.setInt(6, 1);
                statement.executeUpdate();
            }
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

    /**
     * 服务评分聚合：301 -> avg 4.5 count 2；302 -> avg 3.0 count 1；软删除不计。
     */
    @Test
    void serviceAggregationGroupsByTargetAndExcludesSoftDeleted() throws Exception {
        Map<Long, BigDecimal[]> stats = aggregate("service", List.of(301L, 302L, 999L));

        assertEquals(2, stats.size());
        assertEquals(0, new BigDecimal("4.5").compareTo(stats.get(301L)[0]));
        assertEquals(2L, stats.get(301L)[1].longValue());
        assertEquals(0, new BigDecimal("3.0").compareTo(stats.get(302L)[0]));
        assertEquals(1L, stats.get(302L)[1].longValue());
    }

    /**
     * 商家评分与服务的 target type 隔离：101 -> avg 4.0 count 1。
     */
    @Test
    void merchantAggregationIsIsolatedFromServiceType() throws Exception {
        Map<Long, BigDecimal[]> stats = aggregate("merchant", List.of(101L, 102L));

        assertEquals(0, new BigDecimal("4.0").compareTo(stats.get(101L)[0]));
        assertEquals(1L, stats.get(101L)[1].longValue());
        assertEquals(0, new BigDecimal("5.0").compareTo(stats.get(102L)[0]));
    }

    private Map<Long, BigDecimal[]> aggregate(String targetType, List<Long> targetIds) throws Exception {
        Map<Long, BigDecimal[]> result = new HashMap<>();
        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
            StringBuilder in = new StringBuilder();
            for (int i = 0; i < targetIds.size(); i++) {
                if (i > 0) in.append(",");
                in.append("?");
            }
            String sql = "SELECT target_id_wsh AS targetId, AVG(score_wsh) AS avgScore, COUNT(*) AS cnt " +
                    "FROM rating_wsh WHERE target_type_wsh = ? AND deleted_wsh = 0 " +
                    "AND target_id_wsh IN (" + in + ") GROUP BY target_id_wsh";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, targetType);
                for (int i = 0; i < targetIds.size(); i++) {
                    statement.setLong(2 + i, targetIds.get(i));
                }
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        result.put(rs.getLong("targetId"), new BigDecimal[]{
                                rs.getBigDecimal("avgScore"), BigDecimal.valueOf(rs.getLong("cnt"))});
                    }
                }
            }
        }
        return result;
    }

    private static List<Object> toList(ResultSet rs) throws SQLException {
        List<Object> row = new ArrayList<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            row.add(rs.getObject(i));
        }
        return row;
    }
}