package com.itto.backend.domain.relationship.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RelationshipDetailResponse {

    private Long relationshipId;
    private OtherUserDetail otherUser;
    private String status;
    private int encounterCount;
    private LocalDateTime firstEncounterAt;
    private LocalDateTime lastEncounterAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private String chatRoomId;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OtherUserDetail {
        private Long userId;
        private String nickname;
        private String gender;
        private int age;
        private List<HobbyInfo> hobbies;
        private String introduction;
        private List<PhotoInfo> photos;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class HobbyInfo {
        private Long id;
        private String name;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PhotoInfo {
        private String url;
        private boolean isMosaic;
        private boolean isRepresentative;
    }
}
