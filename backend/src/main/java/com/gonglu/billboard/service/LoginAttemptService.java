package com.gonglu.billboard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录防爆破：基于 Redis 记录账号连续失败次数。
 * 连续失败 5 次锁定 10 分钟；登录成功清零。
 * Redis 不可用时降级为"不锁定"，不影响登录主流程。
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final int MAX_FAIL = 5;
    private static final Duration FAIL_WINDOW = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;

    public LoginAttemptService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(String username) {
        return "login:fail:" + username;
    }

    /** 账号是否处于锁定状态 */
    public boolean isLocked(String username) {
        try {
            String v = redis.opsForValue().get(key(username));
            return v != null && Integer.parseInt(v) >= MAX_FAIL;
        } catch (Exception e) {
            log.warn("[Redis] 登录锁定检查失败，降级为不锁定: {}", e.getMessage());
            return false;
        }
    }

    /** 记录一次登录失败；返回当前连续失败次数（Redis 不可用时返回 0） */
    public int onFail(String username) {
        try {
            Long count = redis.opsForValue().increment(key(username));
            if (count != null && count == 1L) {
                redis.expire(key(username), FAIL_WINDOW);
            }
            return count == null ? 0 : count.intValue();
        } catch (Exception e) {
            log.warn("[Redis] 记录登录失败次数失败，已跳过: {}", e.getMessage());
            return 0;
        }
    }

    /** 登录成功，清零失败计数 */
    public void onSuccess(String username) {
        try {
            redis.delete(key(username));
        } catch (Exception e) {
            log.warn("[Redis] 清零登录失败计数失败，已跳过: {}", e.getMessage());
        }
    }
}
