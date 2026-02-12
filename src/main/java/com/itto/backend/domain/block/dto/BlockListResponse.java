package com.itto.backend.domain.block.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockListResponse {

    private List<BlockInfo> blocks;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BlockInfo {
        private Long id;
        private BlockedUserInfo blockedUser;
        private LocalDateTime createdAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BlockedUserInfo {
        private Long userId;
        private String nickname;
    }
}
