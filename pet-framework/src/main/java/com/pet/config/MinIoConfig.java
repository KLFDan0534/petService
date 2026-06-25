package com.pet.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for MinIO object storage client.
 * Reads endpoint, access key, and secret key from application properties
 * and provides a MinioClient bean.
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Configuration
public class MinIoConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    /**
     * Creates a MinioClient bean configured with the endpoint and credentials.
     * @return the configured MinioClient instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
