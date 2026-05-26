package com.itto.backend.application.profile;

import com.itto.backend.domain.common.enums.Gender;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import com.itto.backend.domain.hobby.entity.Hobby;
import com.itto.backend.domain.hobby.entity.UserHobby;
import com.itto.backend.domain.hobby.repository.HobbyRepository;
import com.itto.backend.domain.hobby.repository.UserHobbyRepository;
import com.itto.backend.domain.relationship.repository.RelationshipRepository;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.entity.UserPhoto;
import com.itto.backend.domain.user.repository.UserPhotoRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.user.repository.VerificationRepository;
import com.itto.backend.infrastructure.minio.StorageService;
import com.itto.backend.presentation.dto.profile.MyProfileResponse;
import com.itto.backend.presentation.dto.profile.ProfileUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService")
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPhotoRepository userPhotoRepository;

    @Mock
    private UserHobbyRepository userHobbyRepository;

    @Mock
    private HobbyRepository hobbyRepository;

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private RelationshipRepository relationshipRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ProfileService profileService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .providerUid("test-uid")
                .provider(com.itto.backend.domain.common.enums.Provider.GOOGLE)
                .nickname("testNick")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(1995, 6, 15))
                .introduction("hello")
                .build();
    }

    @Nested
    @DisplayName("getMyProfile")
    class GetMyProfileTest {

        @Test
        @DisplayName("내 프로필 조회 성공")
        void getMyProfileSuccess() {
            // given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            Hobby hobby = Hobby.builder().name("running").build();
            UserHobby userHobby = UserHobby.builder().user(testUser).hobby(hobby).build();
            when(userHobbyRepository.findByUserUserId(userId)).thenReturn(List.of(userHobby));

            UserPhoto photo = UserPhoto.builder()
                    .user(testUser)
                    .photoUrl("http://photo.jpg")
                    .isRepresentative(true)
                    .build();
            when(userPhotoRepository.findByUserUserIdOrderByDisplayOrderAsc(userId))
                    .thenReturn(List.of(photo));

            // when
            MyProfileResponse response = profileService.getMyProfile(userId);

            // then
            assertThat(response.getNickname()).isEqualTo("testNick");
            assertThat(response.getGender()).isEqualTo("MALE");
            assertThat(response.getAge()).isGreaterThan(0);
            assertThat(response.getHobbies()).hasSize(1);
            assertThat(response.getPhotos()).hasSize(1);
            assertThat(response.getIntroduction()).isEqualTo("hello");
        }

        @Test
        @DisplayName("존재하지 않는 유저 프로필 조회 → BusinessException")
        void getMyProfileUserNotFound() {
            // given
            Long userId = 999L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> profileService.getMyProfile(userId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("생년월일 없는 경우 나이 0")
        void getMyProfileNoBirthDate() {
            // given
            Long userId = 1L;
            User noBirthUser = User.builder()
                    .providerUid("test-uid")
                    .provider(com.itto.backend.domain.common.enums.Provider.GOOGLE)
                    .nickname("noBirth")
                    .gender(Gender.FEMALE)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(noBirthUser));
            when(userHobbyRepository.findByUserUserId(userId)).thenReturn(List.of());
            when(userPhotoRepository.findByUserUserIdOrderByDisplayOrderAsc(userId)).thenReturn(List.of());

            // when
            MyProfileResponse response = profileService.getMyProfile(userId);

            // then
            assertThat(response.getAge()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("updateMyProfile")
    class UpdateMyProfileTest {

        @Test
        @DisplayName("닉네임 변경 성공")
        void updateNicknameSuccess() {
            // given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByNicknameAndUserIdNot("newNick", userId)).thenReturn(false);

            ProfileUpdateRequest request = createProfileUpdateRequest("newNick", null, null);

            // when
            profileService.updateMyProfile(userId, request);

            // then
            assertThat(testUser.getNickname()).isEqualTo("newNick");
        }

        @Test
        @DisplayName("중복 닉네임 변경 → BusinessException")
        void updateDuplicateNickname() {
            // given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByNicknameAndUserIdNot("dupNick", userId)).thenReturn(true);

            ProfileUpdateRequest request = createProfileUpdateRequest("dupNick", null, null);

            // when & then
            assertThatThrownBy(() -> profileService.updateMyProfile(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.NICKNAME_DUPLICATE));
        }

        @Test
        @DisplayName("취미 변경 성공")
        void updateHobbiesSuccess() {
            // given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            Hobby hobby1 = Hobby.builder().name("running").build();
            Hobby hobby2 = Hobby.builder().name("reading").build();
            when(hobbyRepository.findById(10L)).thenReturn(Optional.of(hobby1));
            when(hobbyRepository.findById(20L)).thenReturn(Optional.of(hobby2));

            ProfileUpdateRequest request = createProfileUpdateRequest(null, List.of(10L, 20L), null);

            // when
            profileService.updateMyProfile(userId, request);

            // then
            verify(userHobbyRepository).deleteByUserUserId(userId);
            verify(userHobbyRepository, times(2)).save(any(UserHobby.class));
        }

        @Test
        @DisplayName("존재하지 않는 취미 ID → BusinessException")
        void updateHobbiesNotFound() {
            // given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(hobbyRepository.findById(999L)).thenReturn(Optional.empty());

            ProfileUpdateRequest request = createProfileUpdateRequest(null, List.of(999L), null);

            // when & then
            assertThatThrownBy(() -> profileService.updateMyProfile(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.HOBBY_NOT_FOUND));
        }

        @Test
        @DisplayName("한줄소개만 변경 성공")
        void updateIntroductionOnly() {
            // given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            ProfileUpdateRequest request = createProfileUpdateRequest(null, null, "new intro");

            // when
            profileService.updateMyProfile(userId, request);

            // then
            assertThat(testUser.getIntroduction()).isEqualTo("new intro");
        }
    }

    @Nested
    @DisplayName("deletePhoto")
    class DeletePhotoTest {

        @Test
        @DisplayName("사진 삭제 성공")
        void deletePhotoSuccess() {
            // given
            Long userId = 1L;
            Long photoId = 10L;

            UserPhoto photo = UserPhoto.builder()
                    .user(testUser)
                    .photoUrl("http://photo.jpg")
                    .isRepresentative(false)
                    .build();
            when(userPhotoRepository.findById(photoId)).thenReturn(Optional.of(photo));

            // when
            profileService.deletePhoto(userId, photoId);

            // then
            verify(storageService).deleteFile("http://photo.jpg");
            verify(userPhotoRepository).delete(photo);
        }

        @Test
        @DisplayName("존재하지 않는 사진 삭제 → BusinessException")
        void deletePhotoNotFound() {
            // given
            Long userId = 1L;
            Long photoId = 999L;
            when(userPhotoRepository.findById(photoId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> profileService.deletePhoto(userId, photoId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.PHOTO_NOT_FOUND));
        }
    }

    // --- Helper methods ---

    private ProfileUpdateRequest createProfileUpdateRequest(String nickname, List<Long> hobbyIds, String introduction) {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        if (nickname != null) setField(request, "nickname", nickname);
        if (hobbyIds != null) setField(request, "hobbyIds", hobbyIds);
        if (introduction != null) setField(request, "introduction", introduction);
        return request;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
