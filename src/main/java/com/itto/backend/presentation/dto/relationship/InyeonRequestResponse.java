package com.itto.backend.presentation.dto.relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InyeonRequestResponse {
    private String status;
    private String message;
    private int remainingFreeRequests;
}
