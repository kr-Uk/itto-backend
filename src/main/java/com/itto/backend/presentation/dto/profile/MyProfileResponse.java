package com.itto.backend.presentation.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MyProfileResponse {

    private Long userId;
    private String nickname;
    private String gender;
    private LocalDate birthDate;
    private int age;
    private List<HobbyDto> hobbies;
    private String introduction;
    private List<PhotoDto> photos;
    private String verificationStatus;
    private int knots;
    private boolean isPremium;
    private LocalDateTime createdAt;

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
        private Long id;
        private String url;
        private boolean isRepresentative;
    }
}
