package com.itto.backend.presentation.api;

import com.itto.backend.presentation.dto.contact.*;
import com.itto.backend.domain.contact.entity.ContactHash;
import com.itto.backend.application.contact.ContactHashService;
import com.itto.backend.presentation.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@Tag(name = "Contact Hash", description = "연락처 해시 & 지인 차단 API")
public class ContactHashController {

    private final ContactHashService contactHashService;

    @PostMapping("/hashes")
    @Operation(summary = "연락처 해시 업로드", description = "연락처 해시를 벌크 업로드합니다 (전체 교체)")
    public ResponseEntity<ApiResponse<ContactHashUploadResponse>> uploadHashes(
            Authentication authentication,
            @Valid @RequestBody ContactHashUploadRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        contactHashService.uploadHashes(userId, request.getHashes());

        ContactHashUploadResponse response = ContactHashUploadResponse.builder()
                .uploadedCount(request.getHashes().size())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("연락처 해시가 동기화되었습니다", response));
    }

    @GetMapping("/hashes")
    @Operation(summary = "연락처 해시 목록 조회", description = "현재 사용자의 연락처 해시 목록을 조회합니다")
    public ResponseEntity<ApiResponse<ContactHashListResponse>> getHashes(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<ContactHash> hashes = contactHashService.getHashes(userId);

        List<ContactHashListResponse.ContactHashInfo> hashInfos = hashes.stream()
                .map(h -> ContactHashListResponse.ContactHashInfo.builder()
                        .id(h.getId())
                        .phoneHash(h.getPhoneHash())
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();

        ContactHashListResponse response = ContactHashListResponse.builder()
                .hashes(hashInfos)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/block-by-phone")
    @Operation(summary = "직접 번호 입력 차단", description = "전화번호를 직접 입력하여 차단합니다 (서버에서 해시 처리)")
    public ResponseEntity<ApiResponse<Void>> blockByPhone(
            Authentication authentication,
            @Valid @RequestBody ManualBlockRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        contactHashService.addManualHash(userId, request.getPhoneNumber());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("번호가 차단 목록에 추가되었습니다"));
    }

    @DeleteMapping("/hashes/{hashId}")
    @Operation(summary = "연락처 해시 삭제", description = "특정 연락처 해시를 삭제합니다")
    public ResponseEntity<ApiResponse<Void>> removeHash(
            Authentication authentication,
            @PathVariable Long hashId) {
        Long userId = (Long) authentication.getPrincipal();
        contactHashService.removeHash(userId, hashId);
        return ResponseEntity.ok(ApiResponse.success("차단이 해제되었습니다"));
    }
}
