package com.itto.backend.infrastructure.redis;

import com.itto.backend.application.auth.port.RefreshTokenPort;
import com.itto.backend.domain.common.constants.BusinessConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenAdapter implements RefreshTokenPort {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(Long userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(
                BusinessConstants.REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                ttl
        );
    }

    @Override
    public String get(Long userId) {
        return redisTemplate.opsForValue().get(BusinessConstants.REFRESH_TOKEN_PREFIX + userId);
    }

    @Override
    public void delete(Long userId) {
        redisTemplate.delete(BusinessConstants.REFRESH_TOKEN_PREFIX + userId);
    }
}
