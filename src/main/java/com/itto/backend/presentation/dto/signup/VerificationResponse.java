package com.itto.backend.presentation.dto.signup;

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
