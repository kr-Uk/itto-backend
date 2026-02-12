package com.itto.backend.domain.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProfilePhotosResponse {

    private List<PhotoItem> photos;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PhotoItem {
        private Long id;
        private String url;
        private boolean isRepresentative;
    }
}
