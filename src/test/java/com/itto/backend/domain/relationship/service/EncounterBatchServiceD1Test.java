package com.itto.backend.domain.relationship.service;

import com.itto.backend.domain.block.repository.BlockRepository;
import com.itto.backend.domain.location.repository.ExcludedAreaH3IndexRepository;
import com.itto.backend.domain.notification.service.NotificationService;
import com.itto.backend.domain.relationship.entity.Encounter;
import com.itto.backend.domain.relationship.entity.Relationship;
import com.itto.backend.domain.relationship.repository.EncounterRepository;
import com.itto.backend.domain.relationship.repository.RelationshipLogRepository;
import com.itto.backend.domain.relationship.repository.RelationshipRepository;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.user.repository.UserSettingRepository;
import com.itto.backend.global.enums.Provider;
import com.itto.backend.global.enums.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * D1: Encounter distance threshold change from 200m to 100m
 * RED phase: These tests should FAIL with the current 200m threshold
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EncounterBatchServiceD1Test {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private UserRepository userRepository;
    @Mock private UserSettingRepository userSettingRepository;
    @Mock private RelationshipRepository relationshipRepository;
    @Mock private EncounterRepository encounterRepository;
    @Mock private RelationshipLogRepository relationshipLogRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private ExcludedAreaH3IndexRepository excludedAreaH3IndexRepository;
    @Mock private NotificationService notificationService;
    @Mock private SetOperations<String, String> setOperations;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private Cursor<String> cursor;

    private EncounterBatchService encounterBatchService;
    private User userA;
    private User userB;

    // Seoul City Hall coordinates as base point
    private static final double BASE_LAT = 37.5665;
    private static final double BASE_LNG = 126.9780;

    @BeforeEach
    void setUp() {
        encounterBatchService = new EncounterBatchService(
                redisTemplate, userRepository, userSettingRepository,
                relationshipRepository, encounterRepository, relationshipLogRepository,
                blockRepository, excludedAreaH3IndexRepository, notificationService);

        userA = User.builder()
                .userId(1L)
                .firebaseUid("firebase-a")
                .provider(Provider.KAKAO)
                .nickname("UserA")
                .verificationStatus(VerificationStatus.APPROVED)
                .locationEnabled(true)
                .build();

        userB = User.builder()
                .userId(2L)
                .firebaseUid("firebase-b")
                .provider(Provider.KAKAO)
                .nickname("UserB")
                .verificationStatus(VerificationStatus.APPROVED)
                .locationEnabled(true)
                .build();
    }

    /**
     * Helper: Set up Redis mocks for two users in the same H3 cell.
     * Returns the approximate distance in meters between the two users based on lat offset.
     */
    private void setupRedisForTwoUsers(double latA, double lngA, double latB, double lngB) {
        // Mock Redis scan for H3 keys
        when(redisTemplate.scan(any())).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn("location:h3:testH3Index");

        // Mock H3 cell members
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("location:h3:testH3Index")).thenReturn(Set.of("1", "2"));

        // Mock excluded area (none excluded)
        when(excludedAreaH3IndexRepository.findUserIdsByH3Index("testH3Index")).thenReturn(Collections.emptySet());

        // Mock cooldown check
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // Mock block check
        when(blockRepository.existsBlockBetween(anyLong(), anyLong())).thenReturn(false);

        // Mock user lookups
        when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
        when(userRepository.findById(2L)).thenReturn(Optional.of(userB));

        // Mock user settings (no filter)
        when(userSettingRepository.findByUserUserId(anyLong())).thenReturn(Optional.empty());

        // Mock no existing relationship
        when(relationshipRepository.findByUser1UserIdAndUser2UserId(1L, 2L)).thenReturn(Optional.empty());

        // Mock user locations from Redis
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        Map<Object, Object> locA = Map.of("lat", String.valueOf(latA), "lng", String.valueOf(lngA));
        Map<Object, Object> locB = Map.of("lat", String.valueOf(latB), "lng", String.valueOf(lngB));
        when(hashOperations.entries("location:user:1")).thenReturn(locA);
        when(hashOperations.entries("location:user:2")).thenReturn(locB);

        // Mock save operations
        when(relationshipRepository.save(any(Relationship.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encounterRepository.save(any(Encounter.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mock cooldown set
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    /**
     * Calculate a latitude offset to produce approximately the given distance in meters.
     * 1 degree latitude ~ 111,320 meters
     */
    private double latOffsetForMeters(double meters) {
        return meters / 111320.0;
    }

    @Test
    @DisplayName("D1: 99m 거리의 두 사용자 - 스침 판정 성공 (100m 이내)")
    void detectEncounters_usersAt99m_shouldDetect() {
        // given: two users approximately 99m apart (within new 100m threshold)
        double latOffset = latOffsetForMeters(99);
        setupRedisForTwoUsers(BASE_LAT, BASE_LNG, BASE_LAT + latOffset, BASE_LNG);

        // when
        int result = encounterBatchService.detectEncounters();

        // then: encounter should be detected
        assertThat(result).isEqualTo(1);
        verify(relationshipRepository).save(any(Relationship.class));
    }

    @Test
    @DisplayName("D1: 101m 거리의 두 사용자 - 스침 판정 실패 (100m 초과)")
    void detectEncounters_usersAt101m_shouldNotDetect() {
        // given: two users approximately 101m apart (outside new 100m threshold)
        double latOffset = latOffsetForMeters(101);
        setupRedisForTwoUsers(BASE_LAT, BASE_LNG, BASE_LAT + latOffset, BASE_LNG);

        // when
        int result = encounterBatchService.detectEncounters();

        // then: encounter should NOT be detected
        assertThat(result).isEqualTo(0);
        verify(relationshipRepository, never()).save(any(Relationship.class));
    }

    @Test
    @DisplayName("D1: 150m 거리의 두 사용자 - 스침 판정 실패 (구 200m 기준에서는 통과, 신 100m에서는 차단)")
    void detectEncounters_usersAt150m_shouldNotDetect() {
        // given: two users approximately 150m apart
        // Under old 200m threshold this would pass, but under new 100m threshold it must fail
        double latOffset = latOffsetForMeters(150);
        setupRedisForTwoUsers(BASE_LAT, BASE_LNG, BASE_LAT + latOffset, BASE_LNG);

        // when
        int result = encounterBatchService.detectEncounters();

        // then: encounter should NOT be detected with 100m threshold
        assertThat(result).isEqualTo(0);
        verify(relationshipRepository, never()).save(any(Relationship.class));
    }

    @Test
    @DisplayName("D1: 200m 거리의 두 사용자 - 스침 판정 실패 (구 임계값 회귀 방지)")
    void detectEncounters_usersAt200m_shouldNotDetect_regressionGuard() {
        // given: two users approximately 200m apart
        // This is the old threshold — must be rejected under new 100m threshold
        double latOffset = latOffsetForMeters(200);
        setupRedisForTwoUsers(BASE_LAT, BASE_LNG, BASE_LAT + latOffset, BASE_LNG);

        // when
        int result = encounterBatchService.detectEncounters();

        // then: encounter should NOT be detected
        assertThat(result).isEqualTo(0);
        verify(relationshipRepository, never()).save(any(Relationship.class));
    }
}
