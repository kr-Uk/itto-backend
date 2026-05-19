package com.itto.backend.presentation.dto.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * STOMP /app/chat.send 페이로드.
 * content와 imageUrl 중 적어도 하나는 채워져야 한다.
 */
@Getter
@Setter
@NoArgsConstructor
@Builder
public class ChatSendRequest {
    private Long chatRoomId;
    private String content;
    private String imageUrl;

    public ChatSendRequest(Long chatRoomId, String content, String imageUrl) {
        this.chatRoomId = chatRoomId;
        this.content = content;
        this.imageUrl = imageUrl;
    }
}
