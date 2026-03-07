package com.itto.backend.presentation.dto.location;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationToggleRequest {

    @NotNull(message = "enabled 값은 필수입니다")
    private Boolean enabled;
}
