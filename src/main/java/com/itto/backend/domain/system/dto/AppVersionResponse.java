package com.itto.backend.domain.system.dto;

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
