package com.itto.backend.presentation.dto.system;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NicknameValidateResponse {
    private boolean valid;
    private String message;
    private String code;
}
