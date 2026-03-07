package com.itto.backend.presentation.dto.system;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HobbyResponse {
    private Long id;
    private String name;
    private String category;
}
