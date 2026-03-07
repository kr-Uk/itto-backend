package com.itto.backend.presentation.dto.contact;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactHashUploadRequest {

    @NotNull(message = "해시 목록은 필수입니다")
    private List<String> hashes;
}
