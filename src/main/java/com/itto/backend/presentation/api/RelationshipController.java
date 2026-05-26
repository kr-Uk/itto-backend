package com.itto.backend.presentation.api;

import com.itto.backend.presentation.dto.relationship.*;
import com.itto.backend.application.relationship.RelationshipService;
import com.itto.backend.presentation.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Relationship", description = "관계 API")
@RestController
@RequestMapping("/api/v1/relationships")
@RequiredArgsConstructor
public class RelationshipController {

    private final RelationshipService relationshipService;

    @Operation(summary = "관계 목록 조회", description = "상태별 관계 목록을 페이지네이션으로 조회합니다")
    @GetMapping
    public ResponseEntity<ApiResponse<RelationshipListResponse>> getRelationships(
            Authentication authentication,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) authentication.getPrincipal();
        RelationshipListResponse response = relationshipService.getRelationships(userId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "관계 상세 조회", description = "특정 관계의 상세 정보를 조회합니다")
    @GetMapping("/{relationshipId}")
    public ResponseEntity<ApiResponse<RelationshipDetailResponse>> getRelationshipDetail(
            Authentication authentication,
            @PathVariable Long relationshipId) {
        Long userId = (Long) authentication.getPrincipal();
        RelationshipDetailResponse response = relationshipService.getRelationshipDetail(userId, relationshipId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "인연 신청", description = "필연 상태인 관계에 인연 신청을 보냅니다")
    @PostMapping("/{relationshipId}/request")
    public ResponseEntity<ApiResponse<InyeonRequestResponse>> requestInyeon(
            Authentication authentication,
            @PathVariable Long relationshipId) {
        Long userId = (Long) authentication.getPrincipal();
        InyeonRequestResponse response = relationshipService.requestInyeon(userId, relationshipId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "인연 신청 취소", description = "대기 중인 인연 신청을 취소합니다")
    @PostMapping("/{relationshipId}/cancel")
    public ResponseEntity<ApiResponse<InyeonCancelResponse>> cancelInyeon(
            Authentication authentication,
            @PathVariable Long relationshipId) {
        Long userId = (Long) authentication.getPrincipal();
        InyeonCancelResponse response = relationshipService.cancelInyeon(userId, relationshipId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "인연 신청 수락", description = "받은 인연 신청을 수락합니다")
    @PostMapping("/{relationshipId}/accept")
    public ResponseEntity<ApiResponse<InyeonAcceptResponse>> acceptInyeon(
            Authentication authentication,
            @PathVariable Long relationshipId) {
        Long userId = (Long) authentication.getPrincipal();
        InyeonAcceptResponse response = relationshipService.acceptInyeon(userId, relationshipId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "인연 신청 거절", description = "받은 인연 신청을 거절합니다")
    @PostMapping("/{relationshipId}/reject")
    public ResponseEntity<ApiResponse<InyeonRejectResponse>> rejectInyeon(
            Authentication authentication,
            @PathVariable Long relationshipId) {
        Long userId = (Long) authentication.getPrincipal();
        InyeonRejectResponse response = relationshipService.rejectInyeon(userId, relationshipId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "인연 삭제", description = "인연 관계를 삭제합니다")
    @PostMapping("/{relationshipId}/delete")
    public ResponseEntity<ApiResponse<RelationshipDeleteResponse>> deleteRelationship(
            Authentication authentication,
            @PathVariable Long relationshipId) {
        Long userId = (Long) authentication.getPrincipal();
        RelationshipDeleteResponse response = relationshipService.deleteRelationship(userId, relationshipId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "복원 요청", description = "지난인연에 복원 요청을 보냅니다")
    @PostMapping("/{relationshipId}/restore")
    public ResponseEntity<ApiResponse<RestoreRequestResponse>> requestRestore(
            Authentication authentication,
            @PathVariable Long relationshipId) {
        Long userId = (Long) authentication.getPrincipal();
        RestoreRequestResponse response = relationshipService.requestRestore(userId, relationshipId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "복원 수락", description = "복원 요청을 수락합니다")
    @PostMapping("/{relationshipId}/restore/accept")
    public ResponseEntity<ApiResponse<RestoreAcceptResponse>> acceptRestore(
            Authentication authentication,
            @PathVariable Long relationshipId) {
        Long userId = (Long) authentication.getPrincipal();
        RestoreAcceptResponse response = relationshipService.acceptRestore(userId, relationshipId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "복원 거절", description = "복원 요청을 거절합니다")
    @PostMapping("/{relationshipId}/restore/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRestore(
            Authentication authentication,
            @PathVariable Long relationshipId) {
        Long userId = (Long) authentication.getPrincipal();
        relationshipService.rejectRestore(userId, relationshipId);
        return ResponseEntity.ok(ApiResponse.success("복원이 거절되었습니다. 관계가 종료되었습니다"));
    }

    @Operation(summary = "조우 장소 조회", description = "2번째 조우 장소를 조회합니다 (인연 상태만)")
    @GetMapping("/{relationshipId}/encounter-location")
    public ResponseEntity<ApiResponse<EncounterLocationResponse>> getEncounterLocation(
            Authentication authentication,
            @PathVariable Long relationshipId) {
        Long userId = (Long) authentication.getPrincipal();
        EncounterLocationResponse response = relationshipService.getEncounterLocation(userId, relationshipId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "만남 H3 헥사곤 heatmap",
            description = "사용자 위치 주변 H3 셀별로 본인이 마주친 횟수를 집계 (정확한 위치 노출 없이 동네 단위 시각화)")
    @GetMapping("/encounter-heatmap")
    public ResponseEntity<ApiResponse<EncounterHeatmapResponse>> getEncounterHeatmap(
            Authentication authentication,
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng) {
        Long userId = (Long) authentication.getPrincipal();
        EncounterHeatmapResponse response = relationshipService.getEncounterHeatmap(userId, lat, lng);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "우연→필연 전환", description = "우연 관계를 매듭을 사용하여 필연으로 전환합니다")
    @PostMapping("/{relationshipId}/convert")
    public ResponseEntity<ApiResponse<ConvertResponse>> convertToPilyeon(
            Authentication authentication,
            @PathVariable Long relationshipId) {
        Long userId = (Long) authentication.getPrincipal();
        ConvertResponse response = relationshipService.convertToPilyeon(userId, relationshipId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
