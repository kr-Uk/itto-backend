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
@Schema(description = "구독 상태 응답")
public class SubscriptionResponse {

    @Schema(description = "프리미엄 구독 여부", example = "true")
    private Boolean isPremium;

    @Schema(description = "구독 플랜", example = "MONTHLY")
    private String plan;

    @Schema(description = "구독 가격 (원)", example = "6900")
    private Integer price;

    @Schema(description = "구독 시작 일시", example = "2026-01-15T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;

    @Schema(description = "구독 만료 일시", example = "2026-02-15T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    @Schema(description = "프로모션 적용 여부", example = "true")
    private Boolean isPromotion;

    @Schema(description = "프리미엄 혜택 정보")
    private SubscriptionBenefits benefits;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "프리미엄 혜택 정보")
    public static class SubscriptionBenefits {

        @Schema(description = "월간 무료 변환 횟수", example = "6")
        private Integer convertPerMonth;

        @Schema(description = "무제한 신청 가능 여부", example = "false")
        private Boolean unlimitedRequests;

        @Schema(description = "월간 무료 인연 신청 횟수", example = "6")
        private Integer requestsPerMonth;

        @Schema(description = "월간 무료 복원 횟수", example = "3")
        private Integer freeRestorePerMonth;

        @Schema(description = "광고 제거 여부", example = "true")
        private Boolean adFree;
    }
}
