package com.itto.backend.presentation.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatImageUploadResponse {
    private String imageUrl;
}
