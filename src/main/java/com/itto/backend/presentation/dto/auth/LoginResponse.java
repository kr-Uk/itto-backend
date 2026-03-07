package com.itto.backend.presentation.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;

    @JsonProperty("isNewUser")
    private boolean isNewUser;
    private String verificationStatus;
}
