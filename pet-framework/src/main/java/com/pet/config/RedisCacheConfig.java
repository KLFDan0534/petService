package com.pet.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


/**
 * @author pet
 * @date 2026/9/7
 */

/**
* Redis缓存配置
 * 业务作用: 为Spring Cache提供Redis缓存管理器,Json序列化,业务前缀区分
* */
@EnableCaching
@Configuration
public class RedisCacheConfig {

    /**
     * 【构建 Redis 缓存管理器】
     *
     * 业务作用：为 Spring Cache 提供统一的缓存管理器，按业务名区分 TTL。
     *
     * 调用场景：Spring 容器启动时创建 CacheManager Bean。
     *
     * 调用链：Spring 装配 → redisCacheManager → 默认配置 → 各业务 TTL → transactionAware → build。
     *
     * 数据处理：默认 TTL 30 分钟；category 2 小时、notice 10 分钟、notification 与 notificationUnreadCount 各 5 分钟。
     *
     * 业务规则：key 用字符串序列化，value 用 JSON 序列化；不缓存 null 值。
     *
     * 状态影响：无，返回配置好的管理器实例。
     *
     * 异常情况：RedisConnectionFactory 不可用时启动失败。
     *
     * 注意事项：开启事务感知后，写缓存与清除缓存推迟到事务提交，避免并发读回写旧值。
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
        //默认配置: key用字符串,value用JSON (高可读,跨服务通用)
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) //默认过期时间30分钟
                .disableCachingNullValues()       //不缓存null,防止缓存穿透
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer()));

        //按照缓存满足差异化设置TTL: key会变成分类名::业务key
        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put("category", defaultCacheConfig.entryTtl(Duration.ofHours(2)));
        perCache.put("notice", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));
        // 用户通知列表:变更频繁且要求实时(已读状态、新通知),TTL 放短,
        // 并依赖 NotificationServiceImpl 上的 @CacheEvict 主动失效
        perCache.put("notification", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        // 未读数量:与通知列表同源,独立缓存名避免与公告(notice)混用
        perCache.put("notificationUnreadCount", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(perCache)
                // 事务感知:将 put/evict 推迟到事务提交之后再执行。
                // 否则「先清缓存、后提交事务」的窗口期内,并发读会把旧数据重新写回缓存,
                // 导致刚发的通知/刚标记的已读在 TTL 到期前一直不可见。
                .transactionAware()
                .build();
    }

    /**
     * 【构建 JSON 序列化器】
     *
     * 业务作用：生成带类型信息的 JSON 序列化器，保证缓存对象可正确反序列化。
     *
     * 调用场景：redisCacheManager 配置默认缓存时调用。
     *
     * 调用链：redisCacheManager → jsonSerializer → ObjectMapper 配置。
     *
     * 数据处理：注册 JavaTimeModule 支持时间类型，开启 NON_FINAL 默认类型信息，关闭未知字段报错。
     *
     * 业务规则：必须带类型信息，否则反序列化会退化为 LinkedHashMap。
     *
     * 状态影响：无。
     *
     * 异常情况：无。
     *
     * 注意事项：实体存在只读别名 getter，缓存 JSON 会多出字段，故关闭 FAIL_ON_UNKNOWN_PROPERTIES 以免命中即 500。
     */
    private GenericJackson2JsonRedisSerializer jsonSerializer(){
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        // 实体上存在只读别名 getter（如 Notice.getId()），缓存 JSON 会同时包含
        // id_wsh 与 id 两套字段；反序列化时只读属性不可写，默认会抛
        // Unrecognized field 导致缓存命中即 500，这里关闭该严格校验。
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new GenericJackson2JsonRedisSerializer(om);
    }

}
