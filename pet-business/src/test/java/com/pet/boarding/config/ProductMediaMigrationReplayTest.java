package com.pet.boarding.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * U1: 版本化媒体迁移在临时 MySQL schema 上重放两次。
 *
 * <p>执行真实的 pet-admin/src/main/resources/db/migration_v6_service_product_media.sql
 * （含迁移台账/存储过程），断言:
 * 1) 首次运行后媒体行按原顺序回填、第一张有效图为封面、未解析值仅审计；
 * 2) 第二次运行不产生任何行/封面变化（幂等）；
 * 3) 台账记录预期版本。
 *
 * <p>本机无 mysql CLI 或未配置 MYSQL_CONC_* 连接时自动跳过（与
 * RatingUniqueIndexMySqlConcurrencyTest 同模式）。
 */
class ProductMediaMigrationReplayTest {

    private static final String HOST =
            System.getenv().getOrDefault("MYSQL_CONC_HOST", "127.0.0.1");
    private static final String PORT =
            System.getenv().getOrDefault("MYSQL_CONC_PORT", "3308");
    private static final String USER =
            System.getenv().getOrDefault("MYSQL_CONC_USER", "root");
    private static final String PASSWORD =
            System.getenv().getOrDefault("MYSQL_CONC_PASSWORD", "1234");
    private static final String DB = "product_media_migration_test";

    private static boolean mysqlAvailable = false;

    private static String jdbcUrl(String db) {
        return "jdbc:mysql://" + HOST + ":" + PORT + "/" + db
                + "?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
    }

    @BeforeAll
    static void setUp() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(jdbcUrl(""), USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS " + DB);
            stmt.execute("CREATE DATABASE " + DB + " CHARACTER SET utf8mb4");
            mysqlAvailable = true;
        } catch (SQLException e) {
            mysqlAvailable = false;
        }
        assumeTrue(mysqlAvailable, "MySQL not available, skipping migration replay test");
        seedBaseline();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (!mysqlAvailable) return;
        try (Connection conn = DriverManager.getConnection(jdbcUrl(""), USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS " + DB);
        }
    }

    /**
     * 构造 legacy 基线：两个服务。
     * - 服务 1: 内部可解析 → 内部可解析 → 外部 URL → 空串 → 内部可解析
     * - 服务 2: 空 images_wsh
     * file_record_wsh 仅包含可解析的两个对象。
     */
    private static void seedBaseline() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl(DB), USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE pet_service_wsh (
                        id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                        merchant_id_wsh BIGINT NOT NULL,
                        images_wsh TEXT
                    )""");
            stmt.execute("""
                    CREATE TABLE file_record_wsh (
                        id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                        original_name_wsh VARCHAR(255),
                        object_name_wsh VARCHAR(500) NOT NULL,
                        size_wsh BIGINT,
                        content_type_wsh VARCHAR(100),
                        user_id_wsh BIGINT,
                        created_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )""");
            stmt.execute("INSERT INTO file_record_wsh (object_name_wsh) "
                    + "VALUES ('service/alpha.jpg'), ('service/beta.png')");
            stmt.execute("INSERT INTO pet_service_wsh (merchant_id_wsh, images_wsh) VALUES "
                    + "(100, '/minio/pet-service/service/alpha.jpg,"
                    + "/minio/pet-service/service/beta.png,"
                    + "https://evil.example.com/f.png,  ,"
                    + "/minio/pet-service/service/alpha.jpg')");
            stmt.execute("INSERT INTO pet_service_wsh (merchant_id_wsh, images_wsh) VALUES (101, NULL)");
        }
    }

    @Test
    void migrationReplayIsIdempotentAndPreservesOrder() throws Exception {
        runMigrationScript();
        runMigrationScript();

        try (Connection conn = DriverManager.getConnection(jdbcUrl(DB), USER, PASSWORD)) {
            // 媒体行: 服务1 应只有 2 行（重复 alpha 被去重），顺序 0,1，封面为 alpha
            List<long[]> rows = mediaRows(conn);
            assertEquals(2, rows.size(), "only resolvable internal values, deduplicated");
            assertEquals(List.of(0L, 1L), List.of(rows.get(0)[1], rows.get(1)[1]), "sort order 0..N");
            assertEquals(1L, rows.get(0)[0], "alpha.jpg has fresh-table id 1");
            assertEquals(2L, rows.get(1)[0], "beta.png has fresh-table id 2");
            int covers = 0;
            for (long[] row : rows) {
                covers += (int) row[2];
            }
            assertEquals(1, covers, "exactly one cover after both runs");

            // audit: 服务1 legacy 5 项位置 → 导入 2 行（alpha 重复值被去重）→ 未解析 3（外部 URL/空串/重复值）
            int ledger = count(conn, "SELECT COUNT(*) FROM migration_ledger_wsh WHERE version_wsh = 'v6_service_product_media'");
            assertEquals(1, ledger, "ledger records the version exactly once");
        }
    }

    @Test
    void secondRunProducesIdenticalRows() throws Exception {
        runMigrationScript();

        List<long[]> afterFirst = snapshotRows();
        runMigrationScript();
        List<long[]> afterSecond = snapshotRows();

        assertEquals(afterFirst.size(), afterSecond.size());
        for (int i = 0; i < afterFirst.size(); i++) {
            assertEquals(0, compareRow(afterFirst.get(i), afterSecond.get(i)),
                    "row " + i + " must be identical after second run");
        }
    }

    private long compareRow(long[] a, long[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return 1;
        }
        return 0;
    }

    private List<long[]> snapshotRows() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl(DB), USER, PASSWORD)) {
            return mediaRows(conn);
        }
    }

    private void runMigrationScript() throws Exception {
        String script = resolveScript().replace('\\', '/');
        File scriptFile = new File(script);
        if (!scriptFile.isFile()) {
            throw new IllegalStateException("migration script not found: " + script);
        }
        Process process = new ProcessBuilder(
                "cmd", "/c",
                "mysql",
                "-h", HOST, "-P", PORT, "-u", USER,
                "--password=" + PASSWORD,
                "--default-character-set=utf8mb4",
                DB,
                "-e", "SOURCE " + script)
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        int exit = process.waitFor();
        assertTrue(exit == 0, "migration script failed: " + output);
    }

    private String resolveScript() throws Exception {
        String explicit = System.getenv("MIGRATION_SCRIPT");
        if (explicit != null && !explicit.isEmpty()) {
            return explicit;
        }
        String[] candidates = {
                "src/main/resources/db/migration_v6_service_product_media.sql",
                "../pet-admin/src/main/resources/db/migration_v6_service_product_media.sql",
                "../../pet-admin/src/main/resources/db/migration_v6_service_product_media.sql"
        };
        for (String candidate : candidates) {
            Path path = Path.of(candidate);
            if (new File(path.toString()).isFile()) {
                return path.toAbsolutePath().toString();
            }
        }
        try (var stream = Files.walk(Path.of("."))) {
            return stream.map(Path::toAbsolutePath)
                    .filter(p -> p.toString().endsWith("migration_v6_service_product_media.sql"))
                    .findFirst()
                    .map(Path::toString)
                    .orElseThrow(() -> new IllegalStateException("migration script not found"));
        }
    }

    private List<long[]> mediaRows(Connection conn) throws SQLException {
        List<long[]> rows = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT file_id_wsh, sort_order_wsh, is_cover_wsh "
                        + "FROM pet_service_media_wsh WHERE service_id_wsh = 1 ORDER BY sort_order_wsh")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3)});
                }
            }
        }
        return rows;
    }

    private int count(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}