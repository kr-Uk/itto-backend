package com.itto.backend.presentation.api;

import com.itto.backend.presentation.dto.signup.*;
import com.itto.backend.application.signup.SignupService;
import com.itto.backend.presentation.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Signup", description = "회원가입 API")
@RestController
@RequestMapping("/api/v1/signup")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    @Operation(summary = "약관 동의 저장", description = "필수/선택 약관 동의 정보를 저장합니다")
    @PostMapping("/terms")
    public ResponseEntity<ApiResponse<Void>> saveTerms(
            Authentication authentication,
            @Valid @RequestBody TermsRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        signupService.saveTerms(userId, request);
        return ResponseEntity.ok(ApiResponse.success("약관 동의가 저장되었습니다"));
    }

    @Operation(summary = "기본 정보 저장", description = "닉네임, 성별, 생년월일, 취미, 한줄소개를 저장합니다")
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> saveProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        signupService.saveProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("프로필이 저장되었습니다"));
    }

    @Operation(summary = "프로필 사진 업로드", description = "프로필 사진을 최대 3장 업로드합니다")
    @PostMapping("/photos")
    public ResponseEntity<ApiResponse<List<PhotoResponse>>> uploadPhotos(
            Authentication authentication,
            @RequestParam("photos") List<MultipartFile> photos,
            @RequestParam(value = "representativeIndex", defaultValue = "0") int representativeIndex) {
        Long userId = (Long) authentication.getPrincipal();
        List<PhotoResponse> response = signupService.uploadPhotos(userId, photos, representativeIndex);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "얼굴 인증 셀카 업로드", description = "얼굴 인증을 위한 셀카 사진을 업로드합니다")
    @PostMapping("/verification")
    public ResponseEntity<ApiResponse<VerificationResponse>> uploadVerification(
            Authentication authentication,
            @RequestParam("selfie") MultipartFile selfie) {
        Long userId = (Long) authentication.getPrincipal();
        VerificationResponse response = signupService.uploadVerification(userId, selfie);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "회원가입 완료", description = "회원가입 프로세스를 완료 처리합니다")
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<VerificationResponse>> completeSignup(
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        VerificationResponse response = signupService.completeSignup(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
