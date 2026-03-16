package com.itto.backend.application.auth;

import com.itto.backend.domain.common.constants.BusinessConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String PREFIX = BusinessConstants.LOGIN_ATTEMPT_PREFIX;
    private static final int MAX_ATTEMPTS = BusinessConstants.LOGIN_MAX_ATTEMPTS;
    private static final int LOCKOUT_MINUTES = BusinessConstants.LOGIN_LOCKOUT_MINUTES;

    private final StringRedisTemplate redisTemplate;

    public boolean isBlocked(String ip) {
        String count = redisTemplate.opsForValue().get(PREFIX + ip);
        return count != null && Integer.parseInt(count) >= MAX_ATTEMPTS;
    }

    public void recordFailure(String ip) {
        String key = PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(LOCKOUT_MINUTES));
        }
    }

    public void resetAttempts(String ip) {
        redisTemplate.delete(PREFIX + ip);
    }
}
