package com.itto.backend.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @NotNull(message = "스토어 정보는 필수입니다")
    @Pattern(regexp = "^(APPLE|GOOGLE)$", message = "스토어는 APPLE 또는 GOOGLE이어야 합니다")
    @Schema(description = "앱 스토어 종류", example = "APPLE", allowableValues = {"APPLE", "GOOGLE"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String store;
}
