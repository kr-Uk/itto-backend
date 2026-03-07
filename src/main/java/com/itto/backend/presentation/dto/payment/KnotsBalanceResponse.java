package com.itto.backend.presentation.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "매듭 잔액 응답")
public class KnotsBalanceResponse {

    @Schema(description = "현재 매듭 잔액", example = "150")
    private Integer knots;

    @Schema(description = "남은 무료 변환 횟수 (월간)", example = "1")
    private Integer freeConvertRemaining;

    @Schema(description = "남은 무료 신청 횟수 (월간)", example = "3")
    private Integer freeRequestRemaining;

    @Schema(description = "남은 무료 복원 횟수 (월간)", example = "0")
    private Integer freeRestoreRemaining;
}
