package com.itto.backend.domain.block.controller;

import com.itto.backend.domain.block.dto.BlockListResponse;
import com.itto.backend.domain.block.dto.BlockRequest;
import com.itto.backend.domain.block.dto.ReportRequest;
import com.itto.backend.domain.block.dto.ReportResponse;
import com.itto.backend.domain.block.service.BlockService;
import com.itto.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Block & Report", description = "차단 및 신고 API")
public class BlockController {

    private final BlockService blockService;

    @GetMapping("/blocks")
    @Operation(summary = "차단 목록 조회", description = "현재 사용자가 차단한 사용자 목록을 조회합니다")
    public ResponseEntity<ApiResponse<BlockListResponse>> getBlocks(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        BlockListResponse response = blockService.getBlocks(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/blocks")
    @Operation(summary = "사용자 차단", description = "특정 사용자를 차단합니다")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            Authentication authentication,
            @Valid @RequestBody BlockRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        blockService.blockUser(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("사용자를 차단했습니다"));
    }

    @DeleteMapping("/blocks/{blockId}")
    @Operation(summary = "차단 해제", description = "특정 차단을 해제합니다")
    public ResponseEntity<ApiResponse<Void>> unblock(
            Authentication authentication,
            @PathVariable Long blockId) {
        Long userId = (Long) authentication.getPrincipal();
        blockService.unblock(userId, blockId);
        return ResponseEntity.ok(ApiResponse.success("차단이 해제되었습니다"));
    }

    @PostMapping("/reports")
    @Operation(summary = "신고 등록", description = "사용자, 프로필, 또는 채팅을 신고합니다")
    public ResponseEntity<ApiResponse<ReportResponse>> report(
            Authentication authentication,
            @Valid @RequestBody ReportRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        ReportResponse response = blockService.report(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("신고가 접수되었습니다", response));
    }
}
