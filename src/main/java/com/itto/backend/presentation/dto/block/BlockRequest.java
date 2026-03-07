package com.itto.backend.presentation.dto.block;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockRequest {

    @NotNull(message = "차단할 사용자 ID는 필수입니다")
    private Long targetUserId;
}
