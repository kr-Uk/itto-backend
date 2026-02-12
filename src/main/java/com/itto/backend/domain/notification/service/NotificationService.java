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
}
