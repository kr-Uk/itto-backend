package com.itto.backend.presentation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter")
class JwtAuthFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternalTest {

        @Test
        @DisplayName("유효한 토큰 → 인증 정보 설정")
        void validTokenSetsAuthentication() throws ServletException, IOException {
            // given
            String token = "valid-jwt-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtProvider.validateToken(token)).thenReturn(true);
            when(jwtProvider.getUserIdFromToken(token)).thenReturn(1L);
            when(jwtProvider.getRoleFromToken(token)).thenReturn("USER");

            // when
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // then
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isEqualTo(1L);
            assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("ADMIN 역할 토큰 → ROLE_ADMIN 권한 설정")
        void adminRoleToken() throws ServletException, IOException {
            // given
            String token = "admin-jwt-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtProvider.validateToken(token)).thenReturn(true);
            when(jwtProvider.getUserIdFromToken(token)).thenReturn(99L);
            when(jwtProvider.getRoleFromToken(token)).thenReturn("ADMIN");

            // when
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // then
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("만료된 토큰 → 인증 정보 없이 통과")
        void expiredTokenNoAuthentication() throws ServletException, IOException {
            // given
            String token = "expired-jwt-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtProvider.validateToken(token)).thenReturn(false);

            // when
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // then
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Authorization 헤더 없음 → 인증 정보 없이 통과")
        void noAuthorizationHeader() throws ServletException, IOException {
            // given
            when(request.getHeader("Authorization")).thenReturn(null);

            // when
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // then
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNull();
            verify(filterChain).doFilter(request, response);
            verify(jwtProvider, never()).validateToken(anyString());
        }

        @Test
        @DisplayName("Bearer 접두어 없는 헤더 → 인증 정보 없이 통과")
        void noBearerPrefix() throws ServletException, IOException {
            // given
            when(request.getHeader("Authorization")).thenReturn("Basic some-credentials");

            // when
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // then
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNull();
            verify(filterChain).doFilter(request, response);
            verify(jwtProvider, never()).validateToken(anyString());
        }

        @Test
        @DisplayName("역할이 null인 토큰 → ROLE_USER로 기본 설정")
        void nullRoleDefaultsToUser() throws ServletException, IOException {
            // given
            String token = "no-role-token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtProvider.validateToken(token)).thenReturn(true);
            when(jwtProvider.getUserIdFromToken(token)).thenReturn(5L);
            when(jwtProvider.getRoleFromToken(token)).thenReturn(null);

            // when
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // then
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("filterChain.doFilter는 항상 호출됨")
        void filterChainAlwaysCalled() throws ServletException, IOException {
            // given
            when(request.getHeader("Authorization")).thenReturn(null);

            // when
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain, times(1)).doFilter(request, response);
        }
    }
}
