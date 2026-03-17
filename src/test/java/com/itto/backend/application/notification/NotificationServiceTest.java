package com.itto.backend.application.notification;

import com.itto.backend.domain.common.enums.NotificationType;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import com.itto.backend.domain.notification.entity.Notification;
import com.itto.backend.domain.notification.repository.NotificationRepository;
import com.itto.backend.domain.user.entity.FcmToken;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.FcmTokenRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.presentation.dto.notification.FcmTokenRequest;
import com.itto.backend.presentation.dto.notification.NotificationPageResponse;
import com.itto.backend.presentation.dto.notification.NotificationReadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .providerUid("test-uid")
                .provider(com.itto.backend.domain.common.enums.Provider.GOOGLE)
                .nickname("testUser")
                .build();
    }

    @Nested
    @DisplayName("getNotifications")
    class GetNotificationsTest {

        @Test
        @DisplayName("페이지네이션 알림 목록 조회 성공")
        void getNotificationsSuccess() {
            // given
            Long userId = 1L;
            Notification notification = Notification.builder()
                    .user(testUser)
                    .type(NotificationType.INYEON_REQUEST)
                    .title("새로운 스침")
                    .message("누군가와 스쳤습니다")
                    .build();

            Page<Notification> page = new PageImpl<>(
                    List.of(notification),
                    PageRequest.of(0, 10, Sort.by("createdAt").descending()),
                    1
            );
            when(notificationRepository.findByUserUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(page);

            // when
            NotificationPageResponse response = notificationService.getNotifications(userId, 0, 10);

            // then
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("알림이 없는 경우 빈 목록 반환")
        void getNotificationsEmpty() {
            // given
            Long userId = 1L;
            Page<Notification> emptyPage = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 10, Sort.by("createdAt").descending()),
                    0
            );
            when(notificationRepository.findByUserUserId(eq(userId), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // when
            NotificationPageResponse response = notificationService.getNotifications(userId, 0, 10);

            // then
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTest {

        @Test
        @DisplayName("알림 읽음 처리 성공")
        void markAsReadSuccess() {
            // given
            Long userId = 1L;
            Notification notification1 = Notification.builder()
                    .user(testUser)
                    .type(NotificationType.INYEON_REQUEST)
                    .title("title1")
                    .message("msg1")
                    .build();
            Notification notification2 = Notification.builder()
                    .user(testUser)
                    .type(NotificationType.INYEON_REQUEST)
                    .title("title2")
                    .message("msg2")
                    .build();

            List<Long> ids = List.of(1L, 2L);
            NotificationReadRequest request = createNotificationReadRequest(ids);

            when(notificationRepository.findByIdInAndUserUserId(ids, userId))
                    .thenReturn(List.of(notification1, notification2));

            // when
            notificationService.markAsRead(userId, request);

            // then
            assertThat(notification1.getIsRead()).isTrue();
            assertThat(notification2.getIsRead()).isTrue();
        }

        @Test
        @DisplayName("유효한 알림이 없는 경우 아무 동작 없이 리턴")
        void markAsReadNoValidNotifications() {
            // given
            Long userId = 1L;
            List<Long> ids = List.of(999L);
            NotificationReadRequest request = createNotificationReadRequest(ids);

            when(notificationRepository.findByIdInAndUserUserId(ids, userId))
                    .thenReturn(List.of());

            // when (예외 발생하지 않음)
            notificationService.markAsRead(userId, request);

            // then - no exception thrown
        }
    }

    @Nested
    @DisplayName("deleteAll")
    class DeleteAllTest {

        @Test
        @DisplayName("전체 알림 삭제 성공")
        void deleteAllSuccess() {
            // given
            Long userId = 1L;

            // when
            notificationService.deleteAll(userId);

            // then
            verify(notificationRepository).deleteByUserUserId(userId);
        }
    }

    @Nested
    @DisplayName("registerFcmToken")
    class RegisterFcmTokenTest {

        @Test
        @DisplayName("신규 FCM 토큰 등록 성공")
        void registerNewFcmToken() {
            // given
            Long userId = 1L;
            FcmTokenRequest request = createFcmTokenRequest("fcm-token-123", "device-1");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(fcmTokenRepository.findByUserUserIdAndDeviceId(userId, "device-1"))
                    .thenReturn(Optional.empty());

            // when
            notificationService.registerFcmToken(userId, request);

            // then
            verify(fcmTokenRepository).save(any(FcmToken.class));
        }

        @Test
        @DisplayName("기존 FCM 토큰 업데이트 성공")
        void updateExistingFcmToken() {
            // given
            Long userId = 1L;
            FcmTokenRequest request = createFcmTokenRequest("new-fcm-token", "device-1");

            FcmToken existingToken = FcmToken.builder()
                    .user(testUser)
                    .token("old-fcm-token")
                    .deviceId("device-1")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(fcmTokenRepository.findByUserUserIdAndDeviceId(userId, "device-1"))
                    .thenReturn(Optional.of(existingToken));

            // when
            notificationService.registerFcmToken(userId, request);

            // then
            assertThat(existingToken.getToken()).isEqualTo("new-fcm-token");
            verify(fcmTokenRepository, never()).save(any(FcmToken.class));
        }

        @Test
        @DisplayName("존재하지 않는 유저 → BusinessException")
        void registerFcmTokenUserNotFound() {
            // given
            Long userId = 999L;
            FcmTokenRequest request = createFcmTokenRequest("fcm-token", "device-1");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.registerFcmToken(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createAndPush")
    class CreateAndPushTest {

        @Test
        @DisplayName("알림 저장 성공")
        void createAndPushSavesNotification() {
            // given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(fcmTokenRepository.findByUserUserId(userId)).thenReturn(List.of());

            // when
            notificationService.createAndPush(userId, NotificationType.INYEON_REQUEST,
                    "새로운 스침", "누군가와 스쳤습니다", "/encounter/1");

            // then
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("존재하지 않는 유저 → BusinessException")
        void createAndPushUserNotFound() {
            // given
            Long userId = 999L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.createAndPush(userId,
                    NotificationType.INYEON_REQUEST, "title", "msg", "/link"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    // --- Helper methods ---

    private NotificationReadRequest createNotificationReadRequest(List<Long> ids) {
        NotificationReadRequest request = new NotificationReadRequest();
        setField(request, "notificationIds", ids);
        return request;
    }

    private FcmTokenRequest createFcmTokenRequest(String fcmToken, String deviceId) {
        FcmTokenRequest request = new FcmTokenRequest();
        setField(request, "fcmToken", fcmToken);
        setField(request, "deviceId", deviceId);
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
