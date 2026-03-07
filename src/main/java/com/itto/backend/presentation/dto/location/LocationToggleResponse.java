package com.itto.backend.presentation.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LocationToggleResponse {

    private Boolean enabled;
    private String message;

    public static LocationToggleResponse of(Boolean enabled) {
        String message = enabled ? "인연 만나기가 활성화되었습니다" : "인연 만나기가 비활성화되었습니다";
        return LocationToggleResponse.builder()
                .enabled(enabled)
                .message(message)
                .build();
    }
}
