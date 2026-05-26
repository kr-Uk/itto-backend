package com.itto.backend.presentation.dto.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MatchingSettingsRequest {

    @Pattern(
            regexp = "^(?i)(MALE|FEMALE)?$",
            message = "genderFilter는 MALE 또는 FEMALE이어야 합니다"
    )
    private String genderFilter;

    @Min(value = 19, message = "최소 나이는 19세 이상이어야 합니다")
    private int ageRangeMin;

    @Min(value = 19, message = "최대 나이는 19세 이상이어야 합니다")
    @Max(value = 100, message = "최대 나이는 100세 이하여야 합니다")
    private int ageRangeMax;
}
