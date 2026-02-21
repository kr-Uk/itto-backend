package com.itto.backend.domain.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class IdentityRequestResponse {
    private String requestNo;
    private String authUrl;
}
