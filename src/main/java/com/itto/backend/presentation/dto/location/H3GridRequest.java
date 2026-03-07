package com.itto.backend.presentation.dto.location;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class H3GridRequest {

    @NotNull(message = "위도는 필수입니다")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다")
    private Double longitude;

    @Min(value = 1, message = "k는 최소 1이어야 합니다")
    @Max(value = 6, message = "k는 최대 6이어야 합니다")
    @Builder.Default
    private Integer k = 4;
}
