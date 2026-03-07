package com.itto.backend.application.notification.port;

import java.util.Map;

public interface PushNotificationPort {

    void send(Long userId, String title, String body, Map<String, String> data);
}
