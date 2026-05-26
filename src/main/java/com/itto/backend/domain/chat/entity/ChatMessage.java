package com.itto.backend.domain.chat.entity;

import com.itto.backend.domain.common.entity.BaseTimeEntity;
import com.itto.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "chat_messages",
    indexes = {
        @Index(name = "idx_chat_messages_room_created", columnList = "chat_room_id, created_at DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    public void markAsRead() {
        this.isRead = true;
    }
}
