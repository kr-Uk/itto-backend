package com.itto.backend.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "구독 시작 응답")
public class SubscriptionStartResponse {

    @Schema(description = "프리미엄 구독 여부", example = "true")
    private Boolean isPremium;

    @Schema(description = "구독 만료 일시", example = "2026-02-15T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    @Schema(description = "메시지", example = "프리미엄 구독이 시작되었습니다")
    private String message;
}
