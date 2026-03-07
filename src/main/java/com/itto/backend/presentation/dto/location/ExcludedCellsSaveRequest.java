package com.itto.backend.presentation.dto.location;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcludedCellsSaveRequest {

    @Size(max = 20, message = "제외 셀은 최대 20개까지 선택할 수 있습니다")
    private List<String> h3Indexes;
}
