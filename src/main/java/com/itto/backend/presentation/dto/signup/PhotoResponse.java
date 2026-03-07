package com.itto.backend.presentation.dto.signup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PhotoResponse {

    private Long id;
    private String url;
    private boolean isRepresentative;
}
