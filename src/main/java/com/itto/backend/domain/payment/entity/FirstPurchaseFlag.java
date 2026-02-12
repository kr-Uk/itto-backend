package com.itto.backend.domain.payment.entity;

import com.itto.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "first_purchase_flags",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_package", columnNames = {"user_id", "package_name"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FirstPurchaseFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "package_name", nullable = false, length = 50)
    private String packageName;

    @CreatedDate
    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;
}
