package com.itto.backend.domain.payment.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.global.entity.BaseEntity;
import com.itto.backend.global.enums.Store;
import com.itto.backend.global.enums.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "subscriptions",
    indexes = {
        @Index(name = "idx_subscriptions_user", columnList = "user_id"),
        @Index(name = "idx_subscriptions_active", columnList = "is_active, expires_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "store", nullable = false, length = 20)
    private Store store;

    @Column(name = "store_subscription_id", length = 200)
    private String storeSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 30)
    @Builder.Default
    private SubscriptionPlan plan = SubscriptionPlan.MONTHLY;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "is_promotion", nullable = false)
    @Builder.Default
    private Boolean isPromotion = false;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Cancel the subscription
     * Sets cancelledAt to now and deactivates the subscription
     */
    public void cancel() {
        this.cancelledAt = LocalDateTime.now();
        this.isActive = false;
    }
}
