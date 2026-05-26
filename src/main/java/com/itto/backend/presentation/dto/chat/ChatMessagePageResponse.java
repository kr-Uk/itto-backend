package com.itto.backend.presentation.dto.chat;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatMessagePageResponse {
    private List<ChatMessageResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
