package com.itto.backend.domain.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NotificationSettingsResponse {

    private boolean chatNotification;
    private boolean relationshipNotification;
    private boolean statsNotification;
    private boolean marketingNotification;
}
