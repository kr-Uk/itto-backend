package com.itto.backend.application.encounter.port;

import java.util.Set;

public interface LocationDataPort {

    Set<String> getActiveH3CellKeys();

    Set<String> getUserIdsInCell(String h3Key);

    double[] getUserLocation(Long userId);

    boolean isOnCooldown(Long userIdA, Long userIdB);

    void setCooldown(Long userIdA, Long userIdB);
}
