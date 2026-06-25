package com.pet.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * Configuration class for MyBatis-Plus.
 * Registers a pagination interceptor and a meta-object handler for
 * automatic population of created_at_wsh and updated_at_wsh fields.
 * @author: wsh
 * @date: 2026/06/24 11:05
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * Registers the MyBatis-Plus pagination interceptor.
     * @return the configured MybatisPlusInterceptor instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    /**
     * Registers a meta-object handler that auto-fills created_at_wsh
     * and updated_at_wsh on insert, and updated_at_wsh on update.
     * @return the MetaObjectHandler instance
     * @author: wsh
     * @date: 2026/06/24 11:05
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "created_at_wsh", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updated_at_wsh", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updated_at_wsh", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
