package com.itto.backend.presentation.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "매듭 패키지 목록 응답")
public class PackageListResponse {

    @Schema(description = "매듭 패키지 목록")
    private List<PackageInfo> packages;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "매듭 패키지 정보")
    public static class PackageInfo {

        @Schema(description = "패키지 ID", example = "1")
        private Long id;

        @Schema(description = "패키지 이름", example = "실마리")
        private String name;

        @Schema(description = "가격 (원)", example = "7900")
        private Integer price;

        @Schema(description = "기본 매듭 개수", example = "80")
        private Integer knots;

        @Schema(description = "첫 구매 보너스 매듭 개수", example = "160")
        private Integer firstPurchaseKnots;

        @Schema(description = "첫 구매 보너스 사용 여부", example = "false")
        private Boolean isFirstPurchaseUsed;
    }
}
