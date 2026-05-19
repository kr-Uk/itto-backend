package com.itto.backend.presentation.dto.chat;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {
    private Long messageId;
    private Long chatRoomId;
    private Long senderId;
    private String content;
    private String imageUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
