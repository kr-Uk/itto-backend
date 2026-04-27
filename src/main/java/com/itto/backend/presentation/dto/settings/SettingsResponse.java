package com.itto.backend.presentation.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SettingsResponse {

    private MatchingSettingsResponse matchingSettings;
    private NotificationSettingsResponse notificationSettings;
}
