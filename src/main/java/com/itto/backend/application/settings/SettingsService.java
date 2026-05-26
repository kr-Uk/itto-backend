package com.itto.backend.application.settings;

import com.itto.backend.presentation.dto.settings.*;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.entity.UserSetting;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.user.repository.UserSettingRepository;
import com.itto.backend.domain.common.enums.Gender;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;

    @Transactional
    public SettingsResponse getSettings(Long userId) {
        UserSetting setting = getOrCreateSetting(userId);

        return SettingsResponse.builder()
                .matchingSettings(toMatchingSettingsResponse(setting))
                .notificationSettings(toNotificationSettingsResponse(setting))
                .build();
    }

    @Transactional
    public void updateSettings(Long userId, SettingsUpdateRequest request) {
        updateMatchingSettings(userId, request.getMatchingSettings());
        updateNotificationSettings(userId, request.getNotificationSettings());
    }

    @Transactional
    public MatchingSettingsResponse getMatchingSettings(Long userId) {
        UserSetting setting = getOrCreateSetting(userId);
        return toMatchingSettingsResponse(setting);
    }

    @Transactional
    public void updateMatchingSettings(Long userId, MatchingSettingsRequest request) {
        UserSetting setting = getOrCreateSetting(userId);
        validateMatchingAgeRange(request);
        Gender genderFilter = parseGenderFilter(request.getGenderFilter());

        setting.updateMatching(genderFilter, request.getAgeRangeMin(), request.getAgeRangeMax());
    }

    @Transactional
    public NotificationSettingsResponse getNotificationSettings(Long userId) {
        UserSetting setting = getOrCreateSetting(userId);
        return toNotificationSettingsResponse(setting);
    }

    @Transactional
    public void updateNotificationSettings(Long userId, NotificationSettingsRequest request) {
        UserSetting setting = getOrCreateSetting(userId);

        setting.updateNotifications(
                request.isChatNotification(),
                request.isRelationshipNotification(),
                request.isStatsNotification(),
                request.isMarketingNotification()
        );
    }

    private void validateMatchingAgeRange(MatchingSettingsRequest request) {
        if (request.getAgeRangeMin() > request.getAgeRangeMax()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "최소 나이는 최대 나이보다 클 수 없습니다");
        }
    }

    private Gender parseGenderFilter(String genderFilter) {
        if (genderFilter == null || genderFilter.isBlank()) {
            return null;
        }

        try {
            return Gender.valueOf(genderFilter.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "genderFilter는 MALE 또는 FEMALE이어야 합니다");
        }
    }

    private MatchingSettingsResponse toMatchingSettingsResponse(UserSetting setting) {
        return MatchingSettingsResponse.builder()
                .genderFilter(setting.getGenderFilter() != null ? setting.getGenderFilter().name() : null)
                .ageRangeMin(setting.getAgeRangeMin())
                .ageRangeMax(setting.getAgeRangeMax())
                .build();
    }

    private NotificationSettingsResponse toNotificationSettingsResponse(UserSetting setting) {
        return NotificationSettingsResponse.builder()
                .chatNotification(setting.getChatNotification())
                .relationshipNotification(setting.getRelationshipNotification())
                .statsNotification(setting.getStatsNotification())
                .marketingNotification(setting.getMarketingNotification())
                .build();
    }

    private UserSetting getOrCreateSetting(Long userId) {
        return userSettingRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    return userSettingRepository.save(UserSetting.builder().user(user).build());
                });
    }
}
