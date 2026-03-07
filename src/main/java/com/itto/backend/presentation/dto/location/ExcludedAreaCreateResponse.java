package com.itto.backend.presentation.dto.location;

import com.itto.backend.domain.location.entity.ExcludedArea;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ExcludedAreaCreateResponse {

    private Long id;
    private String name;
    private List<String> h3Indexes;
    private String message;

    public static ExcludedAreaCreateResponse from(ExcludedArea excludedArea, List<String> h3Indexes) {
        return ExcludedAreaCreateResponse.builder()
                .id(excludedArea.getId())
                .name(excludedArea.getName())
                .h3Indexes(h3Indexes)
                .message("제외 지역이 등록되었습니다")
                .build();
    }
}
