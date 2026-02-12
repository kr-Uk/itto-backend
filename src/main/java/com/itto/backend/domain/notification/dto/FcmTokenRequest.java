package com.itto.backend.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FcmTokenRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다")
    private String fcmToken;

    @NotBlank(message = "디바이스 ID는 필수입니다")
    private String deviceId;
}
