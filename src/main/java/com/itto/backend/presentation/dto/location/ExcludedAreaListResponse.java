package com.itto.backend.presentation.dto.location;

import com.itto.backend.domain.location.entity.ExcludedArea;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ExcludedAreaListResponse {

    private List<ExcludedAreaInfo> excludedAreas;

    public static ExcludedAreaListResponse from(List<ExcludedAreaInfo> excludedAreas) {
        return ExcludedAreaListResponse.builder()
                .excludedAreas(excludedAreas)
                .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExcludedAreaInfo {
        private Long id;
        private String name;
        private Double latitude;
        private Double longitude;
        private Integer radius;
        private List<String> h3Indexes;

        public static ExcludedAreaInfo from(ExcludedArea excludedArea, List<String> h3Indexes) {
            return ExcludedAreaInfo.builder()
                    .id(excludedArea.getId())
                    .name(excludedArea.getName())
                    .latitude(excludedArea.getLatitude())
                    .longitude(excludedArea.getLongitude())
                    .radius(excludedArea.getRadius())
                    .h3Indexes(h3Indexes)
                    .build();
        }
    }
}
