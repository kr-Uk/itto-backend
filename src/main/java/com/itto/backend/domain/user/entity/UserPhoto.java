package com.itto.backend.domain.user.entity;

import com.itto.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "user_photos",
    indexes = {
        @Index(name = "idx_user_photos_user_id", columnList = "user_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserPhoto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Long photoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_representative", nullable = false)
    @Builder.Default
    private Boolean isRepresentative = false;

    public void updateOrder(int order) {
        this.displayOrder = order;
    }

    public void updateRepresentative(boolean representative) {
        this.isRepresentative = representative;
    }
}
