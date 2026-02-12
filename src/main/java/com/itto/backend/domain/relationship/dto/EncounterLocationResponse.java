package com.itto.backend.domain.relationship.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class EncounterLocationResponse {
    private String address;
    private double latitude;
    private double longitude;
    private LocalDateTime encounterAt;
}
