package com.itto.backend.presentation.dto.notification;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class NotificationReadRequest {

    @NotEmpty(message = "읽음 처리할 알림 ID 목록은 필수입니다")
    private List<Long> notificationIds;
}
