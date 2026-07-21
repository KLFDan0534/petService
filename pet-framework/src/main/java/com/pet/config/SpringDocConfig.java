package com.pet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("宠物托管服务 API")
                        .version("1.0.0"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("公共接口")
                .pathsToMatch("/**")
                .packagesToScan("com.pet.system.controller")
                .build();
    }

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

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("后台管理接口")
                .pathsToMatch("/**")
                .packagesToScan("com.pet.admin")
                .build();
    }

    @Bean
    public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
                .group("AI接口")
                .pathsToMatch("/**")
                .packagesToScan("com.pet.ai")
                .build();
    }
}
