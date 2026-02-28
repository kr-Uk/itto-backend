package com.itto.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "OAuth 토큰은 필수입니다")
    private String oauthToken;

    @NotNull(message = "소셜 로그인 제공자는 필수입니다")
    private String provider;

    private String fcmToken;
}
