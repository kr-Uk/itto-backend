package com.itto.backend.presentation.dto.settings;

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
