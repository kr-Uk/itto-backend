package com.itto.backend.domain.system.controller;

import com.itto.backend.domain.system.dto.AppVersionResponse;
import com.itto.backend.domain.system.dto.HobbyResponse;
import com.itto.backend.domain.system.dto.NicknameValidateRequest;
import com.itto.backend.domain.system.dto.NicknameValidateResponse;
import com.itto.backend.domain.system.service.SystemService;
import com.itto.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "System", description = "시스템 API")
@RestController
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @Operation(summary = "취미 목록 조회", description = "활성화된 취미 목록을 반환합니다")
    @GetMapping("/api/hobbies")
    public ResponseEntity<ApiResponse<List<HobbyResponse>>> getHobbies() {
        List<HobbyResponse> hobbies = systemService.getHobbies();
        return ResponseEntity.ok(ApiResponse.success(hobbies));
    }

    @Operation(summary = "닉네임 검증", description = "닉네임 사용 가능 여부를 확인합니다")
    @PostMapping("/api/nickname/validate")
    public ResponseEntity<ApiResponse<NicknameValidateResponse>> validateNickname(
            @Valid @RequestBody NicknameValidateRequest request) {
        NicknameValidateResponse response = systemService.validateNickname(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "앱 버전 조회", description = "플랫폼별 최신 버전 정보를 반환합니다")
    @GetMapping("/api/system/version")
    public ResponseEntity<ApiResponse<AppVersionResponse>> getAppVersion(
            @RequestParam String platform) {
        AppVersionResponse response = systemService.getAppVersion(platform);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
