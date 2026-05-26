package com.itto.backend.presentation.dto.chat;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRoomListResponse {
    private List<ChatRoomSummary> rooms;
}
