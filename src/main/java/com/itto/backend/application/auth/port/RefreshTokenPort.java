package com.itto.backend.application.auth.port;

import java.time.Duration;

public interface RefreshTokenPort {

    void save(Long userId, String refreshToken, Duration ttl);

    String get(Long userId);

    void delete(Long userId);
}
