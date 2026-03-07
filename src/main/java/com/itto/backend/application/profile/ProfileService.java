package com.itto.backend.application.profile;

import com.itto.backend.domain.hobby.entity.Hobby;
import com.itto.backend.domain.hobby.entity.UserHobby;
import com.itto.backend.domain.hobby.repository.HobbyRepository;
import com.itto.backend.domain.hobby.repository.UserHobbyRepository;
import com.itto.backend.presentation.dto.profile.*;
import com.itto.backend.domain.relationship.entity.Relationship;
import com.itto.backend.domain.relationship.repository.RelationshipRepository;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.entity.UserPhoto;
import com.itto.backend.domain.user.entity.Verification;
import com.itto.backend.domain.user.repository.UserPhotoRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.user.repository.VerificationRepository;
import com.itto.backend.domain.common.enums.RelationshipStatus;
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
public class ProfileService {

    private final UserRepository userRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final HobbyRepository hobbyRepository;
    private final VerificationRepository verificationRepository;
    private final RelationshipRepository relationshipRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long userId) {
        User user = findUser(userId);

        List<UserHobby> userHobbies = userHobbyRepository.findByUserUserId(userId);
        List<UserPhoto> userPhotos = userPhotoRepository.findByUserUserIdOrderByDisplayOrderAsc(userId);

        int age = user.getBirthDate() != null
                ? Period.between(user.getBirthDate(), LocalDate.now()).getYears()
                : 0;

        return MyProfileResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .birthDate(user.getBirthDate())
                .age(age)
                .hobbies(userHobbies.stream()
                        .map(uh -> MyProfileResponse.HobbyDto.builder()
                                .id(uh.getHobby().getHobbyId())
                                .name(uh.getHobby().getName())
                                .build())
                        .toList())
                .introduction(user.getIntroduction())
                .photos(userPhotos.stream()
                        .map(p -> MyProfileResponse.PhotoDto.builder()
                                .id(p.getPhotoId())
                                .url(p.getPhotoUrl())
                                .isRepresentative(p.getIsRepresentative())
                                .build())
                        .toList())
                .verificationStatus(user.getVerificationStatus().name())
                .knots(user.getKnots())
                .isPremium(user.getIsPremium())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public void updateMyProfile(Long userId, ProfileUpdateRequest request) {
        User user = findUser(userId);

        // 닉네임 변경
        if (request.getNickname() != null && userRepository.existsByNicknameAndUserIdNot(request.getNickname(), userId)) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }
        if (request.getNickname() != null) {
            user.updateProfile(
                    request.getNickname(),
                    user.getGender(),
                    user.getBirthDate(),
                    request.getIntroduction() != null ? request.getIntroduction() : user.getIntroduction()
            );
        } else if (request.getIntroduction() != null) {
            user.updateProfile(
                    user.getNickname(),
                    user.getGender(),
                    user.getBirthDate(),
                    request.getIntroduction()
            );
        }

        // 취미 변경
        if (request.getHobbyIds() != null) {
            userHobbyRepository.deleteByUserUserId(userId);
            for (Long hobbyId : request.getHobbyIds()) {
                Hobby hobby = hobbyRepository.findById(hobbyId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.HOBBY_NOT_FOUND));
                userHobbyRepository.save(UserHobby.builder().user(user).hobby(hobby).build());
            }
        }
    }

    @Transactional
    public ProfilePhotosResponse updatePhotos(Long userId, List<MultipartFile> newPhotos,
                                               List<Long> existingPhotoIds, int representativeIndex) {
        User user = findUser(userId);

        List<ProfilePhotosResponse.PhotoItem> result = new ArrayList<>();
        int order = 0;

        // 1. 기존 사진 유지 (순서대로)
        if (existingPhotoIds != null) {
            for (Long photoId : existingPhotoIds) {
                UserPhoto photo = userPhotoRepository.findById(photoId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_NOT_FOUND));
                photo.updateOrder(order);
                photo.updateRepresentative(order == representativeIndex);
                result.add(toPhotoItem(photo));
                order++;
            }
        }

        // 2. 삭제 대상: 기존 사진 중 existingPhotoIds에 없는 것
        List<UserPhoto> allPhotos = userPhotoRepository.findByUserUserIdOrderByDisplayOrderAsc(userId);
        for (UserPhoto photo : allPhotos) {
            if (existingPhotoIds == null || !existingPhotoIds.contains(photo.getPhotoId())) {
                storageService.deleteFile(photo.getPhotoUrl());
                userPhotoRepository.delete(photo);
            }
        }

        // 3. 새 사진 업로드
        if (newPhotos != null) {
            for (MultipartFile file : newPhotos) {
                String url = storageService.uploadFile(file, "photos/" + userId);
                UserPhoto newPhoto = UserPhoto.builder()
                        .user(user)
                        .photoUrl(url)
                        .displayOrder(order)
                        .isRepresentative(order == representativeIndex)
                        .build();
                UserPhoto saved = userPhotoRepository.save(newPhoto);
                result.add(toPhotoItem(saved));
                order++;
            }
        }

        // 4. 총 사진 수 검증
        if (result.isEmpty()) {
            throw new BusinessException(ErrorCode.MIN_PHOTO_REQUIRED);
        }
        if (result.size() > 3) {
            throw new BusinessException(ErrorCode.PHOTO_MAX);
        }

        return ProfilePhotosResponse.builder().photos(result).build();
    }

    @Transactional
    public void deletePhoto(Long userId, Long photoId) {
        // 최소 1장 체크
        long count = userPhotoRepository.countByUserUserId(userId);
        if (count <= 1) {
            throw new BusinessException(ErrorCode.MIN_PHOTO_REQUIRED);
        }

        UserPhoto photo = userPhotoRepository.findById(photoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_NOT_FOUND));

        if (!photo.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        storageService.deleteFile(photo.getPhotoUrl());
        userPhotoRepository.delete(photo);
    }

    @Transactional
    public void reverify(Long userId, MultipartFile selfie) {
        User user = findUser(userId);

        String selfieUrl = storageService.uploadFile(selfie, "verification/" + userId);

        Verification verification = Verification.builder()
                .user(user)
                .selfieUrl(selfieUrl)
                .build();
        verificationRepository.save(verification);

        user.updateVerificationStatus(VerificationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public OtherProfileResponse getOtherProfile(Long myUserId, Long targetUserId) {
        User target = findUser(targetUserId);

        // 관계 조회
        Long smaller = Math.min(myUserId, targetUserId);
        Long larger = Math.max(myUserId, targetUserId);
        Relationship relationship = relationshipRepository
                .findByUser1UserIdAndUser2UserId(smaller, larger)
                .orElseThrow(() -> new BusinessException(ErrorCode.RELATIONSHIP_NOT_FOUND));

        RelationshipStatus status = relationship.getStatus();

        List<UserHobby> userHobbies = userHobbyRepository.findByUserUserId(targetUserId);
        List<UserPhoto> userPhotos = userPhotoRepository.findByUserUserIdOrderByDisplayOrderAsc(targetUserId);

        int age = target.getBirthDate() != null
                ? Period.between(target.getBirthDate(), LocalDate.now()).getYears()
                : 0;

        // 관계 상태에 따른 공개 범위
        return switch (status) {
            case UYEON -> buildOtherProfile(target, status, age, userHobbies, userPhotos,
                    true, true, relationship.getEncounterCount());
            case PILYEON, PENDING -> buildOtherProfile(target, status, age, userHobbies, userPhotos,
                    false, true, relationship.getEncounterCount());
            case INYEON -> buildOtherProfile(target, status, age, userHobbies, userPhotos,
                    false, true, relationship.getEncounterCount());
            case PAST -> buildPastProfile(target, status, userPhotos);
            case ENDED -> throw new BusinessException(ErrorCode.RELATIONSHIP_NOT_FOUND);
        };
    }

    private OtherProfileResponse buildOtherProfile(User target, RelationshipStatus status,
                                                     int age, List<UserHobby> hobbies,
                                                     List<UserPhoto> photos,
                                                     boolean mosaic, boolean showDetails,
                                                     int encounterCount) {
        return OtherProfileResponse.builder()
                .userId(target.getUserId())
                .nickname(target.getNickname())
                .relationshipStatus(status.name())
                .gender(target.getGender() != null ? target.getGender().name() : null)
                .age(age)
                .hobbies(showDetails ? hobbies.stream()
                        .map(uh -> OtherProfileResponse.HobbyDto.builder()
                                .id(uh.getHobby().getHobbyId())
                                .name(uh.getHobby().getName())
                                .build())
                        .toList() : null)
                .introduction(showDetails ? target.getIntroduction() : null)
                .photos(photos.stream()
                        .map(p -> OtherProfileResponse.PhotoDto.builder()
                                .url(p.getPhotoUrl())
                                .isMosaic(mosaic)
                                .isRepresentative(p.getIsRepresentative())
                                .build())
                        .toList())
                .encounterCount(encounterCount)
                .build();
    }

    private OtherProfileResponse buildPastProfile(User target, RelationshipStatus status,
                                                    List<UserPhoto> photos) {
        // 지난인연: 대표사진 1장 + 닉네임만
        UserPhoto representative = photos.stream()
                .filter(UserPhoto::getIsRepresentative)
                .findFirst()
                .orElse(photos.isEmpty() ? null : photos.get(0));

        List<OtherProfileResponse.PhotoDto> photoList = representative != null
                ? List.of(OtherProfileResponse.PhotoDto.builder()
                        .url(representative.getPhotoUrl())
                        .isMosaic(false)
                        .isRepresentative(true)
                        .build())
                : List.of();

        return OtherProfileResponse.builder()
                .userId(target.getUserId())
                .nickname(target.getNickname())
                .relationshipStatus(status.name())
                .photos(photoList)
                .build();
    }

    private ProfilePhotosResponse.PhotoItem toPhotoItem(UserPhoto photo) {
        return ProfilePhotosResponse.PhotoItem.builder()
                .id(photo.getPhotoId())
                .url(photo.getPhotoUrl())
                .isRepresentative(photo.getIsRepresentative())
                .build();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
