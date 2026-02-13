package com.itto.backend.domain.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class H3CellInfo {

    private String h3Index;
    private H3Center center;
    private List<List<Double>> boundary;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class H3Center {
        private double latitude;
        private double longitude;
    }
}
