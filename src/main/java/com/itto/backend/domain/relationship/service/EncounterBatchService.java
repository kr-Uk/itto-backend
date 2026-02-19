package com.itto.backend.domain.relationship.service;

import com.itto.backend.domain.block.repository.BlockRepository;
import com.itto.backend.domain.location.repository.ExcludedAreaH3IndexRepository;
import com.itto.backend.domain.location.service.LocationService;
import com.itto.backend.domain.notification.entity.Notification;
import com.itto.backend.domain.notification.repository.NotificationRepository;
import com.itto.backend.domain.relationship.entity.Encounter;
import com.itto.backend.domain.relationship.entity.Relationship;
import com.itto.backend.domain.relationship.entity.RelationshipLog;
import com.itto.backend.domain.relationship.repository.EncounterRepository;
import com.itto.backend.domain.relationship.repository.RelationshipLogRepository;
import com.itto.backend.domain.relationship.repository.RelationshipRepository;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.entity.UserSetting;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.user.repository.UserSettingRepository;
import com.itto.backend.global.enums.NotificationType;
import com.itto.backend.global.enums.RelationshipStatus;
import com.itto.backend.global.enums.RelationshipTrigger;
import com.itto.backend.global.enums.VerificationStatus;
import com.uber.h3core.H3Core;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

@Slf4j
@Service
public class EncounterBatchService {

    private static final double ENCOUNTER_DISTANCE_METERS = 200.0;
    private static final String LOCATION_USER_PREFIX = "location:user:";
    private static final String LOCATION_H3_PREFIX = "location:h3:";
    private static final String ENCOUNTER_COOLDOWN_PREFIX = "encounter:cooldown:";
    private static final int COOLDOWN_MINUTES = 30;

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final RelationshipRepository relationshipRepository;
    private final EncounterRepository encounterRepository;
    private final RelationshipLogRepository relationshipLogRepository;
    private final BlockRepository blockRepository;
    private final ExcludedAreaH3IndexRepository excludedAreaH3IndexRepository;
    private final NotificationRepository notificationRepository;
    private final H3Core h3;

    public EncounterBatchService(StringRedisTemplate redisTemplate,
                                 UserRepository userRepository,
                                 UserSettingRepository userSettingRepository,
                                 RelationshipRepository relationshipRepository,
                                 EncounterRepository encounterRepository,
                                 RelationshipLogRepository relationshipLogRepository,
                                 BlockRepository blockRepository,
                                 ExcludedAreaH3IndexRepository excludedAreaH3IndexRepository,
                                 NotificationRepository notificationRepository) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.userSettingRepository = userSettingRepository;
        this.relationshipRepository = relationshipRepository;
        this.encounterRepository = encounterRepository;
        this.relationshipLogRepository = relationshipLogRepository;
        this.blockRepository = blockRepository;
        this.excludedAreaH3IndexRepository = excludedAreaH3IndexRepository;
        this.notificationRepository = notificationRepository;
        try {
            this.h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new IllegalStateException("H3 라이브러리 초기화 실패", e);
        }
    }

    /**
     * 스침 판정 배치 메인 로직
     * 1. Redis에서 활성 H3 셀 스캔
     * 2. 각 셀 내 사용자 페어 추출
     * 3. 필터링 (차단, 제외, 매칭설정, 기존관계)
     * 4. 거리 계산 후 스침 판정
     * 5. Relationship + Encounter 생성/갱신
     */
    @Transactional
    public int detectEncounters() {
        LocalDateTime batchWindow = LocalDateTime.now();
        int totalEncounters = 0;

        // Redis에서 location:h3:* 키를 스캔하여 활성 H3 셀 수집
        Set<String> h3Keys = scanRedisKeys(LOCATION_H3_PREFIX + "*");
        log.info("[배치] 활성 H3 셀: {}개", h3Keys.size());

        for (String h3Key : h3Keys) {
            String h3Index = h3Key.substring(LOCATION_H3_PREFIX.length());
            Set<String> userIdStrings = redisTemplate.opsForSet().members(h3Key);

            if (userIdStrings == null || userIdStrings.size() < 2) {
                continue;
            }

            // 이 H3 셀을 제외 지역으로 등록한 사용자 ID
            Set<Long> excludedUserIds = excludedAreaH3IndexRepository.findUserIdsByH3Index(h3Index);

            List<Long> userIds = userIdStrings.stream()
                    .map(Long::valueOf)
                    .filter(id -> !excludedUserIds.contains(id))
                    .toList();

            if (userIds.size() < 2) {
                continue;
            }

            // 사용자 페어별 스침 판정
            totalEncounters += processUserPairs(userIds, h3Index, batchWindow);
        }

        log.info("[배치] 스침 판정 완료: {}건", totalEncounters);
        return totalEncounters;
    }

    /**
     * 사용자 페어별 스침 처리
     */
    private int processUserPairs(List<Long> userIds, String h3Index, LocalDateTime batchWindow) {
        int encounters = 0;

        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i + 1; j < userIds.size(); j++) {
                Long userIdA = userIds.get(i);
                Long userIdB = userIds.get(j);

                try {
                    if (processEncounterPair(userIdA, userIdB, h3Index, batchWindow)) {
                        encounters++;
                    }
                } catch (Exception e) {
                    log.error("[배치] 스침 처리 실패: userA={}, userB={}", userIdA, userIdB, e);
                }
            }
        }

        return encounters;
    }

    /**
     * 두 사용자 간 스침 판정 + 처리
     */
    private boolean processEncounterPair(Long userIdA, Long userIdB, String h3Index,
                                          LocalDateTime batchWindow) {
        // 1. 쿨다운 체크
        if (isOnCooldown(userIdA, userIdB)) {
            return false;
        }

        // 2. 차단 체크
        if (blockRepository.existsBlockBetween(userIdA, userIdB)) {
            return false;
        }

        // 3. 사용자 정보 조회 + 유효성 체크
        User userA = userRepository.findById(userIdA).orElse(null);
        User userB = userRepository.findById(userIdB).orElse(null);
        if (userA == null || userB == null) return false;
        if (userA.isDeleted() || userB.isDeleted()) return false;
        if (!Boolean.TRUE.equals(userA.getLocationEnabled()) || !Boolean.TRUE.equals(userB.getLocationEnabled())) return false;
        if (userA.getVerificationStatus() != VerificationStatus.APPROVED
                || userB.getVerificationStatus() != VerificationStatus.APPROVED) return false;

        // 4. 매칭 설정 필터 (상호 체크)
        if (!matchesFilter(userA, userB) || !matchesFilter(userB, userA)) {
            return false;
        }

        // 5. 기존 관계 체크 (INYEON, PENDING 상태면 스침 불필요)
        Long user1Id = Math.min(userIdA, userIdB);
        Long user2Id = Math.max(userIdA, userIdB);
        Optional<Relationship> existingOpt = relationshipRepository.findByUser1UserIdAndUser2UserId(user1Id, user2Id);

        if (existingOpt.isPresent()) {
            Relationship existing = existingOpt.get();
            RelationshipStatus status = existing.getStatus();
            // 이미 인연/대기/필연 상태면 추가 스침 불필요
            if (status == RelationshipStatus.INYEON
                    || status == RelationshipStatus.PENDING
                    || status == RelationshipStatus.PILYEON) {
                return false;
            }
        }

        // 6. 실제 거리 계산
        double[] locA = getUserLocation(userIdA);
        double[] locB = getUserLocation(userIdB);
        if (locA == null || locB == null) return false;

        double distance = haversineDistance(locA[0], locA[1], locB[0], locB[1]);
        if (distance > ENCOUNTER_DISTANCE_METERS) {
            return false;
        }

        // 7. 스침 확정 → 처리
        User user1 = user1Id.equals(userIdA) ? userA : userB;
        User user2 = user2Id.equals(userIdA) ? userA : userB;
        double midLat = (locA[0] + locB[0]) / 2;
        double midLng = (locA[1] + locB[1]) / 2;

        if (existingOpt.isPresent()) {
            handleExistingRelationship(existingOpt.get(), user1, user2, h3Index,
                    midLat, midLng, distance, batchWindow);
        } else {
            handleNewRelationship(user1, user2, h3Index, midLat, midLng, distance, batchWindow);
        }

        // 쿨다운 설정
        setCooldown(userIdA, userIdB);
        return true;
    }

    /**
     * 신규 관계 생성 (1번째 스침 → UYEON)
     */
    private void handleNewRelationship(User user1, User user2, String h3Index,
                                        double lat, double lng, double distance,
                                        LocalDateTime batchWindow) {
        Relationship relationship = Relationship.builder()
                .user1(user1)
                .user2(user2)
                .status(RelationshipStatus.UYEON)
                .encounterCount(1)
                .firstEncounterAt(batchWindow)
                .lastEncounterAt(batchWindow)
                .statusChangedAt(batchWindow)
                .build();
        relationshipRepository.save(relationship);

        Encounter encounter = Encounter.builder()
                .relationship(relationship)
                .user1(user1)
                .user2(user2)
                .h3Index(h3Index)
                .latitude(lat)
                .longitude(lng)
                .distance(distance)
                .encounterNumber(1)
                .batchWindowAt(batchWindow)
                .build();
        encounterRepository.save(encounter);

        // 알림 생성
        createEncounterNotification(user1, relationship.getId(), NotificationType.ENCOUNTER_UYEON,
                "새로운 우연", "누군가와 스쳤어요!");
        createEncounterNotification(user2, relationship.getId(), NotificationType.ENCOUNTER_UYEON,
                "새로운 우연", "누군가와 스쳤어요!");

        log.info("[배치] 새 우연: user1={}, user2={}, distance={:.1f}m", user1.getUserId(), user2.getUserId(), distance);
    }

    /**
     * 기존 관계 업데이트 (UYEON→PILYEON 자동 전환, ENDED→UYEON 재시작)
     */
    private void handleExistingRelationship(Relationship relationship, User user1, User user2,
                                             String h3Index, double lat, double lng,
                                             double distance, LocalDateTime batchWindow) {
        RelationshipStatus prevStatus = relationship.getStatus();

        if (prevStatus == RelationshipStatus.ENDED) {
            // 종료된 관계 → 우연으로 재시작
            relationship.updateStatus(RelationshipStatus.UYEON);
            relationship.recordEncounter();

            Encounter encounter = Encounter.builder()
                    .relationship(relationship)
                    .user1(user1)
                    .user2(user2)
                    .h3Index(h3Index)
                    .latitude(lat)
                    .longitude(lng)
                    .distance(distance)
                    .encounterNumber(relationship.getEncounterCount())
                    .batchWindowAt(batchWindow)
                    .build();
            encounterRepository.save(encounter);

            saveLog(relationship, prevStatus, RelationshipStatus.UYEON, RelationshipTrigger.ENCOUNTER, null);

            createEncounterNotification(user1, relationship.getId(), NotificationType.ENCOUNTER_UYEON,
                    "새로운 우연", "누군가와 다시 스쳤어요!");
            createEncounterNotification(user2, relationship.getId(), NotificationType.ENCOUNTER_UYEON,
                    "새로운 우연", "누군가와 다시 스쳤어요!");

            log.info("[배치] 재시작 우연: rel={}", relationship.getId());
        } else if (prevStatus == RelationshipStatus.UYEON) {
            // 우연 → 필연 자동 전환 (2번째 스침)
            relationship.recordEncounter();
            LocalDateTime expiresAt = batchWindow.plusHours(24);
            relationship.updateStatusWithExpiry(RelationshipStatus.PILYEON, expiresAt);

            Encounter encounter = Encounter.builder()
                    .relationship(relationship)
                    .user1(user1)
                    .user2(user2)
                    .h3Index(h3Index)
                    .latitude(lat)
                    .longitude(lng)
                    .distance(distance)
                    .encounterNumber(relationship.getEncounterCount())
                    .batchWindowAt(batchWindow)
                    .build();
            encounterRepository.save(encounter);

            saveLog(relationship, RelationshipStatus.UYEON, RelationshipStatus.PILYEON, RelationshipTrigger.ENCOUNTER, null);

            createEncounterNotification(user1, relationship.getId(), NotificationType.ENCOUNTER_PILYEON,
                    "필연이 되었어요!", "두 번째 스침! 상대방의 프로필을 확인해보세요.");
            createEncounterNotification(user2, relationship.getId(), NotificationType.ENCOUNTER_PILYEON,
                    "필연이 되었어요!", "두 번째 스침! 상대방의 프로필을 확인해보세요.");

            log.info("[배치] 우연→필연: rel={}, expires={}", relationship.getId(), expiresAt);
        } else if (prevStatus == RelationshipStatus.PAST) {
            // PAST 상태에서 다시 스침 → 스침 카운트만 증가 (상태 변경 없음)
            relationship.recordEncounter();

            Encounter encounter = Encounter.builder()
                    .relationship(relationship)
                    .user1(user1)
                    .user2(user2)
                    .h3Index(h3Index)
                    .latitude(lat)
                    .longitude(lng)
                    .distance(distance)
                    .encounterNumber(relationship.getEncounterCount())
                    .batchWindowAt(batchWindow)
                    .build();
            encounterRepository.save(encounter);

            log.info("[배치] PAST 상태 스침 기록: rel={}", relationship.getId());
        }
    }

    /**
     * 만료된 관계 상태 전이 처리
     * - PILYEON(24h) → PAST(7d)
     * - PENDING(필연 만료) → PAST(7d)
     * - INYEON(7d) → PAST(7d)
     * - PAST(7d) → ENDED
     */
    @Transactional
    public int processExpirations() {
        LocalDateTime now = LocalDateTime.now();
        int processed = 0;

        // PILYEON 만료 → PAST
        List<Relationship> expiredPilyeon = relationshipRepository.findByStatusAndExpiresAtBefore(
                RelationshipStatus.PILYEON, now);
        for (Relationship rel : expiredPilyeon) {
            rel.updateStatusWithExpiry(RelationshipStatus.PAST, now.plusDays(7));
            saveLog(rel, RelationshipStatus.PILYEON, RelationshipStatus.PAST, RelationshipTrigger.EXPIRE, null);
            processed++;
        }

        // PENDING 만료 (필연 타이머가 만료된 경우) → PAST
        List<Relationship> expiredPending = relationshipRepository.findByStatusAndExpiresAtBefore(
                RelationshipStatus.PENDING, now);
        for (Relationship rel : expiredPending) {
            rel.updateStatusWithExpiry(RelationshipStatus.PAST, now.plusDays(7));
            saveLog(rel, RelationshipStatus.PENDING, RelationshipStatus.PAST, RelationshipTrigger.EXPIRE, null);
            processed++;
        }

        // INYEON 만료 → PAST
        List<Relationship> expiredInyeon = relationshipRepository.findByStatusAndExpiresAtBefore(
                RelationshipStatus.INYEON, now);
        for (Relationship rel : expiredInyeon) {
            rel.updateStatusWithExpiry(RelationshipStatus.PAST, now.plusDays(7));
            saveLog(rel, RelationshipStatus.INYEON, RelationshipStatus.PAST, RelationshipTrigger.EXPIRE, null);
            processed++;
        }

        // PAST 만료 → ENDED
        List<Relationship> expiredPast = relationshipRepository.findByStatusAndExpiresAtBefore(
                RelationshipStatus.PAST, now);
        for (Relationship rel : expiredPast) {
            rel.updateStatus(RelationshipStatus.ENDED);
            saveLog(rel, RelationshipStatus.PAST, RelationshipStatus.ENDED, RelationshipTrigger.EXPIRE, null);
            processed++;
        }

        if (processed > 0) {
            log.info("[배치] 만료 처리: {}건 (PILYEON={}, PENDING={}, INYEON={}, PAST={})",
                    processed, expiredPilyeon.size(), expiredPending.size(),
                    expiredInyeon.size(), expiredPast.size());
        }

        return processed;
    }

    // --- Helper Methods ---

    /**
     * 매칭 설정 필터 체크 (A의 설정 기준으로 B가 매칭 가능한지)
     */
    private boolean matchesFilter(User settingsOwner, User target) {
        UserSetting settings = userSettingRepository.findByUserUserId(settingsOwner.getUserId())
                .orElse(null);

        // 설정이 없으면 기본값으로 통과
        if (settings == null) return true;

        // 성별 필터
        if (settings.getGenderFilter() != null && target.getGender() != null
                && settings.getGenderFilter() != target.getGender()) {
            return false;
        }

        // 연령 필터
        if (target.getBirthDate() != null) {
            int age = Period.between(target.getBirthDate(), LocalDate.now()).getYears();
            if (age < settings.getAgeRangeMin() || age > settings.getAgeRangeMax()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Redis에서 사용자 위치 조회
     */
    private double[] getUserLocation(Long userId) {
        String userKey = LOCATION_USER_PREFIX + userId;
        Map<Object, Object> locationData = redisTemplate.opsForHash().entries(userKey);
        if (locationData.isEmpty()) return null;

        try {
            double lat = Double.parseDouble((String) locationData.get("lat"));
            double lng = Double.parseDouble((String) locationData.get("lng"));
            return new double[]{lat, lng};
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Haversine 거리 계산 (미터)
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // 지구 반지름 (미터)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 쿨다운 체크
     */
    private boolean isOnCooldown(Long userIdA, Long userIdB) {
        String cooldownKey = getCooldownKey(userIdA, userIdB);
        return Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey));
    }

    /**
     * 쿨다운 설정
     */
    private void setCooldown(Long userIdA, Long userIdB) {
        String cooldownKey = getCooldownKey(userIdA, userIdB);
        redisTemplate.opsForValue().set(cooldownKey, "1",
                java.time.Duration.ofMinutes(COOLDOWN_MINUTES));
    }

    private String getCooldownKey(Long userIdA, Long userIdB) {
        long min = Math.min(userIdA, userIdB);
        long max = Math.max(userIdA, userIdB);
        return ENCOUNTER_COOLDOWN_PREFIX + min + ":" + max;
    }

    /**
     * Redis 키 스캔 (SCAN 명령 사용, KEYS 대신 안전)
     */
    private Set<String> scanRedisKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    private void createEncounterNotification(User user, Long relationshipId,
                                              NotificationType type, String title, String message) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .deepLink("/relationship/" + relationshipId)
                .build());
    }

    private void saveLog(Relationship rel, RelationshipStatus from, RelationshipStatus to,
                         RelationshipTrigger trigger, User triggeredUser) {
        relationshipLogRepository.save(RelationshipLog.builder()
                .relationship(rel)
                .fromStatus(from)
                .toStatus(to)
                .triggeredBy(trigger)
                .triggeredUser(triggeredUser)
                .build());
    }
}
