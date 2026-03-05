package com.itto.backend.domain.contact.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualBlockRequest {

    @NotBlank(message = "전화번호는 필수입니다")
    private String phoneNumber;
}
