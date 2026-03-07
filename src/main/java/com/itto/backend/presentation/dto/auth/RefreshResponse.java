package com.itto.backend.presentation.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RefreshResponse {

    private String accessToken;
    private String refreshToken;
}
