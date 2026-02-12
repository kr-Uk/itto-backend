package com.itto.backend.domain.system.repository;

import com.itto.backend.domain.system.entity.AppVersion;
import com.itto.backend.global.enums.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {

    /**
     * Find app version configuration by platform
     */
    Optional<AppVersion> findByPlatform(Platform platform);
}
