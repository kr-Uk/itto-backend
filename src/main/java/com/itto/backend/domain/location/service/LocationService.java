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
import com.uber.h3core.H3Core;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class LocationService {

    private static final int H3_RESOLUTION = 9; // ~100m hex edge, ~105,332 m² area

    private final UserRepository userRepository;
    private final ExcludedAreaRepository excludedAreaRepository;
    private final ExcludedAreaH3IndexRepository excludedAreaH3IndexRepository;
    private final H3Core h3;

    public LocationService(UserRepository userRepository,
                          ExcludedAreaRepository excludedAreaRepository,
                          ExcludedAreaH3IndexRepository excludedAreaH3IndexRepository) {
        this.userRepository = userRepository;
        this.excludedAreaRepository = excludedAreaRepository;
        this.excludedAreaH3IndexRepository = excludedAreaH3IndexRepository;
        try {
            this.h3 = H3Core.newInstance();
        } catch (IOException e) {
            log.error("Failed to initialize H3Core", e);
            throw new IllegalStateException("H3 라이브러리 초기화 실패", e);
        }
    }

    /**
     * 위치 데이터 전송
     * 현재는 로그만 남기고, 추후 매칭 로직에서 활용
     */
    public void sendLocation(Long userId, LocationRequest request) {
        User user = getUserOrThrow(userId);

        // 서버 측에서 H3 인덱스 계산 (클라이언트 검증용 + 향후 매칭 로직 활용)
        String serverH3Index = h3.latLngToCellAddress(
                request.getLatitude(),
                request.getLongitude(),
                H3_RESOLUTION
        );

        log.info("User {} sent location: clientH3Index={}, serverH3Index={}, lat={}, lng={}, accuracy={}, timestamp={}",
                userId, request.getH3Index(), serverH3Index, request.getLatitude(),
                request.getLongitude(), request.getAccuracy(), request.getTimestamp());

        // TODO: 추후 실시간 매칭 로직에서 활용 (제외 지역 H3 인덱스와 비교)
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

        // H3 인덱스 계산 및 저장
        List<String> h3Indexes = calculateH3Indexes(request.getLatitude(), request.getLongitude(), request.getRadius());

        // H3 인덱스를 DB에 저장
        ExcludedArea finalExcludedArea = excludedArea;
        List<ExcludedAreaH3Index> h3IndexEntities = h3Indexes.stream()
                .map(h3Index -> ExcludedAreaH3Index.builder()
                        .excludedArea(finalExcludedArea)
                        .h3Index(h3Index)
                        .build())
                .collect(Collectors.toList());

        excludedAreaH3IndexRepository.saveAll(h3IndexEntities);

        log.info("User {} created excluded area: id={}, name={}, lat={}, lng={}, radius={}, h3IndexCount={}",
                userId, excludedArea.getId(), request.getName(), request.getLatitude(),
                request.getLongitude(), request.getRadius(), h3Indexes.size());

        return ExcludedAreaCreateResponse.from(excludedArea, h3Indexes);
    }

    /**
     * 주어진 위도, 경도, 반경(미터)을 기반으로 H3 인덱스 목록을 계산
     *
     * @param latitude 중심점 위도
     * @param longitude 중심점 경도
     * @param radiusMeters 반경(미터)
     * @return H3 인덱스 목록
     */
    private List<String> calculateH3Indexes(double latitude, double longitude, int radiusMeters) {
        // 중심점의 H3 인덱스 계산
        String centerH3Index = h3.latLngToCellAddress(latitude, longitude, H3_RESOLUTION);

        // 반경에 따른 k 값 계산 (resolution 9의 hex edge는 약 174m)
        // k = 반경 / 100 (대략적인 계산, 100m 단위로 링 추가)
        int k = Math.max(1, radiusMeters / 100);

        // gridDisk를 사용하여 k-ring 내의 모든 H3 인덱스 계산
        List<String> diskIndexes = new ArrayList<>(h3.gridDisk(centerH3Index, k));

        log.debug("Calculated H3 indexes: center={}, radius={}m, k={}, indexCount={}",
                centerH3Index, radiusMeters, k, diskIndexes.size());

        return diskIndexes;
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
