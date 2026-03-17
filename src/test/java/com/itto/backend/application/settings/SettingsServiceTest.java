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
import static org.mockito.Mockito.*;

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
    @DisplayName("getMatchingSettings")
    class GetMatchingSettingsTest {

        @Test
        @DisplayName("매칭 설정 조회 성공 - 기존 설정 존재")
        void getMatchingSettingsExisting() {
            // given
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            // when
            MatchingSettingsResponse response = settingsService.getMatchingSettings(userId);

            // then
            assertThat(response.getGenderFilter()).isEqualTo("FEMALE");
            assertThat(response.getAgeRangeMin()).isEqualTo(25);
            assertThat(response.getAgeRangeMax()).isEqualTo(35);
        }

        @Test
        @DisplayName("매칭 설정 조회 - 설정 없으면 새로 생성")
        void getMatchingSettingsCreateNew() {
            // given
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            UserSetting defaultSetting = UserSetting.builder().user(testUser).build();
            when(userSettingRepository.save(any(UserSetting.class))).thenReturn(defaultSetting);

            // when
            MatchingSettingsResponse response = settingsService.getMatchingSettings(userId);

            // then
            verify(userSettingRepository).save(any(UserSetting.class));
            assertThat(response.getAgeRangeMin()).isEqualTo(20); // default
            assertThat(response.getAgeRangeMax()).isEqualTo(60); // default
        }

        @Test
        @DisplayName("설정 생성 시 유저 없음 → BusinessException")
        void getMatchingSettingsUserNotFound() {
            // given
            Long userId = 999L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // when & then
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
        @DisplayName("매칭 설정 업데이트 성공")
        void updateMatchingSettingsSuccess() {
            // given
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            MatchingSettingsRequest request = createMatchingSettingsRequest("MALE", 20, 40);

            // when
            settingsService.updateMatchingSettings(userId, request);

            // then
            assertThat(testSetting.getGenderFilter()).isEqualTo(Gender.MALE);
            assertThat(testSetting.getAgeRangeMin()).isEqualTo(20);
            assertThat(testSetting.getAgeRangeMax()).isEqualTo(40);
        }

        @Test
        @DisplayName("성별 필터 null로 설정")
        void updateMatchingSettingsNullGender() {
            // given
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            MatchingSettingsRequest request = createMatchingSettingsRequest(null, 19, 50);

            // when
            settingsService.updateMatchingSettings(userId, request);

            // then
            assertThat(testSetting.getGenderFilter()).isNull();
        }
    }

    @Nested
    @DisplayName("getNotificationSettings")
    class GetNotificationSettingsTest {

        @Test
        @DisplayName("알림 설정 조회 성공")
        void getNotificationSettingsSuccess() {
            // given
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            // when
            NotificationSettingsResponse response = settingsService.getNotificationSettings(userId);

            // then
            assertThat(response.isChatNotification()).isTrue();
            assertThat(response.isRelationshipNotification()).isTrue();
            assertThat(response.isStatsNotification()).isTrue();
            assertThat(response.isMarketingNotification()).isFalse();
        }
    }

    @Nested
    @DisplayName("updateNotificationSettings")
    class UpdateNotificationSettingsTest {

        @Test
        @DisplayName("알림 설정 업데이트 성공")
        void updateNotificationSettingsSuccess() {
            // given
            Long userId = 1L;
            when(userSettingRepository.findByUserUserId(userId)).thenReturn(Optional.of(testSetting));

            NotificationSettingsRequest request = createNotificationSettingsRequest(false, true, false, true);

            // when
            settingsService.updateNotificationSettings(userId, request);

            // then
            assertThat(testSetting.getChatNotification()).isFalse();
            assertThat(testSetting.getRelationshipNotification()).isTrue();
            assertThat(testSetting.getStatsNotification()).isFalse();
            assertThat(testSetting.getMarketingNotification()).isTrue();
        }
    }

    // --- Helper methods ---

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
