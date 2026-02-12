package com.itto.backend.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RejectRequest {

    @NotBlank(message = "거절 사유를 입력해주세요")
    @Size(max = 200, message = "거절 사유는 200자 이하여야 합니다")
    private String reason;
}
