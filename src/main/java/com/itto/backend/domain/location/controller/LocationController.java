package com.itto.backend.domain.location.controller;

import com.itto.backend.domain.location.dto.*;
import com.itto.backend.domain.location.service.LocationService;
import com.itto.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Location", description = "위치 API")
@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @Operation(summary = "위치 데이터 전송", description = "사용자의 현재 위치 데이터를 전송합니다")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> sendLocation(
            Authentication authentication,
            @Valid @RequestBody LocationRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        locationService.sendLocation(userId, request);
        return ResponseEntity.ok(ApiResponse.success("위치가 전송되었습니다"));
    }

    @Operation(summary = "인연 만나기 토글", description = "인연 만나기 기능을 활성화/비활성화합니다")
    @PutMapping("/toggle")
    public ResponseEntity<ApiResponse<LocationToggleResponse>> toggleLocation(
            Authentication authentication,
            @Valid @RequestBody LocationToggleRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        LocationToggleResponse response = locationService.toggleLocation(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "제외 지역 목록 조회", description = "등록된 제외 지역 목록을 조회합니다")
    @GetMapping("/excluded")
    public ResponseEntity<ApiResponse<ExcludedAreaListResponse>> getExcludedAreas(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ExcludedAreaListResponse response = locationService.getExcludedAreas(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "제외 지역 등록", description = "새로운 제외 지역을 등록합니다 (최대 3개)")
    @PostMapping("/excluded")
    public ResponseEntity<ApiResponse<ExcludedAreaCreateResponse>> createExcludedArea(
            Authentication authentication,
            @Valid @RequestBody ExcludedAreaCreateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        ExcludedAreaCreateResponse response = locationService.createExcludedArea(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("제외 지역이 등록되었습니다", response));
    }

    @Operation(summary = "제외 지역 삭제", description = "등록된 제외 지역을 삭제합니다")
    @DeleteMapping("/excluded/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExcludedArea(
            Authentication authentication,
            @PathVariable("id") Long excludedAreaId) {
        Long userId = (Long) authentication.getPrincipal();
        locationService.deleteExcludedArea(userId, excludedAreaId);
        return ResponseEntity.ok(ApiResponse.success("제외 지역이 삭제되었습니다"));
    }

    @Operation(summary = "H3 그리드 조회", description = "주어진 좌표 주변의 H3 셀 그리드를 조회합니다")
    @PostMapping("/h3/grid")
    public ResponseEntity<ApiResponse<H3GridResponse>> getH3Grid(
            Authentication authentication,
            @Valid @RequestBody H3GridRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        H3GridResponse response = locationService.getH3Grid(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "제외 셀 저장", description = "H3 셀 인덱스 목록으로 제외 지역을 저장합니다 (최대 20개)")
    @PutMapping("/excluded/cells")
    public ResponseEntity<ApiResponse<ExcludedCellsResponse>> saveExcludedCells(
            Authentication authentication,
            @Valid @RequestBody ExcludedCellsSaveRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        ExcludedCellsResponse response = locationService.saveExcludedCells(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
