package com.itto.backend.presentation.dto.contact;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ContactHashUploadResponse {
    private int uploadedCount;
}
