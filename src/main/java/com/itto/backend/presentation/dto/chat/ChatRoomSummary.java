package com.itto.backend.presentation.dto.chat;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomSummary {
    private Long chatRoomId;
    private Long relationshipId;
    private Long otherUserId;
    private String otherUserNickname;
    private String otherUserPhotoUrl;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
    private LocalDateTime expiresAt;
}
