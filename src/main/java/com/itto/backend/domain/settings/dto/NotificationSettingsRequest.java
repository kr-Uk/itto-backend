package com.itto.backend.domain.settings.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationSettingsRequest {

    private boolean chatNotification;
    private boolean relationshipNotification;
    private boolean statsNotification;
    private boolean marketingNotification;
}
