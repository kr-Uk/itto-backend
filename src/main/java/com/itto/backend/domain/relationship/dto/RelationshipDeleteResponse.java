package com.itto.backend.domain.relationship.dto;

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
