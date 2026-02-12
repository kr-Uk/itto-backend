package com.itto.backend.domain.block.dto;

import com.itto.backend.global.enums.ReportCategory;
import com.itto.backend.global.enums.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {

    @NotNull(message = "신고 대상 사용자 ID는 필수입니다")
    private Long targetUserId;

    @NotNull(message = "신고 카테고리는 필수입니다")
    private ReportCategory category;

    @NotNull(message = "신고 대상 타입은 필수입니다")
    private ReportTargetType targetType;

    @Size(max = 100, message = "채팅방 ID는 100자 이하여야 합니다")
    private String chatRoomId;

    @Size(max = 500, message = "신고 내용은 500자 이하여야 합니다")
    private String description;
}
