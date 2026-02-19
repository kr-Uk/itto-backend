package com.itto.backend.domain.notification.service;

import com.itto.backend.domain.notification.dto.FcmTokenRequest;
import com.itto.backend.domain.notification.dto.NotificationPageResponse;
import com.itto.backend.domain.notification.dto.NotificationReadRequest;
import com.itto.backend.domain.notification.dto.NotificationResponse;
import com.itto.backend.domain.notification.entity.Notification;
import com.itto.backend.domain.notification.repository.NotificationRepository;
import com.itto.backend.domain.user.entity.FcmToken;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.FcmTokenRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.global.enums.NotificationType;
import com.itto.backend.global.exception.BusinessException;
import com.itto.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    /**
     * Get paginated notifications for a user
     */
    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notificationPage = notificationRepository.findByUserUserId(userId, pageable);

        Page<NotificationResponse> responsePage = notificationPage.map(NotificationResponse::from);
        return NotificationPageResponse.from(responsePage);
    }

    /**
     * Mark notifications as read (only user's own notifications)
     */
    @Transactional
    public void markAsRead(Long userId, NotificationReadRequest request) {
        List<Long> notificationIds = request.getNotificationIds();

        // Fetch notifications that belong to the user
        List<Notification> notifications = notificationRepository
                .findByIdInAndUserUserId(notificationIds, userId);

        if (notifications.isEmpty()) {
            log.warn("No valid notifications found for user: userId={}, requestedIds={}",
                    userId, notificationIds);
            return;
        }

        // Mark each as read
        notifications.forEach(Notification::markAsRead);

        log.info("Marked {} notifications as read for user: userId={}",
                notifications.size(), userId);
    }

    /**
     * Delete all notifications for a user
     */
    @Transactional
    public void deleteAll(Long userId) {
        notificationRepository.deleteByUserUserId(userId);
        log.info("Deleted all notifications for user: userId={}", userId);
    }

    /**
     * Register or update FCM token for a user
     */
    @Transactional
    public void registerFcmToken(Long userId, FcmTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Check if token already exists for this device
        FcmToken existingToken = fcmTokenRepository
                .findByUserUserIdAndDeviceId(userId, request.getDeviceId())
                .orElse(null);

        if (existingToken != null) {
            // Update existing token
            existingToken.updateToken(request.getFcmToken());
            log.info("Updated FCM token for user: userId={}, deviceId={}",
                    userId, request.getDeviceId());
        } else {
            // Create new token
            FcmToken newToken = FcmToken.builder()
                    .user(user)
                    .token(request.getFcmToken())
                    .deviceId(request.getDeviceId())
                    .build();
            fcmTokenRepository.save(newToken);
            log.info("Registered new FCM token for user: userId={}, deviceId={}",
                    userId, request.getDeviceId());
        }
    }

    /**
     * 앱 내 알림 저장 + FCM 푸시 발송
     */
    @Transactional
    public void createAndPush(Long recipientUserId, NotificationType type,
                              String title, String message, String deepLink) {
        User user = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. DB에 알림 저장
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .deepLink(deepLink)
                .build();
        notificationRepository.save(notification);

        // 2. FCM 푸시 발송
        sendFcmPush(recipientUserId, title, message, type.name(), deepLink);
    }

    /**
     * FCM 푸시 발송 (실패해도 예외를 던지지 않음)
     */
    private void sendFcmPush(Long userId, String title, String body,
                             String notificationType, String deepLink) {
        List<FcmToken> tokens = fcmTokenRepository.findByUserUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("FCM 토큰 없음: userId={}", userId);
            return;
        }

        for (FcmToken fcmToken : tokens) {
            try {
                com.google.firebase.messaging.Message message = com.google.firebase.messaging.Message.builder()
                        .setToken(fcmToken.getToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("type", notificationType)
                        .putData("deepLink", deepLink != null ? deepLink : "")
                        .build();

                String messageId = com.google.firebase.messaging.FirebaseMessaging.getInstance().send(message);
                log.info("FCM 발송 성공: userId={}, deviceId={}, messageId={}", userId, fcmToken.getDeviceId(), messageId);
            } catch (com.google.firebase.messaging.FirebaseMessagingException e) {
                log.warn("FCM 발송 실패: userId={}, deviceId={}, error={}", userId, fcmToken.getDeviceId(), e.getMessage());
                // 유효하지 않은 토큰 삭제
                if (isInvalidTokenError(e)) {
                    fcmTokenRepository.delete(fcmToken);
                    log.info("유효하지 않은 FCM 토큰 삭제: userId={}, deviceId={}", userId, fcmToken.getDeviceId());
                }
            }
        }
    }

    private boolean isInvalidTokenError(com.google.firebase.messaging.FirebaseMessagingException e) {
        String code = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "";
        return "UNREGISTERED".equals(code) || "INVALID_ARGUMENT".equals(code);
    }
}
