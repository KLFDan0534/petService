package com.pet.operation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
@Slf4j
public class NoticeReadSchemaInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    public NoticeReadSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS notice_read_wsh (
                        id_wsh BIGINT AUTO_INCREMENT PRIMARY KEY,
                        notice_id_wsh BIGINT NOT NULL,
                        user_id_wsh BIGINT NOT NULL,
                        read_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP,
                        deleted_wsh TINYINT DEFAULT 0,
                        created_at_wsh DATETIME DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT uk_notice_read_notice_user UNIQUE (notice_id_wsh, user_id_wsh)
                    )
                    """);
        }
        log.info("notice_read_wsh schema checked");
    }
}
