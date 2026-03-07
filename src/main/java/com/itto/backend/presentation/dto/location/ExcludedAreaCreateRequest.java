package com.itto.backend.presentation.dto.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcludedAreaCreateRequest {

    @NotBlank(message = "지역 이름은 필수입니다")
    @Size(max = 50, message = "지역 이름은 50자 이하여야 합니다")
    private String name;

    @NotNull(message = "위도는 필수입니다")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다")
    private Double longitude;

    @NotNull(message = "반경은 필수입니다")
    @Positive(message = "반경은 양수여야 합니다")
    private Integer radius;
}
