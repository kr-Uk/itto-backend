package com.itto.backend.presentation.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * STOMP CONNECT 프레임에서 JWT를 검증하고 사용자 식별 정보를 세션에 부착한다.
 *
 * 클라이언트는 STOMP CONNECT 시 헤더에 "Authorization: Bearer <token>"를 넣어 보낸다.
 * 인증된 사용자만 broker 구독/전송이 허용되며, 이후 모든 STOMP 프레임은 동일한 Principal로 처리된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("STOMP CONNECT 인증 실패: Authorization 헤더 누락");
            throw new IllegalArgumentException("WebSocket 인증 실패");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtProvider.validateToken(token)) {
            log.warn("STOMP CONNECT 인증 실패: 유효하지 않은 토큰");
            throw new IllegalArgumentException("WebSocket 인증 실패");
        }

        Long userId = jwtProvider.getUserIdFromToken(token);
        String role = jwtProvider.getRoleFromToken(token);
        String authority = "ROLE_" + (role != null ? role : "USER");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                );
        accessor.setUser(authentication);
        return message;
    }
}
