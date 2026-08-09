package com.pet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * SpringDoc OpenAPI configuration.
 * Defines the API info and groups API endpoints into logical groups:
 * public, user-facing, admin, and AI interfaces.
 */
public class SpringDocConfig {

    /**
     * Creates the custom OpenAPI specification with title and version.
     *
     * @return the OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("宠物托管服务 API")
                        .version("1.0.0"));
    }

    /**
     * Groups public-facing API endpoints under the "公共接口" group.
     * Scans controllers in the system module.
     *
     * @return the GroupedOpenApi instance
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("公共接口")
                .pathsToMatch("/**")
                .packagesToScan("com.pet.system.controller")
                .build();
    }

    /**
     * Groups user-facing API endpoints under the "用户端接口" group.
     * Scans controllers across multiple business modules.
     *
     * @return the GroupedOpenApi instance
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户端接口")
                .pathsToMatch("/**")
                .packagesToScan(
                        "com.pet.boarding.controller",
                        "com.pet.pet.controller",
                        "com.pet.order.controller",
                        "com.pet.customer.controller",
                        "com.pet.operation.controller",
                        "com.pet.finance.controller",
                        "com.pet.fulfillment.controller",
                        "com.pet.qualification.controller",
                        "com.pet.marketing.controller",
                        "com.pet.geo.controller",
                        "com.pet.common")
                .build();
    }

    /**
     * Groups admin API endpoints under the "后台管理接口" group.
     * Scans the admin module controllers.
     *
     * @return the GroupedOpenApi instance
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("后台管理接口")
                .pathsToMatch("/**")
                .packagesToScan("com.pet.admin")
                .build();
    }

    /**
     * Groups AI-related API endpoints under the "AI接口" group.
     * Scans the AI module controllers.
     *
     * @return the GroupedOpenApi instance
     */
    @Bean
    public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
                .group("AI接口")
                .pathsToMatch("/**")
                .packagesToScan("com.pet.ai")
                .build();
    }
}
