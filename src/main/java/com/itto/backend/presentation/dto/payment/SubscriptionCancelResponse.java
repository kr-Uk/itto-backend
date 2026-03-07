package com.itto.backend.presentation.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "구독 해지 응답")
public class SubscriptionCancelResponse {

    @Schema(description = "메시지", example = "구독이 해지되었습니다. 만료일까지 혜택이 유지됩니다")
    private String message;

    @Schema(description = "구독 만료 일시", example = "2026-02-15T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
}
