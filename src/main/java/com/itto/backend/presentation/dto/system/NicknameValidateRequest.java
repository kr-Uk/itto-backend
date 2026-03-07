package com.itto.backend.presentation.dto.system;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NicknameValidateRequest {
    @NotBlank(message = "닉네임은 필수입니다")
    private String nickname;
}
