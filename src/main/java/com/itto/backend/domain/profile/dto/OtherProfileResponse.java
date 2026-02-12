package com.itto.backend.domain.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtherProfileResponse {

    private Long userId;
    private String nickname;
    private String relationshipStatus;
    private String gender;
    private Integer age;
    private List<HobbyDto> hobbies;
    private String introduction;
    private List<PhotoDto> photos;
    private Integer encounterCount;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class HobbyDto {
        private Long id;
        private String name;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PhotoDto {
        private String url;
        private boolean isMosaic;
        private boolean isRepresentative;
    }
}
