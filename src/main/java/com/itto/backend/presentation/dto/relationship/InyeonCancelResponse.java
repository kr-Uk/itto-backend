package com.itto.backend.presentation.dto.relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InyeonCancelResponse {
    private String status;
    private String message;
}
