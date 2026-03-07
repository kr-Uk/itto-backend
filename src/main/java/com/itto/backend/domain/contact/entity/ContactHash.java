package com.itto.backend.domain.contact.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "contact_hashes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_phone_hash", columnNames = {"user_id", "phone_hash"})
    },
    indexes = {
        @Index(name = "idx_contact_hashes_user", columnList = "user_id"),
        @Index(name = "idx_contact_hashes_phone_hash", columnList = "phone_hash")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ContactHash extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_hash_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "phone_hash", nullable = false, length = 64)
    private String phoneHash;
}
