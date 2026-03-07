package com.itto.backend.presentation.dto.relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class RelationshipDeleteResponse {
    private String status;
    private String message;
    private LocalDateTime expiresAt;
}
