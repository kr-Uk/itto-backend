package com.itto.backend.application.signup;

import com.itto.backend.domain.hobby.entity.Hobby;
import com.itto.backend.domain.hobby.entity.UserHobby;
import com.itto.backend.domain.hobby.repository.HobbyRepository;
import com.itto.backend.domain.hobby.repository.UserHobbyRepository;
import com.itto.backend.presentation.dto.signup.*;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.entity.UserPhoto;
import com.itto.backend.domain.user.entity.UserSetting;
import com.itto.backend.domain.user.entity.UserTerm;
import com.itto.backend.domain.user.entity.Verification;
import com.itto.backend.domain.user.repository.*;
import com.itto.backend.domain.common.constants.BusinessConstants;
import com.itto.backend.domain.common.enums.Gender;
import com.itto.backend.domain.common.enums.VerificationStatus;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import com.itto.backend.infrastructure.minio.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final UserTermRepository userTermRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final UserSettingRepository userSettingRepository;
    private final VerificationRepository verificationRepository;
    private final HobbyRepository hobbyRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final StorageService storageService;

    @Transactional
    public void saveTerms(Long userId, TermsRequest request) {
        User user = findUser(userId);

        UserTerm userTerm = UserTerm.builder()
                .user(user)
                .serviceTerms(request.isServiceTerms())
                .privacyPolicy(request.isPrivacyPolicy())
                .locationTerms(request.isLocationTerms())
                .marketingConsent(request.isMarketingConsent())
                .termsVersion("1.0")
                .agreedAt(java.time.LocalDateTime.now())
                .build();

        userTermRepository.save(userTerm);
    }

    @Transactional
    public void saveProfile(Long userId, ProfileRequest request) {
        User user = findUser(userId);

        // 닉네임 중복 체크 (본인 제외)
        if (userRepository.existsByNicknameAndUserIdNot(request.getNickname(), userId)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }

        // 나이 제한 체크 (만 19세 이상)
        if (Period.between(request.getBirthDate(), LocalDate.now()).getYears() < BusinessConstants.MIN_AGE) {
            throw new BusinessException(ErrorCode.AGE_RESTRICTION);
        }

        // 프로필 업데이트
        Gender gender = Gender.valueOf(request.getGender().toUpperCase());
        user.updateProfile(request.getNickname(), gender, request.getBirthDate(), request.getIntroduction());

        // 기존 취미 삭제 후 재등록
        userHobbyRepository.deleteByUserUserId(userId);

        for (Long hobbyId : request.getHobbyIds()) {
            Hobby hobby = hobbyRepository.findById(hobbyId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.HOBBY_NOT_FOUND));

            UserHobby userHobby = UserHobby.builder()
                    .user(user)
                    .hobby(hobby)
                    .build();
            userHobbyRepository.save(userHobby);
        }
    }

    @Transactional
    public List<PhotoResponse> uploadPhotos(Long userId, List<MultipartFile> photos, int representativeIndex) {
        User user = findUser(userId);

        if (photos == null || photos.isEmpty()) {
            throw new BusinessException(ErrorCode.PHOTO_MIN);
        }
        if (photos.size() > BusinessConstants.MAX_PHOTOS) {
            throw new BusinessException(ErrorCode.PHOTO_MAX);
        }

        // 기존 사진 삭제
        List<UserPhoto> existingPhotos = userPhotoRepository.findByUserUserIdOrderByDisplayOrderAsc(userId);
        for (UserPhoto photo : existingPhotos) {
            storageService.deleteFile(photo.getPhotoUrl());
        }
        if (!existingPhotos.isEmpty()) {
            userPhotoRepository.deleteAll(existingPhotos);
        }

        // 새 사진 업로드 및 저장
        List<PhotoResponse> responses = new ArrayList<>();
        for (int i = 0; i < photos.size(); i++) {
            String url = storageService.uploadFile(photos.get(i), "photos/" + userId);

            UserPhoto userPhoto = UserPhoto.builder()
                    .user(user)
                    .photoUrl(url)
                    .displayOrder(i)
                    .isRepresentative(i == representativeIndex)
                    .build();

            UserPhoto saved = userPhotoRepository.save(userPhoto);
            responses.add(PhotoResponse.builder()
                    .id(saved.getPhotoId())
                    .url(saved.getPhotoUrl())
                    .isRepresentative(saved.getIsRepresentative())
                    .build());
        }

        return responses;
    }

    @Transactional
    public VerificationResponse uploadVerification(Long userId, MultipartFile selfie) {
        User user = findUser(userId);

        // 셀카 업로드
        String selfieUrl = storageService.uploadFile(selfie, "verification/" + userId);

        // 인증 요청 생성
        Verification verification = Verification.builder()
                .user(user)
                .selfieUrl(selfieUrl)
                .build();
        verificationRepository.save(verification);

        // 유저 인증 상태 변경
        user.updateVerificationStatus(VerificationStatus.PENDING);

        return VerificationResponse.builder()
                .verificationStatus("PENDING")
                .message("인증 요청이 접수되었습니다")
                .build();
    }

    @Transactional
    public VerificationResponse completeSignup(Long userId) {
        User user = findUser(userId);

        // 프로필 완성 여부 체크
        if (!user.isProfileComplete()) {
            throw new BusinessException(ErrorCode.PROFILE_INCOMPLETE);
        }

        // 약관 동의 여부 체크
        if (userTermRepository.findTopByUserUserIdOrderByCreatedAtDesc(userId).isEmpty()) {
            throw new BusinessException(ErrorCode.TERMS_NOT_AGREED);
        }

        // 기본 설정 생성 (없는 경우)
        if (userSettingRepository.findByUserUserId(userId).isEmpty()) {
            UserSetting setting = UserSetting.builder()
                    .user(user)
                    .build();
            userSettingRepository.save(setting);
        }

        return VerificationResponse.builder()
                .verificationStatus(user.getVerificationStatus().name())
                .message("회원가입이 완료되었습니다")
                .build();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
