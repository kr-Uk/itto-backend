package com.itto.backend.domain.relationship.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.global.entity.BaseTimeEntity;
import com.itto.backend.global.enums.RelationshipStatus;
import com.itto.backend.global.enums.RelationshipTrigger;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "relationship_logs",
    indexes = {
        @Index(name = "idx_relationship_logs_relationship", columnList = "relationship_id"),
        @Index(name = "idx_relationship_logs_created", columnList = "created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RelationshipLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relationship_id", nullable = false)
    private Relationship relationship;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    private RelationshipStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private RelationshipStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", nullable = false, length = 50)
    private RelationshipTrigger triggeredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_user_id")
    private User triggeredUser;
}
