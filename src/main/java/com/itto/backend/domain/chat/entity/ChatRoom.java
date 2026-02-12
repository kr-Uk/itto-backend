package com.itto.backend.domain.chat.entity;

import com.itto.backend.domain.relationship.entity.Relationship;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "chat_rooms",
    indexes = {
        @Index(name = "idx_chat_rooms_relationship", columnList = "relationship_id"),
        @Index(name = "idx_chat_rooms_users", columnList = "user_id_1, user_id_2")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @Column(name = "firestore_room_id", nullable = false, unique = true, length = 100)
    private String firestoreRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relationship_id", nullable = false)
    private Relationship relationship;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_1", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_2", nullable = false)
    private User user2;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
