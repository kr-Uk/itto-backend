package com.itto.backend.infrastructure.redis;

import com.itto.backend.application.location.port.LocationCachePort;
import com.itto.backend.domain.common.constants.BusinessConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisLocationCacheAdapter implements LocationCachePort {

    private static final Duration LOCATION_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void saveLocation(Long userId, String h3Index, double latitude, double longitude) {
        String userKey = BusinessConstants.LOCATION_USER_PREFIX + userId;
        String h3Key = BusinessConstants.LOCATION_H3_PREFIX + h3Index;

        // Remove from previous H3 cell if changed
        String prevH3 = (String) redisTemplate.opsForHash().get(userKey, "h3Index");
        if (prevH3 != null && !prevH3.equals(h3Index)) {
            redisTemplate.opsForSet().remove(BusinessConstants.LOCATION_H3_PREFIX + prevH3, userId.toString());
        }

        // Save user location hash
        redisTemplate.opsForHash().put(userKey, "h3Index", h3Index);
        redisTemplate.opsForHash().put(userKey, "lat", String.valueOf(latitude));
        redisTemplate.opsForHash().put(userKey, "lng", String.valueOf(longitude));
        redisTemplate.expire(userKey, LOCATION_TTL);

        // Add to H3 cell set
        redisTemplate.opsForSet().add(h3Key, userId.toString());
        redisTemplate.expire(h3Key, LOCATION_TTL);
    }

    @Override
    public void removeLocation(Long userId) {
        String userKey = BusinessConstants.LOCATION_USER_PREFIX + userId;
        String prevH3 = (String) redisTemplate.opsForHash().get(userKey, "h3Index");
        if (prevH3 != null) {
            redisTemplate.opsForSet().remove(BusinessConstants.LOCATION_H3_PREFIX + prevH3, userId.toString());
        }
        redisTemplate.delete(userKey);
    }

    @Override
    public double[] getLocation(Long userId) {
        String userKey = BusinessConstants.LOCATION_USER_PREFIX + userId;
        Map<Object, Object> locationData = redisTemplate.opsForHash().entries(userKey);
        if (locationData.isEmpty()) return null;

        try {
            double lat = Double.parseDouble((String) locationData.get("lat"));
            double lng = Double.parseDouble((String) locationData.get("lng"));
            return new double[]{lat, lng};
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getH3Index(Long userId) {
        String userKey = BusinessConstants.LOCATION_USER_PREFIX + userId;
        return (String) redisTemplate.opsForHash().get(userKey, "h3Index");
    }
}
