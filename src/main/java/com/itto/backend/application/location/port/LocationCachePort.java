package com.itto.backend.application.location.port;

public interface LocationCachePort {

    void saveLocation(Long userId, String h3Index, double latitude, double longitude);

    void removeLocation(Long userId);

    double[] getLocation(Long userId);

    String getH3Index(Long userId);
}
