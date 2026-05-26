package com.itto.backend.presentation.stomp;

import com.itto.backend.application.chat.ChatService;
import com.itto.backend.presentation.dto.chat.ChatSendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

/**
 * STOMP 메시지 매핑.
 *
 * 클라이언트가 /app/chat.send 로 publish 하면 ChatService.sendMessage가 호출되고,
 * 결과는 ChatService 내부에서 /topic/chat-rooms/{roomId} 로 broadcast 된다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatSendRequest request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        chatService.sendMessage(userId, request);
    }
}
