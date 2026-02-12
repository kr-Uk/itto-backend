package com.itto.backend.domain.system.entity;

import com.itto.backend.global.entity.BaseEntity;
import com.itto.backend.global.enums.Platform;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_versions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AppVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private Platform platform;

    @Column(name = "min_version", nullable = false, length = 20)
    private String minVersion;

    @Column(name = "latest_version", nullable = false, length = 20)
    private String latestVersion;

    @Column(name = "force_update", nullable = false)
    @Builder.Default
    private Boolean forceUpdate = false;

    @Column(name = "update_url", length = 500)
    private String updateUrl;
}
