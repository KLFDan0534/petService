package com.pet.boarding.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 产品媒体迁移就绪检查。
 * <p>
 * 版本化迁移脚本（migration_v6_service_product_media.sql）是显式的
 * CI/CD/运维发布步骤，应用启动不会自动执行；本检查在 MySQL 生产型环境
 * 下断言迁移台账已记录预期版本，缺失则启动失败，防止未迁移代码先上线。
 * H2（本地快速启动/测试）不强制执行，由 h2-schema.sql 直接承载新结构。
 */
@Component
@Slf4j
public class ProductMediaMigrationReadiness implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final String requiredVersion;

    /**
     * 【构造媒体迁移就绪检查】
     *
     * 业务作用：注入 JdbcTemplate、数据源，以及要求存在的迁移版本号。
     *
     * 调用场景：Spring 容器启动创建 Bean 时调用。
     *
     * 调用链：Spring 容器 → 本构造器 → 赋值 jdbcTemplate / dataSource / requiredVersion 字段。
     *
     * 数据处理：requiredVersion 取自配置项 app.migration.product-media-version，默认 v6_service_product_media。
     *
     * 业务规则：版本号必须与迁移台账中记录的版本一致。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：该类实现 ApplicationRunner，会在应用启动完成后自动执行 run。
     */
    public ProductMediaMigrationReadiness(JdbcTemplate jdbcTemplate, DataSource dataSource,
                                          @org.springframework.beans.factory.annotation.Value(
                                                  "${app.migration.product-media-version:v6_service_product_media}")
                                          String requiredVersion) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.requiredVersion = requiredVersion;
    }

    /**
     * 【启动时执行迁移就绪检查】
     *
     * 业务作用：应用启动后检查数据库是否为 MySQL，是则断言媒体表迁移已完成。
     *
     * 调用场景：Spring Boot 启动完成、ApplicationRunner 回调阶段。
     *
     * 调用链：Spring Boot 启动 → run() → databaseProductName() → isMysql() → assertMigrationReady()。
     *
     * 数据处理：读取数据库产品名，判断是否包含 mysql（忽略大小写）。
     *
     * 业务规则：非 MySQL（如 H2 本地环境）直接跳过检查，只打 info 日志。
     *
     * 状态影响：无，只读数据库元信息与迁移台账。
     *
     * 异常情况：MySQL 环境下迁移未就绪时抛 IllegalStateException，导致应用启动失败；读取数据库产品名失败同样抛 IllegalStateException。
     *
     * 注意事项：这是有意的「启动即失败」设计，防止未迁移的代码先上线。
     */
    @Override
    public void run(ApplicationArguments args) {
        String productName = databaseProductName();
        if (!isMysql(productName)) {
            log.info("商品媒体迁移就绪检查跳过: 非 MySQL 数据库 {}", productName);
            return;
        }
        assertMigrationReady(productName);
    }

    /**
     * 就绪断言：迁移台账必须记录 requiredVersion，且媒体表已存在。
     * 台账表/媒体表缺失（未迁移的旧库）一律视为未就绪，绝不会把查询异常当“通过”。
     */
    void assertMigrationReady(String productName) {
        boolean ledgerOk;
        boolean tableOk;
        try {
            Integer ledger = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM migration_ledger_wsh WHERE version_wsh = ?",
                    Integer.class, requiredVersion);
            ledgerOk = ledger != null && ledger > 0;
        } catch (org.springframework.dao.DataAccessException e) {
            ledgerOk = false;
        }
        try {
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES "
                            + "WHERE UPPER(TABLE_SCHEMA) = UPPER(DATABASE()) "
                            + "AND UPPER(TABLE_NAME) = UPPER('pet_service_media_wsh')",
                    Integer.class);
            tableOk = tableCount != null && tableCount > 0;
        } catch (org.springframework.dao.DataAccessException e) {
            tableOk = false;
        }
        if (!ledgerOk || !tableOk) {
            throw new IllegalStateException(
                    "Product media migration is missing on MySQL (" + productName + "): "
                            + "expected migration version '" + requiredVersion + "' recorded in "
                            + "migration_ledger_wsh and table pet_service_media_wsh. "
                            + "Run the v6 建表语句（见 docs/database/full-schema.sql 中的 pet_service_media_wsh / 迁移台账）并更新台账 "
                            + "as an explicit deployment step before starting the application.");
        }
        log.info("商品媒体迁移就绪检查通过: 已存在版本 {}", requiredVersion);
    }

    /**
     * 【读取数据库产品名】
     *
     * 业务作用：通过 JDBC 元数据获取当前数据库产品名称。
     *
     * 调用场景：run() 判断是否需要执行迁移检查。
     *
     * 调用链：run() → databaseProductName() → dataSource.getConnection().getMetaData()。
     *
     * 数据处理：从连接元数据读取 getDatabaseProductName()，连接使用 try-with-resources 自动关闭。
     *
     * 业务规则：无。
     *
     * 状态影响：无。
     *
     * 异常情况：获取连接或读取元数据失败时抛 IllegalStateException「Cannot read database product name for migration readiness」。
     *
     * 注意事项：每次启动只调用一次，不缓存结果。
     */
    private String databaseProductName() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read database product name for migration readiness", e);
        }
    }

    /**
     * 【判断是否MySQL】
     *
     * 业务作用：根据数据库产品名判断当前是否运行在 MySQL 上。
     *
     * 调用场景：run() 决定是否执行迁移断言。
     *
     * 调用链：run() → isMysql()。
     *
     * 数据处理：产品名转小写后判断是否包含 "mysql"。
     *
     * 业务规则：null 返回 false。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：用包含匹配而非等值匹配，可兼容 MariaDB 之外的 MySQL 变体命名。
     */
    private boolean isMysql(String productName) {
        return productName != null && productName.toLowerCase().contains("mysql");
    }
}