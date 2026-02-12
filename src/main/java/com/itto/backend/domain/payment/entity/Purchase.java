package com.itto.backend.domain.payment.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.global.entity.BaseTimeEntity;
import com.itto.backend.global.enums.Store;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "purchases",
    indexes = {
        @Index(name = "idx_purchases_user", columnList = "user_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Purchase extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "package_name", nullable = false, length = 50)
    private String packageName;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "knots_granted", nullable = false)
    private Integer knotsGranted;

    @Column(name = "is_first_purchase", nullable = false)
    @Builder.Default
    private Boolean isFirstPurchase = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "store", nullable = false, length = 20)
    private Store store;

    @Column(name = "receipt_token", nullable = false, length = 1000)
    private String receiptToken;

    @Column(name = "receipt_validated", nullable = false)
    @Builder.Default
    private Boolean receiptValidated = false;
}
