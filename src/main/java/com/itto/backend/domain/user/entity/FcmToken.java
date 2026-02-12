package com.itto.backend.domain.user.entity;

import com.itto.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "fcm_tokens",
    indexes = {
        @Index(name = "idx_fcm_tokens_user_id", columnList = "user_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_fcm_tokens_user_device", columnNames = {"user_id", "device_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_token_id")
    private Long fcmTokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, length = 500)
    private String token;

    @Column(name = "device_id", nullable = false, length = 200)
    private String deviceId;

    /**
     * Update FCM token
     */
    public void updateToken(String token) {
        this.token = token;
    }
}
