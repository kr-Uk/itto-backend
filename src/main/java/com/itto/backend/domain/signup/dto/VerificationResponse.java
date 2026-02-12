package com.itto.backend.domain.signup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class VerificationResponse {

    private String verificationStatus;
    private String message;
}
