package com.itto.backend.domain.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRequest {

    @NotBlank(message = "H3 인덱스는 필수입니다")
    private String h3Index;

    @NotNull(message = "위도는 필수입니다")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다")
    private Double longitude;

    @NotNull(message = "정확도는 필수입니다")
    private Double accuracy;

    @NotNull(message = "타임스탬프는 필수입니다")
    private LocalDateTime timestamp;
}
