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
public class ExcludedCellsResponse {

    private List<String> h3Indexes;
    private int count;

    public static ExcludedCellsResponse of(List<String> h3Indexes) {
        return ExcludedCellsResponse.builder()
                .h3Indexes(h3Indexes)
                .count(h3Indexes.size())
                .build();
    }
}
