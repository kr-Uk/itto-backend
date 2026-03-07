package com.itto.backend.application.location;

import com.itto.backend.presentation.dto.location.*;
import com.itto.backend.domain.location.entity.ExcludedArea;
import com.itto.backend.domain.location.entity.ExcludedAreaH3Index;
import com.itto.backend.domain.location.repository.ExcludedAreaH3IndexRepository;
import com.itto.backend.domain.location.repository.ExcludedAreaRepository;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.common.constants.BusinessConstants;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import com.uber.h3core.H3Core;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uber.h3core.util.LatLng;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class LocationService {

    public static final int H3_RESOLUTION = BusinessConstants.H3_RESOLUTION;
    private static final String LOCATION_USER_PREFIX = BusinessConstants.LOCATION_USER_PREFIX;
    private static final String LOCATION_H3_PREFIX = BusinessConstants.LOCATION_H3_PREFIX;
    private static final Duration LOCATION_TTL = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final ExcludedAreaRepository excludedAreaRepository;
    private final ExcludedAreaH3IndexRepository excludedAreaH3IndexRepository;
    private final StringRedisTemplate redisTemplate;
    private final H3Core h3;

    public LocationService(UserRepository userRepository,
                          ExcludedAreaRepository excludedAreaRepository,
                          ExcludedAreaH3IndexRepository excludedAreaH3IndexRepository,
                          StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.excludedAreaRepository = excludedAreaRepository;
        this.excludedAreaH3IndexRepository = excludedAreaH3IndexRepository;
        this.redisTemplate = redisTemplate;
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

        // Redis에 위치 데이터 저장 (배치 스침 판정에서 활용)
        saveLocationToRedis(userId, serverH3Index, request.getLatitude(), request.getLongitude());
    }

    /**
     * Redis에 사용자 위치 저장
     * - location:user:{userId} → Hash {h3Index, lat, lng}
     * - location:h3:{h3Index} → Set<userId>
     */
    private void saveLocationToRedis(Long userId, String h3Index, double lat, double lng) {
        String userKey = LOCATION_USER_PREFIX + userId;
        String h3Key = LOCATION_H3_PREFIX + h3Index;

        // 이전 H3 셀에서 제거 (셀이 바뀐 경우)
        String prevH3 = (String) redisTemplate.opsForHash().get(userKey, "h3Index");
        if (prevH3 != null && !prevH3.equals(h3Index)) {
            redisTemplate.opsForSet().remove(LOCATION_H3_PREFIX + prevH3, userId.toString());
        }

        // 사용자 위치 Hash 저장
        redisTemplate.opsForHash().put(userKey, "h3Index", h3Index);
        redisTemplate.opsForHash().put(userKey, "lat", String.valueOf(lat));
        redisTemplate.opsForHash().put(userKey, "lng", String.valueOf(lng));
        redisTemplate.expire(userKey, LOCATION_TTL);

        // H3 셀별 사용자 Set에 추가
        redisTemplate.opsForSet().add(h3Key, userId.toString());
        redisTemplate.expire(h3Key, LOCATION_TTL);
    }

    /**
     * Redis에서 사용자 위치 삭제 (위치 추적 OFF 시)
     */
    public void removeLocationFromRedis(Long userId) {
        String userKey = LOCATION_USER_PREFIX + userId;
        String prevH3 = (String) redisTemplate.opsForHash().get(userKey, "h3Index");
        if (prevH3 != null) {
            redisTemplate.opsForSet().remove(LOCATION_H3_PREFIX + prevH3, userId.toString());
        }
        redisTemplate.delete(userKey);
    }

    /**
     * 인연 만나기 토글
     */
    @Transactional
    public LocationToggleResponse toggleLocation(Long userId, LocationToggleRequest request) {
        User user = getUserOrThrow(userId);

        user.updateLocationEnabled(request.getEnabled());

        // 위치 추적 OFF 시 Redis에서 위치 삭제
        if (!request.getEnabled()) {
            removeLocationFromRedis(userId);
        }

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
        if (count >= BusinessConstants.MAX_EXCLUDED_AREAS) {
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

    /**
     * H3 그리드 조회 - 주어진 좌표 주변의 H3 셀 그리드와 사용자의 기존 선택 셀 반환
     */
    public H3GridResponse getH3Grid(Long userId, H3GridRequest request) {
        getUserOrThrow(userId);

        int k = request.getK() != null ? request.getK() : 4;
        String centerH3 = h3.latLngToCellAddress(request.getLatitude(), request.getLongitude(), H3_RESOLUTION);
        List<String> diskIndexes = new ArrayList<>(h3.gridDisk(centerH3, k));

        List<H3CellInfo> cells = diskIndexes.stream()
                .map(h3Index -> {
                    LatLng center = h3.cellToLatLng(h3Index);
                    List<LatLng> boundary = h3.cellToBoundary(h3Index);
                    List<List<Double>> boundaryList = boundary.stream()
                            .map(latLng -> List.of(latLng.lat, latLng.lng))
                            .collect(Collectors.toList());

                    return H3CellInfo.builder()
                            .h3Index(h3Index)
                            .center(H3CellInfo.H3Center.builder()
                                    .latitude(center.lat)
                                    .longitude(center.lng)
                                    .build())
                            .boundary(boundaryList)
                            .build();
                })
                .collect(Collectors.toList());

        // 사용자의 기존 제외 셀 목록 조회
        List<ExcludedArea> excludedAreas = excludedAreaRepository.findByUserUserId(userId);
        List<String> selectedIndexes = excludedAreas.stream()
                .flatMap(area -> excludedAreaH3IndexRepository.findByExcludedAreaId(area.getId()).stream())
                .map(ExcludedAreaH3Index::getH3Index)
                .distinct()
                .collect(Collectors.toList());

        return H3GridResponse.builder()
                .cells(cells)
                .selectedIndexes(selectedIndexes)
                .build();
    }

    /**
     * 제외 셀 저장 - 기존 제외 데이터 삭제 후 새로 저장
     */
    @Transactional
    public ExcludedCellsResponse saveExcludedCells(Long userId, ExcludedCellsSaveRequest request) {
        User user = getUserOrThrow(userId);

        List<String> h3Indexes = request.getH3Indexes() != null ? request.getH3Indexes() : List.of();

        if (h3Indexes.size() > BusinessConstants.MAX_EXCLUDED_CELLS) {
            throw new BusinessException(ErrorCode.EXCLUDED_CELLS_LIMIT);
        }

        // 기존 제외 지역 + H3 인덱스 전체 삭제
        List<ExcludedArea> existingAreas = excludedAreaRepository.findByUserUserId(userId);
        for (ExcludedArea area : existingAreas) {
            excludedAreaH3IndexRepository.deleteByExcludedAreaId(area.getId());
        }
        excludedAreaRepository.deleteAll(existingAreas);

        // 빈 목록이면 전체 해제
        if (h3Indexes.isEmpty()) {
            log.info("User {} cleared all excluded cells", userId);
            return ExcludedCellsResponse.of(List.of());
        }

        // 첫 번째 셀 중심 좌표로 ExcludedArea 생성
        LatLng firstCenter = h3.cellToLatLng(h3Indexes.getFirst());
        ExcludedArea excludedArea = ExcludedArea.builder()
                .user(user)
                .name("직접선택")
                .latitude(firstCenter.lat)
                .longitude(firstCenter.lng)
                .radius(0)
                .build();
        excludedArea = excludedAreaRepository.save(excludedArea);

        // H3 인덱스 저장
        ExcludedArea finalExcludedArea = excludedArea;
        List<ExcludedAreaH3Index> h3IndexEntities = h3Indexes.stream()
                .map(h3Index -> ExcludedAreaH3Index.builder()
                        .excludedArea(finalExcludedArea)
                        .h3Index(h3Index)
                        .build())
                .collect(Collectors.toList());
        excludedAreaH3IndexRepository.saveAll(h3IndexEntities);

        log.info("User {} saved {} excluded cells", userId, h3Indexes.size());
        return ExcludedCellsResponse.of(h3Indexes);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
