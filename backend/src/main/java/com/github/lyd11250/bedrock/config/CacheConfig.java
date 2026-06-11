package com.github.lyd11250.bedrock.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.Map;

/**
 * 缓存能力（Spring Cache + Redis）。
 *
 * <p>为基座与上层业务模块提供声明式缓存：方法上加 {@code @Cacheable}/{@code @CacheEvict} 即可，
 * 值经 Jackson 3 序列化为 JSON 存入 Redis（与 Sa-token 会话同库，但 key 前缀 {@code bedrock:cache:} 隔离）。
 *
 * <p>命名缓存约定：缓存名即 cacheName，最终 Redis key 形如 {@code bedrock:cache:{cacheName}:{key}}。
 * 默认 30 分钟过期；鉴权相关缓存（{@link #CACHE_RBAC_PERM}/{@link #CACHE_RBAC_ROLE}）TTL 缩短为 10 分钟，
 * 作为「漏失效」时的兜底（正常由写操作显式清除，见 {@code PermissionCacheService}）。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Redis 缓存 key 统一前缀，与 Sa-token 会话 key 区分。 */
    public static final String KEY_PREFIX = "bedrock:cache:";

    /** 用户权限码集合缓存（key = {@code tenantId:userId}）。 */
    public static final String CACHE_RBAC_PERM = "rbacPerm";
    /** 用户角色码集合缓存（key = {@code tenantId:userId}）。 */
    public static final String CACHE_RBAC_ROLE = "rbacRole";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder().build();

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> KEY_PREFIX + cacheName + ":")
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(serializer));

        RedisCacheConfiguration rbac = defaults.entryTtl(Duration.ofMinutes(10));
        Map<String, RedisCacheConfiguration> perCache = Map.of(
                CACHE_RBAC_PERM, rbac,
                CACHE_RBAC_ROLE, rbac
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
