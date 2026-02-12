package com.itto.backend.domain.settings.service;

import com.itto.backend.domain.settings.dto.*;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.entity.UserSetting;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.user.repository.UserSettingRepository;
import com.itto.backend.global.enums.Gender;
import com.itto.backend.global.exception.BusinessException;
import com.itto.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MatchingSettingsResponse getMatchingSettings(Long userId) {
        UserSetting setting = getOrCreateSetting(userId);

        return MatchingSettingsResponse.builder()
                .genderFilter(setting.getGenderFilter() != null ? setting.getGenderFilter().name() : null)
                .ageRangeMin(setting.getAgeRangeMin())
                .ageRangeMax(setting.getAgeRangeMax())
                .build();
    }

    @Transactional
    public void updateMatchingSettings(Long userId, MatchingSettingsRequest request) {
        UserSetting setting = getOrCreateSetting(userId);

        Gender genderFilter = request.getGenderFilter() != null
                ? Gender.valueOf(request.getGenderFilter().toUpperCase())
                : null;

        setting.updateMatching(genderFilter, request.getAgeRangeMin(), request.getAgeRangeMax());
    }

    @Transactional(readOnly = true)
    public NotificationSettingsResponse getNotificationSettings(Long userId) {
        UserSetting setting = getOrCreateSetting(userId);

        return NotificationSettingsResponse.builder()
                .chatNotification(setting.getChatNotification())
                .relationshipNotification(setting.getRelationshipNotification())
                .statsNotification(setting.getStatsNotification())
                .marketingNotification(setting.getMarketingNotification())
                .build();
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

    private UserSetting getOrCreateSetting(Long userId) {
        return userSettingRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    return userSettingRepository.save(UserSetting.builder().user(user).build());
                });
    }
}
