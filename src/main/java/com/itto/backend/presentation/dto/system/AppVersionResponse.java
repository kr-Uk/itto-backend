package com.itto.backend.presentation.dto.system;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AppVersionResponse {

    private String minVersion;
    private String latestVersion;
    private boolean forceUpdate;
    private String updateUrl;
}
