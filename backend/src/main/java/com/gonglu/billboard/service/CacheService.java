package com.gonglu.billboard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 缓存服务：封装 Redis 读写，带降级容错。
 * Redis 未启动或连接失败时，所有操作静默降级（读=未命中，写=跳过），不影响主流程。
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    /** 下拉联动数据缓存 key */
    public static final String KEY_LOOKUP = "cache:lookup";
    /** 默认缓存时长 */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 读取缓存；Redis 不可用时返回 null（视为未命中） */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("[Redis] 读取缓存失败，降级为未命中: key={}, err={}", key, e.getMessage());
            return null;
        }
    }

    /** 写缓存（默认 5 分钟过期）；Redis 不可用时静默跳过 */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_TTL);
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("[Redis] 写缓存失败，已跳过: key={}, err={}", key, e.getMessage());
        }
    }

    /** 删除缓存；Redis 不可用时静默跳过 */
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[Redis] 清缓存失败，已跳过: key={}, err={}", key, e.getMessage());
        }
    }
}
