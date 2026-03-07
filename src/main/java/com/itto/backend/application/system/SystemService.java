package com.itto.backend.application.system;

import com.itto.backend.domain.hobby.entity.Hobby;
import com.itto.backend.domain.hobby.repository.HobbyRepository;
import com.itto.backend.presentation.dto.system.AppVersionResponse;
import com.itto.backend.presentation.dto.system.HobbyResponse;
import com.itto.backend.presentation.dto.system.NicknameValidateRequest;
import com.itto.backend.presentation.dto.system.NicknameValidateResponse;
import com.itto.backend.domain.system.entity.AppVersion;
import com.itto.backend.domain.system.repository.AppVersionRepository;
import com.itto.backend.domain.system.repository.ProfanityWordRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.common.enums.Platform;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemService {

    private final HobbyRepository hobbyRepository;
    private final ProfanityWordRepository profanityWordRepository;
    private final AppVersionRepository appVersionRepository;
    private final UserRepository userRepository;

    public AppVersionResponse getAppVersion(String platform) {
        Platform p = Platform.valueOf(platform.toUpperCase());
        AppVersion version = appVersionRepository.findByPlatform(p)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        return AppVersionResponse.builder()
                .minVersion(version.getMinVersion())
                .latestVersion(version.getLatestVersion())
                .forceUpdate(version.getForceUpdate())
                .updateUrl(version.getUpdateUrl())
                .build();
    }

    public List<HobbyResponse> getHobbies() {
        return hobbyRepository.findByIsActiveTrue().stream()
                .map(hobby -> HobbyResponse.builder()
                        .id(hobby.getHobbyId())
                        .name(hobby.getName())
                        .category(hobby.getCategory())
                        .build())
                .toList();
    }

    public NicknameValidateResponse validateNickname(NicknameValidateRequest request) {
        String nickname = request.getNickname().trim();

        // 길이 체크 (2~8자)
        if (nickname.length() < 2) {
            return NicknameValidateResponse.builder()
                    .valid(false)
                    .message("닉네임은 2자 이상이어야 합니다")
                    .code("NICKNAME_TOO_SHORT")
                    .build();
        }

        if (nickname.length() > 8) {
            return NicknameValidateResponse.builder()
                    .valid(false)
                    .message("닉네임은 8자 이하여야 합니다")
                    .code("NICKNAME_TOO_LONG")
                    .build();
        }

        // 욕설 체크
        List<String> profanityWords = profanityWordRepository.findByIsActiveTrue().stream()
                .map(pw -> pw.getWord())
                .toList();

        for (String word : profanityWords) {
            if (nickname.contains(word)) {
                return NicknameValidateResponse.builder()
                        .valid(false)
                        .message("부적절한 단어가 포함되어 있습니다")
                        .code("NICKNAME_PROFANITY")
                        .build();
            }
        }

        // 중복 체크
        if (userRepository.existsByNickname(nickname)) {
            return NicknameValidateResponse.builder()
                    .valid(false)
                    .message("이미 사용 중인 닉네임입니다")
                    .code("NICKNAME_DUPLICATE")
                    .build();
        }

        return NicknameValidateResponse.builder()
                .valid(true)
                .message("사용 가능한 닉네임입니다")
                .build();
    }
}
