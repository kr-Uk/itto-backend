package com.itto.backend.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "구독 시작 요청")
public class SubscriptionStartRequest {

    @NotNull(message = "영수증 토큰은 필수입니다")
    @Schema(description = "앱스토어 영수증 토큰", example = "subscription_receipt_token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String receiptToken;
}
