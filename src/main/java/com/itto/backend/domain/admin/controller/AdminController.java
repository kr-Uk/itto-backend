package com.itto.backend.domain.admin.controller;

import com.itto.backend.domain.admin.dto.RejectRequest;
import com.itto.backend.domain.admin.dto.VerificationListResponse;
import com.itto.backend.domain.admin.service.AdminService;
import com.itto.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "승인 대기 목록 조회", description = "PENDING 상태의 사용자 목록을 페이징하여 조회")
    @GetMapping("/verifications")
    public ResponseEntity<ApiResponse<VerificationListResponse>> getPendingVerifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        VerificationListResponse response = adminService.getPendingVerifications(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "인증 승인", description = "사용자의 인증을 승인하고 APPROVED 상태로 변경")
    @PostMapping("/verifications/{verificationId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveVerification(
            @PathVariable Long verificationId) {
        adminService.approveVerification(verificationId);
        return ResponseEntity.ok(ApiResponse.success("승인되었습니다"));
    }

    @Operation(summary = "인증 거절", description = "사용자의 인증을 거절하고 REJECTED 상태로 변경")
    @PostMapping("/verifications/{verificationId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectVerification(
            @PathVariable Long verificationId,
            @Valid @RequestBody RejectRequest request) {
        adminService.rejectVerification(verificationId, request);
        return ResponseEntity.ok(ApiResponse.success("거절되었습니다"));
    }
}
