package com.itto.backend.domain.relationship.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RelationshipListResponse {

    private List<RelationshipSummary> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RelationshipSummary {
        private Long relationshipId;
        private OtherUserSummary otherUser;
        private String status;
        private int encounterCount;
        private LocalDateTime lastEncounterAt;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OtherUserSummary {
        private Long userId;
        private String nickname;
        private String representativePhoto;
        private boolean isMosaic;
    }
}
