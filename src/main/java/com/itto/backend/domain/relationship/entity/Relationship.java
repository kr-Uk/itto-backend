package com.itto.backend.domain.relationship.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.global.entity.BaseEntity;
import com.itto.backend.global.enums.RelationshipStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "relationships",
    indexes = {
        @Index(name = "idx_relationships_user1", columnList = "user_id_1"),
        @Index(name = "idx_relationships_user2", columnList = "user_id_2"),
        @Index(name = "idx_relationships_status", columnList = "status"),
        @Index(name = "idx_relationships_expires", columnList = "expires_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_relationships_users", columnNames = {"user_id_1", "user_id_2"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Relationship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relationship_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_1", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_2", nullable = false)
    private User user2;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RelationshipStatus status;

    @Column(name = "encounter_count", nullable = false)
    @Builder.Default
    private Integer encounterCount = 0;

    @Column(name = "first_encounter_at")
    private LocalDateTime firstEncounterAt;

    @Column(name = "last_encounter_at")
    private LocalDateTime lastEncounterAt;

    @Column(name = "status_changed_at", nullable = false)
    private LocalDateTime statusChangedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "chat_room_id", length = 100)
    private String chatRoomId;

    public void updateStatus(RelationshipStatus newStatus) {
        this.status = newStatus;
        this.statusChangedAt = LocalDateTime.now();
    }

    public void updateStatusWithExpiry(RelationshipStatus newStatus, LocalDateTime expiresAt) {
        this.status = newStatus;
        this.statusChangedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public boolean isParticipant(Long userId) {
        return user1.getUserId().equals(userId) || user2.getUserId().equals(userId);
    }

    public User getOtherUser(Long myUserId) {
        return user1.getUserId().equals(myUserId) ? user2 : user1;
    }
}
