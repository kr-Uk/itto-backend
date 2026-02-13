package com.itto.backend.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * [JWT 인증 필터 - 모든 요청의 "출입증 검사관"]
 *
 * 필터(Filter)란?
 * - 앱에서 보내는 모든 API 요청이 실제 처리되기 전에 거치는 "검문소" 같은 것입니다.
 * - 이 필터는 요청에 포함된 JWT 토큰을 확인하여 "이 사람이 로그인한 사용자가 맞는지" 검증합니다.
 *
 * 인증 흐름:
 * 1. 앱이 API 요청을 보낼 때 헤더에 "Authorization: Bearer eyJ..." 형태로 토큰을 포함
 * 2. 이 필터가 헤더에서 토큰을 꺼냄
 * 3. JwtProvider를 통해 토큰이 유효한지 검증
 * 4. 유효하면 → "이 요청은 userId=XX인 사용자가 보낸 것"이라고 스프링 시큐리티에 알려줌
 * 5. 유효하지 않으면 → 인증 정보 없이 다음 단계로 넘어감 (접근 거부될 수 있음)
 *
 * OncePerRequestFilter를 상속:
 * - 하나의 요청에 대해 딱 한 번만 실행되는 것을 보장하는 스프링 제공 필터 클래스
 */
@Slf4j                   // Lombok: 로그 출력 기능 자동 제공
@RequiredArgsConstructor // Lombok: final 필드(jwtProvider)를 매개변수로 받는 생성자를 자동 생성
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";  // HTTP 헤더 이름
    private static final String BEARER_PREFIX = "Bearer ";               // 토큰 앞에 붙는 접두어 ("Bearer eyJ...")

    private final JwtProvider jwtProvider;  // 토큰 검증/파싱을 담당하는 객체

    /**
     * 모든 HTTP 요청마다 자동으로 실행되는 메서드
     * - request: 앱이 보낸 요청 정보 (헤더, URL 등)
     * - response: 서버가 보낼 응답 정보
     * - filterChain: 다음 필터로 요청을 넘기는 체인
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1단계: 요청 헤더에서 "Authorization" 값을 꺼냄
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // 2단계: 헤더가 존재하고 "Bearer "로 시작하는 경우에만 토큰 검증 진행
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            // "Bearer " 접두어를 제거하여 순수 토큰 문자열만 추출
            String token = authHeader.substring(BEARER_PREFIX.length());

            // 3단계: 토큰이 유효한지 검증 (만료, 위조 여부 확인)
            if (jwtProvider.validateToken(token)) {
                // 4단계: 토큰에서 사용자 ID를 꺼냄
                Long userId = jwtProvider.getUserIdFromToken(token);

                // 5단계: 스프링 시큐리티에 "이 사용자는 인증된 사용자"라고 등록
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,                                         // 인증된 사용자의 ID
                                null,                                           // 비밀번호 (JWT 방식에서는 불필요)
                                List.of(new SimpleGrantedAuthority("ROLE_USER")) // 사용자 권한 (일반 사용자)
                        );

                // 현재 요청의 보안 컨텍스트에 인증 정보를 저장 → 이후 컨트롤러에서 사용자 ID를 꺼내 쓸 수 있음
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 다음 필터 또는 실제 API 처리로 요청을 넘김 (반드시 호출해야 요청이 진행됨)
        filterChain.doFilter(request, response);
    }
}
