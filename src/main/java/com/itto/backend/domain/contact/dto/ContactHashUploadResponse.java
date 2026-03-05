package com.itto.backend.domain.contact.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ContactHashUploadResponse {
    private int uploadedCount;
}
