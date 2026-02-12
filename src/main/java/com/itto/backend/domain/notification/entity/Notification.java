package com.itto.backend.domain.notification.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.global.entity.BaseTimeEntity;
import com.itto.backend.global.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_user", columnList = "user_id, is_read"),
        @Index(name = "idx_notifications_created", columnList = "created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "message", nullable = false, length = 300)
    private String message;

    @Column(name = "deep_link", length = 200)
    private String deepLink;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * Mark this notification as read
     */
    public void markAsRead() {
        this.isRead = true;
    }
}
