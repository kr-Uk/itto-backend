package com.itto.backend.domain.admin.service;

import com.itto.backend.domain.admin.dto.RejectRequest;
import com.itto.backend.domain.admin.dto.VerificationListResponse;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.entity.UserPhoto;
import com.itto.backend.domain.user.repository.UserPhotoRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.global.enums.VerificationStatus;
import com.itto.backend.global.exception.BusinessException;
import com.itto.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final UserPhotoRepository userPhotoRepository;

    public VerificationListResponse getPendingVerifications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<User> pendingUsers = userRepository.findByVerificationStatus(VerificationStatus.PENDING, pageable);

        List<VerificationListResponse.VerificationInfo> content = pendingUsers.getContent().stream()
                .map(user -> {
                    List<String> profilePhotos = userPhotoRepository.findByUserUserIdOrderByDisplayOrderAsc(user.getUserId())
                            .stream()
                            .map(UserPhoto::getPhotoUrl)
                            .toList();

                    return VerificationListResponse.VerificationInfo.builder()
                            .id(user.getUserId())
                            .userId(user.getUserId())
                            .nickname(user.getNickname())
                            .profilePhotos(profilePhotos)
                            .selfieUrl(null) // 셀카 URL은 현재 구조에서 별도 저장되지 않음
                            .requestedAt(user.getCreatedAt())
                            .build();
                })
                .toList();

        return VerificationListResponse.builder()
                .content(content)
                .page(pendingUsers.getNumber())
                .size(pendingUsers.getSize())
                .totalElements(pendingUsers.getTotalElements())
                .totalPages(pendingUsers.getTotalPages())
                .build();
    }

    @Transactional
    public void approveVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updateVerificationStatus(VerificationStatus.APPROVED);
    }

    @Transactional
    public void rejectVerification(Long userId, RejectRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 거절 사유는 로깅만 하고 별도 저장하지 않음 (요구사항에 저장 명시 없음)
        user.updateVerificationStatus(VerificationStatus.REJECTED);
    }
}
