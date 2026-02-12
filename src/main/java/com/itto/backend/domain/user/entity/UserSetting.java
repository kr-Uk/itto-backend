package com.itto.backend.domain.user.entity;

import com.itto.backend.global.entity.BaseEntity;
import com.itto.backend.global.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_setting_id")
    private Long userSettingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_filter", length = 10)
    private Gender genderFilter;

    @Column(name = "age_range_min", nullable = false)
    @Builder.Default
    private Integer ageRangeMin = 20;

    @Column(name = "age_range_max", nullable = false)
    @Builder.Default
    private Integer ageRangeMax = 60;

    @Column(name = "chat_notification", nullable = false)
    @Builder.Default
    private Boolean chatNotification = true;

    @Column(name = "relationship_notification", nullable = false)
    @Builder.Default
    private Boolean relationshipNotification = true;

    @Column(name = "stats_notification", nullable = false)
    @Builder.Default
    private Boolean statsNotification = true;

    @Column(name = "marketing_notification", nullable = false)
    @Builder.Default
    private Boolean marketingNotification = false;
}
