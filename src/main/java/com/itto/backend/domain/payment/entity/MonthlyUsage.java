package com.itto.backend.domain.payment.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "monthly_usage",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_year_month", columnNames = {"user_id", "year_month"})
    },
    indexes = {
        @Index(name = "idx_monthly_usage_user", columnList = "user_id, year_month")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MonthlyUsage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_usage_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "free_converts_used", nullable = false)
    @Builder.Default
    private Integer freeConvertsUsed = 0;

    @Column(name = "free_requests_used", nullable = false)
    @Builder.Default
    private Integer freeRequestsUsed = 0;

    @Column(name = "free_restores_used", nullable = false)
    @Builder.Default
    private Integer freeRestoresUsed = 0;
}
