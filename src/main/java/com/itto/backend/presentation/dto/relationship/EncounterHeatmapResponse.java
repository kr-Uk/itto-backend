package com.itto.backend.presentation.dto.relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class EncounterHeatmapResponse {

    private List<HeatmapCell> cells;
    private double centerLat;
    private double centerLng;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class HeatmapCell {
        private String h3Index;
        private List<List<Double>> boundary;
        private double centerLat;
        private double centerLng;
        private int count;
    }
}
