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

    public ProductMediaMigrationReadiness(JdbcTemplate jdbcTemplate, DataSource dataSource,
                                          @org.springframework.beans.factory.annotation.Value(
                                                  "${app.migration.product-media-version:v6_service_product_media}")
                                          String requiredVersion) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.requiredVersion = requiredVersion;
    }

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

    private String databaseProductName() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read database product name for migration readiness", e);
        }
    }

    private boolean isMysql(String productName) {
        return productName != null && productName.toLowerCase().contains("mysql");
    }
}