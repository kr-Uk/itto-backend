package com.itto.backend.domain.contact.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class ContactHashListResponse {

    private List<ContactHashInfo> hashes;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ContactHashInfo {
        private Long id;
        private String phoneHash;
        private LocalDateTime createdAt;
    }
}
