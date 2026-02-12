package com.itto.backend.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "매듭 구매 요청")
public class PurchaseRequest {

    @NotNull(message = "패키지 ID는 필수입니다")
    @Schema(description = "구매할 패키지 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long packageId;

    @NotNull(message = "영수증 토큰은 필수입니다")
    @Schema(description = "앱스토어 영수증 토큰", example = "receipt_token_string", requiredMode = Schema.RequiredMode.REQUIRED)
    private String receiptToken;
}
