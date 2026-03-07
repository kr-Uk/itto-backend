package com.itto.backend.presentation.dto.relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RestoreRequestResponse {
    private String message;
    private int knotsUsed;
    private int remainingKnots;
}
