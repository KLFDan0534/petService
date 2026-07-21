package com.pet.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.pet")
@MapperScan({"com.pet.system.mapper", "com.pet.boarding.mapper", "com.pet.customer.mapper", "com.pet.finance.mapper", "com.pet.operation.mapper", "com.pet.order.mapper", "com.pet.pet.mapper", "com.pet.ai.mapper", "com.pet.qualification.mapper", "com.pet.marketing.mapper", "com.pet.membership.mapper"})
@EnableAsync
@EnableScheduling
public class PetApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetApplication.class, args);
    }
}
