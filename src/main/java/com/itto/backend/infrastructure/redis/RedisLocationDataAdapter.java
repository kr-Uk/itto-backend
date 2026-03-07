package com.itto.backend.infrastructure.redis;

import com.itto.backend.application.encounter.port.LocationDataPort;
import com.itto.backend.domain.common.constants.BusinessConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisLocationDataAdapter implements LocationDataPort {

    private final StringRedisTemplate redisTemplate;

    @Override
    public Set<String> getActiveH3CellKeys() {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(BusinessConstants.LOCATION_H3_PREFIX + "*")
                .count(100)
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    @Override
    public Set<String> getUserIdsInCell(String h3Key) {
        return redisTemplate.opsForSet().members(h3Key);
    }

    @Override
    public double[] getUserLocation(Long userId) {
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
    public boolean isOnCooldown(Long userIdA, Long userIdB) {
        String cooldownKey = getCooldownKey(userIdA, userIdB);
        return Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey));
    }

    @Override
    public void setCooldown(Long userIdA, Long userIdB) {
        String cooldownKey = getCooldownKey(userIdA, userIdB);
        redisTemplate.opsForValue().set(cooldownKey, "1",
                Duration.ofMinutes(BusinessConstants.ENCOUNTER_COOLDOWN_MINUTES));
    }

    private String getCooldownKey(Long userIdA, Long userIdB) {
        long min = Math.min(userIdA, userIdB);
        long max = Math.max(userIdA, userIdB);
        return BusinessConstants.ENCOUNTER_COOLDOWN_PREFIX + min + ":" + max;
    }
}
