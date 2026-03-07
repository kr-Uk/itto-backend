package com.itto.backend.presentation.dto.notification;

import com.itto.backend.domain.notification.entity.Notification;
import com.itto.backend.domain.common.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private Boolean isRead;
    private String deepLink;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .deepLink(notification.getDeepLink())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
