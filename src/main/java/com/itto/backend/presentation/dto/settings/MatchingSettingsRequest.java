package com.itto.backend.presentation.dto.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MatchingSettingsRequest {

    private String genderFilter;

    @Min(value = 19, message = "최소 나이는 19세 이상이어야 합니다")
    private int ageRangeMin;

    @Max(value = 100, message = "최대 나이는 100세 이하여야 합니다")
    private int ageRangeMax;
}
