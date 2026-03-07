package com.itto.backend.presentation.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MatchingSettingsResponse {

    private String genderFilter;
    private int ageRangeMin;
    private int ageRangeMax;
}
