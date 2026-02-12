package com.itto.backend.domain.relationship.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.global.entity.BaseEntity;
import com.itto.backend.global.enums.InyeonRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "inyeon_requests",
    indexes = {
        @Index(name = "idx_inyeon_requests_relationship", columnList = "relationship_id"),
        @Index(name = "idx_inyeon_requests_receiver", columnList = "receiver_id, status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InyeonRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relationship_id", nullable = false)
    private Relationship relationship;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InyeonRequestStatus status;

    @Column(name = "knots_used", nullable = false)
    private Integer knotsUsed;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public void respond(InyeonRequestStatus newStatus) {
        this.status = newStatus;
        this.respondedAt = LocalDateTime.now();
    }
}
