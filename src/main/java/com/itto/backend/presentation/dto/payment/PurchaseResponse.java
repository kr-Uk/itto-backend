package com.itto.backend.presentation.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "매듭 구매 응답")
public class PurchaseResponse {

    @Schema(description = "추가된 매듭 개수", example = "160")
    private Integer knotsAdded;

    @Schema(description = "구매 후 총 매듭 개수", example = "310")
    private Integer totalKnots;

    @Schema(description = "첫 구매 보너스 적용 여부", example = "true")
    private Boolean isFirstPurchaseBonus;

    @Schema(description = "메시지", example = "구매가 완료되었습니다")
    private String message;
}
