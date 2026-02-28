package com.itto.backend.domain.admin.service;

import com.itto.backend.domain.admin.dto.RejectRequest;
import com.itto.backend.domain.admin.dto.VerificationListResponse;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.entity.UserPhoto;
import com.itto.backend.domain.user.repository.UserPhotoRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.global.enums.Gender;
import com.itto.backend.global.enums.Provider;
import com.itto.backend.global.enums.VerificationStatus;
import com.itto.backend.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPhotoRepository userPhotoRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("승인 대기 목록 조회 성공")
    void getPendingVerifications_Success() {
        // given
        User user1 = User.builder()
                .userId(1L)
                .providerUid("firebase1")
                .provider(Provider.KAKAO)
                .nickname("유저1")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        User user2 = User.builder()
                .userId(2L)
                .providerUid("firebase2")
                .provider(Provider.KAKAO)
                .nickname("유저2")
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(1995, 5, 15))
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        Page<User> pendingUsers = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 20), 2);

        UserPhoto photo1 = UserPhoto.builder()
                .photoId(1L)
                .user(user1)
                .photoUrl("https://example.com/photo1.jpg")
                .displayOrder(0)
                .isRepresentative(true)
                .build();

        UserPhoto photo2 = UserPhoto.builder()
                .photoId(2L)
                .user(user2)
                .photoUrl("https://example.com/photo2.jpg")
                .displayOrder(0)
                .isRepresentative(true)
                .build();

        given(userRepository.findByVerificationStatus(eq(VerificationStatus.PENDING), any(Pageable.class)))
                .willReturn(pendingUsers);
        given(userPhotoRepository.findByUserUserIdOrderByDisplayOrderAsc(1L))
                .willReturn(List.of(photo1));
        given(userPhotoRepository.findByUserUserIdOrderByDisplayOrderAsc(2L))
                .willReturn(List.of(photo2));

        // when
        VerificationListResponse response = adminService.getPendingVerifications(0, 20);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);

        VerificationListResponse.VerificationInfo info1 = response.getContent().get(0);
        assertThat(info1.getUserId()).isEqualTo(1L);
        assertThat(info1.getNickname()).isEqualTo("유저1");
        assertThat(info1.getProfilePhotos()).hasSize(1);
        assertThat(info1.getProfilePhotos().get(0)).isEqualTo("https://example.com/photo1.jpg");
    }

    @Test
    @DisplayName("인증 승인 성공")
    void approveVerification_Success() {
        // given
        User user = User.builder()
                .userId(1L)
                .providerUid("firebase1")
                .provider(Provider.KAKAO)
                .nickname("유저1")
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        adminService.approveVerification(1L);

        // then
        verify(userRepository).findById(1L);
        assertThat(user.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
    }

    @Test
    @DisplayName("인증 승인 실패 - 사용자 없음")
    void approveVerification_Fail_UserNotFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.approveVerification(999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("인증 거절 성공")
    void rejectVerification_Success() {
        // given
        User user = User.builder()
                .userId(1L)
                .providerUid("firebase1")
                .provider(Provider.KAKAO)
                .nickname("유저1")
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        RejectRequest request = new RejectRequest();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        adminService.rejectVerification(1L, request);

        // then
        verify(userRepository).findById(1L);
        assertThat(user.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    @DisplayName("인증 거절 실패 - 사용자 없음")
    void rejectVerification_Fail_UserNotFound() {
        // given
        RejectRequest request = new RejectRequest();
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.rejectVerification(999L, request))
                .isInstanceOf(BusinessException.class);
    }
}
