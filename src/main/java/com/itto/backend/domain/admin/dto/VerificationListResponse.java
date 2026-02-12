package com.itto.backend.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class VerificationListResponse {

    private List<VerificationInfo> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class VerificationInfo {
        private Long id;
        private Long userId;
        private String nickname;
        private List<String> profilePhotos;
        private String selfieUrl;
        private LocalDateTime requestedAt;
    }
}
