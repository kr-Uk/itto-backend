package com.itto.backend.presentation.api;

import com.itto.backend.application.settings.SettingsService;
import com.itto.backend.presentation.dto.common.ApiResponse;
import com.itto.backend.presentation.dto.settings.MatchingSettingsRequest;
import com.itto.backend.presentation.dto.settings.MatchingSettingsResponse;
import com.itto.backend.presentation.dto.settings.NotificationSettingsRequest;
import com.itto.backend.presentation.dto.settings.NotificationSettingsResponse;
import com.itto.backend.presentation.dto.settings.SettingsResponse;
import com.itto.backend.presentation.dto.settings.SettingsUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Settings", description = "설정 API")
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @Operation(summary = "전체 설정 조회", description = "매칭 설정과 알림 설정을 함께 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<SettingsResponse>> getSettings(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SettingsResponse response = settingsService.getSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "전체 설정 수정", description = "매칭 설정과 알림 설정을 함께 수정합니다.")
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateSettings(
            Authentication authentication,
            @Valid @RequestBody SettingsUpdateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        settingsService.updateSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success("설정이 변경되었습니다"));
    }

    @Operation(summary = "매칭 설정 조회", description = "성별 필터와 나이 범위를 조회합니다.")
    @GetMapping("/matching")
    public ResponseEntity<ApiResponse<MatchingSettingsResponse>> getMatchingSettings(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        MatchingSettingsResponse response = settingsService.getMatchingSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "매칭 설정 수정", description = "성별 필터와 나이 범위를 수정합니다.")
    @PutMapping("/matching")
    public ResponseEntity<ApiResponse<Void>> updateMatchingSettings(
            Authentication authentication,
            @Valid @RequestBody MatchingSettingsRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        settingsService.updateMatchingSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success("매칭 설정이 변경되었습니다"));
    }

    @Operation(summary = "알림 설정 조회", description = "알림 설정을 조회합니다.")
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getNotificationSettings(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        NotificationSettingsResponse response = settingsService.getNotificationSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "알림 설정 수정", description = "알림 설정을 수정합니다.")
    @PutMapping("/notifications")
    public ResponseEntity<ApiResponse<Void>> updateNotificationSettings(
            Authentication authentication,
            @Valid @RequestBody NotificationSettingsRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        settingsService.updateNotificationSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success("알림 설정이 변경되었습니다"));
    }
}
