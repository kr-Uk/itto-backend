package com.itto.backend.domain.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class IdentityStatusResponse {
    private String status;
    private String name;
    private String gender;
    private String birthDate;
    private String phoneNumber;
}
