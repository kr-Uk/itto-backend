package com.itto.backend.presentation.dto.relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class InyeonAcceptResponse {
    private String status;
    private String chatRoomId;
    private String message;
    private LocalDateTime expiresAt;
}
