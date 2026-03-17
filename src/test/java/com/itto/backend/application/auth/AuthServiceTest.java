package com.itto.backend.application.auth;

import com.itto.backend.domain.common.enums.Provider;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.FcmTokenRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.infrastructure.oauth.OAuthUserInfo;
import com.itto.backend.infrastructure.oauth.OAuthVerifier;
import com.itto.backend.infrastructure.oauth.OAuthVerifierFactory;
import com.itto.backend.presentation.dto.auth.*;
import com.itto.backend.presentation.security.JwtProperties;
import com.itto.backend.presentation.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private OAuthVerifierFactory oAuthVerifierFactory;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("login")
    class LoginTest {

        private OAuthVerifier mockVerifier;

        @BeforeEach
        void setUp() {
            mockVerifier = mock(OAuthVerifier.class);
        }

        @Test
        @DisplayName("기존 유저 로그인 성공 - 프로필 완성 상태")
        void loginExistingUser() {
            // given
            LoginRequest request = createLoginRequest("google", "oauth-token");

            OAuthUserInfo oAuthUserInfo = new OAuthUserInfo("provider-uid-1", "GOOGLE", "test@gmail.com");
            when(oAuthVerifierFactory.getVerifier("google")).thenReturn(mockVerifier);
            when(mockVerifier.verify("oauth-token")).thenReturn(oAuthUserInfo);

            User existingUser = User.builder()
                    .providerUid("provider-uid-1")
                    .provider(Provider.GOOGLE)
                    .nickname("testUser")
                    .gender(com.itto.backend.domain.common.enums.Gender.MALE)
                    .birthDate(LocalDate.of(1995, 1, 1))
                    .build();

            when(userRepository.findByProviderAndProviderUid(Provider.GOOGLE, "provider-uid-1"))
                    .thenReturn(Optional.of(existingUser));
            when(jwtProvider.generateAccessToken(any(), anyString())).thenReturn("access-token");
            when(jwtProvider.generateRefreshToken(any())).thenReturn("refresh-token");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(jwtProperties.getRefreshTokenExpiry()).thenReturn(604800000L);

            // when
            LoginResponse response = authService.login(request);

            // then
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.isNewUser()).isFalse();
        }

        @Test
        @DisplayName("신규 유저 로그인 - 유저 생성 후 isNewUser=true")
        void loginNewUser() {
            // given
            LoginRequest request = createLoginRequest("google", "oauth-token");

            OAuthUserInfo oAuthUserInfo = new OAuthUserInfo("new-uid", "GOOGLE", "new@gmail.com");
            when(oAuthVerifierFactory.getVerifier("google")).thenReturn(mockVerifier);
            when(mockVerifier.verify("oauth-token")).thenReturn(oAuthUserInfo);

            when(userRepository.findByProviderAndProviderUid(Provider.GOOGLE, "new-uid"))
                    .thenReturn(Optional.empty());

            User newUser = User.builder()
                    .providerUid("new-uid")
                    .provider(Provider.GOOGLE)
                    .build();
            when(userRepository.save(any(User.class))).thenReturn(newUser);
            when(jwtProvider.generateAccessToken(any(), anyString())).thenReturn("access-token");
            when(jwtProvider.generateRefreshToken(any())).thenReturn("refresh-token");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(jwtProperties.getRefreshTokenExpiry()).thenReturn(604800000L);

            // when
            LoginResponse response = authService.login(request);

            // then
            assertThat(response.isNewUser()).isTrue();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("탈퇴한 유저 로그인 시도 → BusinessException")
        void loginWithdrawnUser() {
            // given
            LoginRequest request = createLoginRequest("google", "oauth-token");

            OAuthUserInfo oAuthUserInfo = new OAuthUserInfo("withdrawn-uid", "GOOGLE", "withdrawn@gmail.com");
            when(oAuthVerifierFactory.getVerifier("google")).thenReturn(mockVerifier);
            when(mockVerifier.verify("oauth-token")).thenReturn(oAuthUserInfo);

            User withdrawnUser = User.builder()
                    .providerUid("withdrawn-uid")
                    .provider(Provider.GOOGLE)
                    .build();
            withdrawnUser.softDelete();

            when(userRepository.findByProviderAndProviderUid(Provider.GOOGLE, "withdrawn-uid"))
                    .thenReturn(Optional.of(withdrawnUser));

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_ALREADY_WITHDRAWN));
        }
    }

    @Nested
    @DisplayName("refresh")
    class RefreshTest {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 갱신 성공")
        void refreshSuccess() {
            // given
            RefreshRequest request = createRefreshRequest("valid-refresh-token");

            when(jwtProvider.validateToken("valid-refresh-token")).thenReturn(true);
            when(jwtProvider.getUserIdFromToken("valid-refresh-token")).thenReturn(1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("refresh:1")).thenReturn("valid-refresh-token");

            User user = User.builder()
                    .providerUid("uid")
                    .provider(Provider.GOOGLE)
                    .build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jwtProvider.generateAccessToken(1L, "USER")).thenReturn("new-access-token");
            when(jwtProvider.generateRefreshToken(1L)).thenReturn("new-refresh-token");
            when(jwtProperties.getRefreshTokenExpiry()).thenReturn(604800000L);

            // when
            RefreshResponse response = authService.refresh(request);

            // then
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰 → BusinessException")
        void refreshInvalidToken() {
            // given
            RefreshRequest request = createRefreshRequest("invalid-token");
            when(jwtProvider.validateToken("invalid-token")).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("Redis에 저장된 토큰과 불일치 → BusinessException")
        void refreshTokenMismatch() {
            // given
            RefreshRequest request = createRefreshRequest("refresh-token");
            when(jwtProvider.validateToken("refresh-token")).thenReturn(true);
            when(jwtProvider.getUserIdFromToken("refresh-token")).thenReturn(1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("refresh:1")).thenReturn("different-token");

            // when & then
            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("존재하지 않는 유저 → BusinessException")
        void refreshUserNotFound() {
            // given
            RefreshRequest request = createRefreshRequest("refresh-token");
            when(jwtProvider.validateToken("refresh-token")).thenReturn(true);
            when(jwtProvider.getUserIdFromToken("refresh-token")).thenReturn(999L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("refresh:999")).thenReturn("refresh-token");
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("탈퇴한 유저의 토큰 갱신 시도 → BusinessException")
        void refreshWithdrawnUser() {
            // given
            RefreshRequest request = createRefreshRequest("refresh-token");
            when(jwtProvider.validateToken("refresh-token")).thenReturn(true);
            when(jwtProvider.getUserIdFromToken("refresh-token")).thenReturn(1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("refresh:1")).thenReturn("refresh-token");

            User withdrawnUser = User.builder()
                    .providerUid("uid")
                    .provider(Provider.GOOGLE)
                    .build();
            withdrawnUser.softDelete();
            when(userRepository.findById(1L)).thenReturn(Optional.of(withdrawnUser));

            // when & then
            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_ALREADY_WITHDRAWN));
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 시 Redis에서 리프레시 토큰 삭제")
        void logoutDeletesRedisToken() {
            // given
            Long userId = 1L;

            // when
            authService.logout(userId);

            // then
            verify(redisTemplate).delete("refresh:1");
        }
    }

    @Nested
    @DisplayName("withdraw")
    class WithdrawTest {

        @Test
        @DisplayName("회원탈퇴 성공 - softDelete + Redis 삭제 + FCM 삭제")
        void withdrawSuccess() {
            // given
            Long userId = 1L;
            WithdrawRequest request = new WithdrawRequest();

            User user = User.builder()
                    .providerUid("uid")
                    .provider(Provider.GOOGLE)
                    .build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // when
            authService.withdraw(userId, request);

            // then
            assertThat(user.isDeleted()).isTrue();
            verify(redisTemplate).delete("refresh:1");
            verify(fcmTokenRepository).deleteByUserUserId(userId);
        }

        @Test
        @DisplayName("존재하지 않는 유저 탈퇴 → BusinessException")
        void withdrawUserNotFound() {
            // given
            Long userId = 999L;
            WithdrawRequest request = new WithdrawRequest();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.withdraw(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("이미 탈퇴한 유저 재탈퇴 → BusinessException")
        void withdrawAlreadyWithdrawn() {
            // given
            Long userId = 1L;
            WithdrawRequest request = new WithdrawRequest();

            User withdrawnUser = User.builder()
                    .providerUid("uid")
                    .provider(Provider.GOOGLE)
                    .build();
            withdrawnUser.softDelete();
            when(userRepository.findById(userId)).thenReturn(Optional.of(withdrawnUser));

            // when & then
            assertThatThrownBy(() -> authService.withdraw(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_ALREADY_WITHDRAWN));
        }
    }

    // --- Helper methods ---

    private LoginRequest createLoginRequest(String provider, String oauthToken) {
        LoginRequest request = new LoginRequest();
        setField(request, "provider", provider);
        setField(request, "oauthToken", oauthToken);
        return request;
    }

    private RefreshRequest createRefreshRequest(String refreshToken) {
        RefreshRequest request = new RefreshRequest();
        setField(request, "refreshToken", refreshToken);
        return request;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
