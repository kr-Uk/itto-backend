package com.itto.backend.domain.profile.controller;

import com.itto.backend.domain.profile.dto.*;
import com.itto.backend.domain.profile.service.ProfileService;
import com.itto.backend.domain.signup.dto.VerificationResponse;
import com.itto.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Profile", description = "프로필 API")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "내 프로필 조회", description = "로그인한 유저의 프로필 정보를 반환합니다")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        MyProfileResponse response = profileService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 프로필 수정", description = "닉네임, 취미, 한줄소개를 수정합니다")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        profileService.updateMyProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다"));
    }

    @Operation(summary = "프로필 사진 교체/순서 변경", description = "기존 사진 유지 + 새 사진 업로드, 순서 및 대표 사진 변경")
    @PutMapping("/photos")
    public ResponseEntity<ApiResponse<ProfilePhotosResponse>> updatePhotos(
            Authentication authentication,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            @RequestParam(value = "existingPhotoIds", required = false) List<Long> existingPhotoIds,
            @RequestParam(value = "representativeIndex", defaultValue = "0") int representativeIndex) {
        Long userId = (Long) authentication.getPrincipal();
        ProfilePhotosResponse response = profileService.updatePhotos(userId, photos, existingPhotoIds, representativeIndex);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "프로필 사진 삭제", description = "특정 사진을 삭제합니다 (최소 1장 유지)")
    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            Authentication authentication,
            @PathVariable Long photoId) {
        Long userId = (Long) authentication.getPrincipal();
        profileService.deletePhoto(userId, photoId);
        return ResponseEntity.ok(ApiResponse.success("사진이 삭제되었습니다"));
    }

    @Operation(summary = "프로필 재인증 요청", description = "새 셀카를 업로드하여 재인증을 요청합니다")
    @PostMapping("/reverify")
    public ResponseEntity<ApiResponse<VerificationResponse>> reverify(
            Authentication authentication,
            @RequestParam("selfie") MultipartFile selfie) {
        Long userId = (Long) authentication.getPrincipal();
        profileService.reverify(userId, selfie);
        return ResponseEntity.ok(ApiResponse.success(
                VerificationResponse.builder()
                        .verificationStatus("PENDING")
                        .message("재인증 요청이 접수되었습니다")
                        .build()));
    }

    @Operation(summary = "상대 프로필 조회", description = "관계 상태에 따라 공개 범위가 달라집니다")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<OtherProfileResponse>> getOtherProfile(
            Authentication authentication,
            @PathVariable Long userId) {
        Long myUserId = (Long) authentication.getPrincipal();
        OtherProfileResponse response = profileService.getOtherProfile(myUserId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
