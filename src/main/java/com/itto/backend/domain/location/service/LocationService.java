package com.itto.backend.domain.location.service;

import com.itto.backend.domain.location.dto.*;
import com.itto.backend.domain.location.entity.ExcludedArea;
import com.itto.backend.domain.location.entity.ExcludedAreaH3Index;
import com.itto.backend.domain.location.repository.ExcludedAreaH3IndexRepository;
import com.itto.backend.domain.location.repository.ExcludedAreaRepository;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.global.exception.BusinessException;
import com.itto.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final UserRepository userRepository;
    private final ExcludedAreaRepository excludedAreaRepository;
    private final ExcludedAreaH3IndexRepository excludedAreaH3IndexRepository;

    /**
     * 위치 데이터 전송
     * 현재는 로그만 남기고, 추후 매칭 로직에서 활용
     */
    public void sendLocation(Long userId, LocationRequest request) {
        User user = getUserOrThrow(userId);

        // TODO: 추후 실시간 매칭 로직에서 활용
        log.info("User {} sent location: h3Index={}, lat={}, lng={}, accuracy={}, timestamp={}",
                userId, request.getH3Index(), request.getLatitude(), request.getLongitude(),
                request.getAccuracy(), request.getTimestamp());
    }

    /**
     * 인연 만나기 토글
     */
    @Transactional
    public LocationToggleResponse toggleLocation(Long userId, LocationToggleRequest request) {
        User user = getUserOrThrow(userId);

        user.updateLocationEnabled(request.getEnabled());

        log.info("User {} toggled location enabled to: {}", userId, request.getEnabled());

        return LocationToggleResponse.of(request.getEnabled());
    }

    /**
     * 제외 지역 목록 조회
     */
    public ExcludedAreaListResponse getExcludedAreas(Long userId) {
        // 사용자 존재 확인
        getUserOrThrow(userId);

        List<ExcludedArea> excludedAreas = excludedAreaRepository.findByUserUserId(userId);

        List<ExcludedAreaListResponse.ExcludedAreaInfo> excludedAreaInfos = excludedAreas.stream()
                .map(area -> {
                    List<String> h3Indexes = excludedAreaH3IndexRepository.findByExcludedAreaId(area.getId())
                            .stream()
                            .map(ExcludedAreaH3Index::getH3Index)
                            .collect(Collectors.toList());
                    return ExcludedAreaListResponse.ExcludedAreaInfo.from(area, h3Indexes);
                })
                .collect(Collectors.toList());

        return ExcludedAreaListResponse.from(excludedAreaInfos);
    }

    /**
     * 제외 지역 등록
     */
    @Transactional
    public ExcludedAreaCreateResponse createExcludedArea(Long userId, ExcludedAreaCreateRequest request) {
        User user = getUserOrThrow(userId);

        // 제외 지역 개수 제한 확인 (최대 3개)
        long count = excludedAreaRepository.countByUserUserId(userId);
        if (count >= 3) {
            throw new BusinessException(ErrorCode.EXCLUDED_AREA_LIMIT);
        }

        // 제외 지역 저장
        ExcludedArea excludedArea = ExcludedArea.builder()
                .user(user)
                .name(request.getName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .radius(request.getRadius())
                .build();

        excludedArea = excludedAreaRepository.save(excludedArea);

        // TODO: H3 라이브러리 연동 후 반경 내 H3 인덱스 계산 및 저장
        // 현재는 빈 리스트 반환
        List<String> h3Indexes = Collections.emptyList();

        log.info("User {} created excluded area: id={}, name={}, lat={}, lng={}, radius={}",
                userId, excludedArea.getId(), request.getName(), request.getLatitude(),
                request.getLongitude(), request.getRadius());

        return ExcludedAreaCreateResponse.from(excludedArea, h3Indexes);
    }

    /**
     * 제외 지역 삭제
     */
    @Transactional
    public void deleteExcludedArea(Long userId, Long excludedAreaId) {
        // 사용자 존재 확인
        getUserOrThrow(userId);

        // 제외 지역 조회 및 권한 확인
        ExcludedArea excludedArea = excludedAreaRepository.findById(excludedAreaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCLUDED_AREA_NOT_FOUND));

        // 본인 것만 삭제 가능
        if (!excludedArea.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // H3 인덱스 먼저 삭제
        excludedAreaH3IndexRepository.deleteByExcludedAreaId(excludedAreaId);

        // 제외 지역 삭제
        excludedAreaRepository.delete(excludedArea);

        log.info("User {} deleted excluded area: id={}", userId, excludedAreaId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
