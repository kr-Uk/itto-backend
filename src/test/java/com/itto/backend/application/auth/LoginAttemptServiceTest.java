package com.itto.backend.application.auth;

import com.itto.backend.domain.common.constants.BusinessConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAttemptService")
class LoginAttemptServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginAttemptService loginAttemptService;

    private static final String TEST_IP = "192.168.1.1";
    private static final String KEY = BusinessConstants.LOGIN_ATTEMPT_PREFIX + TEST_IP;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService(redisTemplate);
    }

    @Nested
    @DisplayName("isBlocked")
    class IsBlocked {

        @Test
        @DisplayName("시도 기록이 없으면 차단 안 됨")
        void notBlockedWhenNoAttempts() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(KEY)).thenReturn(null);

            assertThat(loginAttemptService.isBlocked(TEST_IP)).isFalse();
        }

        @Test
        @DisplayName("최대 시도 미만이면 차단 안 됨")
        void notBlockedWhenBelowMax() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(KEY)).thenReturn("4");

            assertThat(loginAttemptService.isBlocked(TEST_IP)).isFalse();
        }

        @Test
        @DisplayName("최대 시도 도달 시 차단")
        void blockedWhenMaxReached() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(KEY)).thenReturn("5");

            assertThat(loginAttemptService.isBlocked(TEST_IP)).isTrue();
        }

        @Test
        @DisplayName("최대 시도 초과 시에도 차단")
        void blockedWhenOverMax() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(KEY)).thenReturn("10");

            assertThat(loginAttemptService.isBlocked(TEST_IP)).isTrue();
        }
    }

    @Nested
    @DisplayName("recordFailure")
    class RecordFailure {

        @Test
        @DisplayName("첫 실패 시 카운트 증가 + TTL 설정")
        void firstFailureSetsExpiry() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.increment(KEY)).thenReturn(1L);

            loginAttemptService.recordFailure(TEST_IP);

            verify(valueOperations).increment(KEY);
            verify(redisTemplate).expire(KEY, Duration.ofMinutes(BusinessConstants.LOGIN_LOCKOUT_MINUTES));
        }

        @Test
        @DisplayName("두 번째 이후 실패 시 카운트만 증가 (TTL 미변경)")
        void subsequentFailureNoExpiry() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.increment(KEY)).thenReturn(2L);

            loginAttemptService.recordFailure(TEST_IP);

            verify(valueOperations).increment(KEY);
            verify(redisTemplate, never()).expire(any(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("resetAttempts")
    class ResetAttempts {

        @Test
        @DisplayName("로그인 성공 시 카운트 초기화")
        void resetsOnSuccess() {
            loginAttemptService.resetAttempts(TEST_IP);

            verify(redisTemplate).delete(KEY);
        }
    }
}
