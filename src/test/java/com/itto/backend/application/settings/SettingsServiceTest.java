package com.itto.backend.application.settings;

import com.itto.backend.domain.common.enums.Gender;
import com.itto.backend.domain.common.enums.Provider;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.entity.UserSetting;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.user.repository.UserSettingRepository;
import com.itto.backend.presentation.dto.settings.MatchingSettingsRequest;
import com.itto.backend.presentation.dto.settings.MatchingSettingsResponse;
import com.itto.backend.presentation.dto.settings.NotificationSettingsRequest;
import com.itto.backend.presentation.dto.settings.NotificationSettingsResponse;
import com.itto.backend.presentation.dto.settings.SettingsResponse;
import com.itto.backend.presentation.dto.settings.SettingsUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettingsService")
class SettingsServiceTest {

    @Mock
    private UserSettingRepository userSettingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SettingsService settingsService;

    private User testUser;
    private UserSetting testSetting;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .providerUid("test-uid")
                .provider(Provider.GOOGLE)
                .build();

        testSetting = UserSetting.builder()
                .user(testUser)
                .genderFilter(Gender.FEMALE)
                .ageRangeMin(25)
                .ageRangeMax(35)
                .build();
    }

    @Nested
    @DisplayName("getSettings")
    class GetSettingsTest {

        @Test
        @DisplayName("전체 설정을 조회한다")
        void getSettingsSuccess() {
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            SettingsResponse response = settingsService.getSettings(userId);

            assertThat(response.getMatchingSettings().getGenderFilter()).isEqualTo("FEMALE");
            assertThat(response.getMatchingSettings().getAgeRangeMin()).isEqualTo(25);
            assertThat(response.getMatchingSettings().getAgeRangeMax()).isEqualTo(35);
            assertThat(response.getNotificationSettings().isChatNotification()).isTrue();
            assertThat(response.getNotificationSettings().isRelationshipNotification()).isTrue();
            assertThat(response.getNotificationSettings().isStatsNotification()).isTrue();
            assertThat(response.getNotificationSettings().isMarketingNotification()).isFalse();
        }
    }

    @Nested
    @DisplayName("getMatchingSettings")
    class GetMatchingSettingsTest {

        @Test
        @DisplayName("기존 설정을 조회한다")
        void getMatchingSettingsExisting() {
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            MatchingSettingsResponse response = settingsService.getMatchingSettings(userId);

            assertThat(response.getGenderFilter()).isEqualTo("FEMALE");
            assertThat(response.getAgeRangeMin()).isEqualTo(25);
            assertThat(response.getAgeRangeMax()).isEqualTo(35);
        }

        @Test
        @DisplayName("설정이 없으면 기본 설정을 생성한다")
        void getMatchingSettingsCreateNew() {
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            UserSetting defaultSetting = UserSetting.builder().user(testUser).build();
            when(userSettingRepository.save(any(UserSetting.class))).thenReturn(defaultSetting);

            MatchingSettingsResponse response = settingsService.getMatchingSettings(userId);

            verify(userSettingRepository).save(any(UserSetting.class));
            assertThat(response.getAgeRangeMin()).isEqualTo(20);
            assertThat(response.getAgeRangeMax()).isEqualTo(60);
        }

        @Test
        @DisplayName("사용자가 없으면 예외를 던진다")
        void getMatchingSettingsUserNotFound() {
            Long userId = 999L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> settingsService.getMatchingSettings(userId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("updateMatchingSettings")
    class UpdateMatchingSettingsTest {

        @Test
        @DisplayName("매칭 설정을 수정한다")
        void updateMatchingSettingsSuccess() {
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            MatchingSettingsRequest request = createMatchingSettingsRequest("MALE", 20, 40);

            settingsService.updateMatchingSettings(userId, request);

            assertThat(testSetting.getGenderFilter()).isEqualTo(Gender.MALE);
            assertThat(testSetting.getAgeRangeMin()).isEqualTo(20);
            assertThat(testSetting.getAgeRangeMax()).isEqualTo(40);
        }

        @Test
        @DisplayName("genderFilter를 null로 설정할 수 있다")
        void updateMatchingSettingsNullGender() {
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            MatchingSettingsRequest request = createMatchingSettingsRequest(null, 19, 50);

            settingsService.updateMatchingSettings(userId, request);

            assertThat(testSetting.getGenderFilter()).isNull();
        }

        @Test
        @DisplayName("잘못된 genderFilter면 예외를 던진다")
        void updateMatchingSettingsInvalidGender() {
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            MatchingSettingsRequest request = createMatchingSettingsRequest("UNKNOWN", 20, 40);

            assertThatThrownBy(() -> settingsService.updateMatchingSettings(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("최소 나이가 최대 나이보다 크면 예외를 던진다")
        void updateMatchingSettingsInvalidAgeRange() {
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            MatchingSettingsRequest request = createMatchingSettingsRequest("MALE", 40, 20);

            assertThatThrownBy(() -> settingsService.updateMatchingSettings(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.BAD_REQUEST));
        }
    }

    @Nested
    @DisplayName("getNotificationSettings")
    class GetNotificationSettingsTest {

        @Test
        @DisplayName("알림 설정을 조회한다")
        void getNotificationSettingsSuccess() {
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            NotificationSettingsResponse response = settingsService.getNotificationSettings(userId);

            assertThat(response.isChatNotification()).isTrue();
            assertThat(response.isRelationshipNotification()).isTrue();
            assertThat(response.isStatsNotification()).isTrue();
            assertThat(response.isMarketingNotification()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateSettings")
    class UpdateSettingsTest {

        @Test
        @DisplayName("전체 설정을 수정한다")
        void updateSettingsSuccess() {
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            SettingsUpdateRequest request = new SettingsUpdateRequest();
            setField(request, "matchingSettings", createMatchingSettingsRequest("MALE", 20, 40));
            setField(request, "notificationSettings", createNotificationSettingsRequest(false, true, false, true));

            settingsService.updateSettings(userId, request);

            assertThat(testSetting.getGenderFilter()).isEqualTo(Gender.MALE);
            assertThat(testSetting.getAgeRangeMin()).isEqualTo(20);
            assertThat(testSetting.getAgeRangeMax()).isEqualTo(40);
            assertThat(testSetting.getChatNotification()).isFalse();
            assertThat(testSetting.getRelationshipNotification()).isTrue();
            assertThat(testSetting.getStatsNotification()).isFalse();
            assertThat(testSetting.getMarketingNotification()).isTrue();
        }
    }

    @Nested
    @DisplayName("updateNotificationSettings")
    class UpdateNotificationSettingsTest {

        @Test
        @DisplayName("알림 설정을 수정한다")
        void updateNotificationSettingsSuccess() {
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            NotificationSettingsRequest request = createNotificationSettingsRequest(false, true, false, true);

            settingsService.updateNotificationSettings(userId, request);

            assertThat(testSetting.getChatNotification()).isFalse();
            assertThat(testSetting.getRelationshipNotification()).isTrue();
            assertThat(testSetting.getStatsNotification()).isFalse();
            assertThat(testSetting.getMarketingNotification()).isTrue();
        }
    }

    private MatchingSettingsRequest createMatchingSettingsRequest(String genderFilter, int ageMin, int ageMax) {
        MatchingSettingsRequest request = new MatchingSettingsRequest();
        setField(request, "genderFilter", genderFilter);
        setField(request, "ageRangeMin", ageMin);
        setField(request, "ageRangeMax", ageMax);
        return request;
    }

    private NotificationSettingsRequest createNotificationSettingsRequest(
            boolean chat, boolean relationship, boolean stats, boolean marketing) {
        NotificationSettingsRequest request = new NotificationSettingsRequest();
        setField(request, "chatNotification", chat);
        setField(request, "relationshipNotification", relationship);
        setField(request, "statsNotification", stats);
        setField(request, "marketingNotification", marketing);
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
