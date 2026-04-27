package com.itto.backend.presentation.dto.settings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SettingsUpdateRequest {

    @Valid
    @NotNull(message = "matchingSettings는 필수입니다")
    private MatchingSettingsRequest matchingSettings;

    @Valid
    @NotNull(message = "notificationSettings는 필수입니다")
    private NotificationSettingsRequest notificationSettings;
}
