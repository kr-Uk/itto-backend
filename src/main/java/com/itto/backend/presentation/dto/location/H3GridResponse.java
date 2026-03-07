package com.itto.backend.presentation.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class H3GridResponse {

    private List<H3CellInfo> cells;
    private List<String> selectedIndexes;
}
