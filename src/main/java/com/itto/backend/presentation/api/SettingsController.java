package com.itto.backend.presentation.api;

import com.itto.backend.presentation.dto.settings.*;
import com.itto.backend.application.settings.SettingsService;
import com.itto.backend.presentation.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Settings", description = "설정 API")
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @Operation(summary = "매칭 설정 조회", description = "성별 필터, 나이 범위 설정을 조회합니다")
    @GetMapping("/matching")
    public ResponseEntity<ApiResponse<MatchingSettingsResponse>> getMatchingSettings(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        MatchingSettingsResponse response = settingsService.getMatchingSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "매칭 설정 변경", description = "성별 필터, 나이 범위 설정을 변경합니다")
    @PutMapping("/matching")
    public ResponseEntity<ApiResponse<Void>> updateMatchingSettings(
            Authentication authentication,
            @Valid @RequestBody MatchingSettingsRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        settingsService.updateMatchingSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success("매칭 설정이 변경되었습니다"));
    }

    @Operation(summary = "알림 설정 조회", description = "알림 설정을 조회합니다")
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getNotificationSettings(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        NotificationSettingsResponse response = settingsService.getNotificationSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "알림 설정 변경", description = "알림 설정을 변경합니다")
    @PutMapping("/notifications")
    public ResponseEntity<ApiResponse<Void>> updateNotificationSettings(
            Authentication authentication,
            @RequestBody NotificationSettingsRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        settingsService.updateNotificationSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success("알림 설정이 변경되었습니다"));
    }
}
