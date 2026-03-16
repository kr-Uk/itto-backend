package com.itto.backend.domain.relationship.service;
import com.itto.backend.application.relationship.EncounterBatchService;

import com.itto.backend.domain.block.repository.BlockRepository;
import com.itto.backend.domain.location.repository.ExcludedAreaH3IndexRepository;
import com.itto.backend.application.notification.NotificationService;
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
import com.itto.backend.domain.common.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * D3: 3rd encounter (PILYEON) -> auto INYEON + destiny badge.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EncounterBatchServiceD3Test {

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

    private EncounterBatchService encounterBatchService;

    // Test users: verified, location enabled, within 50m
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        encounterBatchService = new EncounterBatchService(
                redisTemplate, userRepository, userSettingRepository,
                relationshipRepository, encounterRepository, relationshipLogRepository,
                blockRepository, excludedAreaH3IndexRepository, notificationService);

        user1 = User.builder()
                .userId(1L).providerUid("fb-1").provider(Provider.KAKAO)
                .nickname("user1").gender(Gender.MALE)
                .birthDate(LocalDate.of(1995, 1, 1))
                .locationEnabled(true)
                .verificationStatus(VerificationStatus.APPROVED)
                .build();
        user2 = User.builder()
                .userId(2L).providerUid("fb-2").provider(Provider.KAKAO)
                .nickname("user2").gender(Gender.FEMALE)
                .birthDate(LocalDate.of(1996, 1, 1))
                .locationEnabled(true)
                .verificationStatus(VerificationStatus.APPROVED)
                .build();

        // Common mocks for detectEncounters flow
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // No cooldown
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // No blocks
        when(blockRepository.existsBlockBetween(anyLong(), anyLong())).thenReturn(false);

        // Users found (batch)
        when(userRepository.findAllById(anyList())).thenReturn(List.of(user1, user2));

        // No matching filter restriction (batch)
        when(userSettingRepository.findByUserUserIdIn(anyList())).thenReturn(Collections.emptyList());

        // Locations within 50m of each other (Seoul area)
        Map<Object, Object> loc1 = Map.of("lat", "37.5665", "lng", "126.9780");
        Map<Object, Object> loc2 = Map.of("lat", "37.5666", "lng", "126.9780");
        when(hashOperations.entries("location:user:1")).thenReturn(loc1);
        when(hashOperations.entries("location:user:2")).thenReturn(loc2);

        // No excluded areas
        when(excludedAreaH3IndexRepository.findUserIdsByH3Index(anyString())).thenReturn(Collections.emptySet());
    }

    @Test
    @DisplayName("D3: PILYEON(encounterCount=2)에서 3번째 스침 → INYEON + destinyBadge=true")
    void thirdEncounter_pilyeonToInyeon_withDestinyBadge() {
        // given: PILYEON relationship with encounterCount=2
        Relationship pilyeonRel = Relationship.builder()
                .user1(user1).user2(user2)
                .status(RelationshipStatus.PILYEON)
                .encounterCount(2)
                .firstEncounterAt(LocalDateTime.now().minusDays(2))
                .lastEncounterAt(LocalDateTime.now().minusDays(1))
                .statusChangedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(relationshipRepository.findByUser1UserIdAndUser2UserId(1L, 2L))
                .thenReturn(Optional.of(pilyeonRel));

        // Setup Redis scan to return one H3 cell with both users
        setupRedisScanWithUsers("h3cell1", "1", "2");

        // when
        int result = encounterBatchService.detectEncounters();

        // then
        assertThat(result).isEqualTo(1);
        assertThat(pilyeonRel.getStatus()).isEqualTo(RelationshipStatus.INYEON);
        assertThat(pilyeonRel.isDestinyBadge()).isTrue();
        assertThat(pilyeonRel.getEncounterCount()).isEqualTo(3);

        // Verify log saved: PILYEON -> INYEON
        ArgumentCaptor<RelationshipLog> logCaptor = ArgumentCaptor.forClass(RelationshipLog.class);
        verify(relationshipLogRepository).save(logCaptor.capture());
        RelationshipLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getFromStatus()).isEqualTo(RelationshipStatus.PILYEON);
        assertThat(savedLog.getToStatus()).isEqualTo(RelationshipStatus.INYEON);
        assertThat(savedLog.getTriggeredBy()).isEqualTo(RelationshipTrigger.ENCOUNTER);

        // Verify ENCOUNTER_INYEON_DESTINY notifications sent to both users
        verify(notificationService, times(2)).createAndPush(
                anyLong(),
                eq(NotificationType.ENCOUNTER_INYEON_DESTINY),
                eq("운명적인 만남!"),
                eq("세 번째 스침! 운명의 상대를 만났어요."),
                anyString());

        // Verify encounter record saved
        verify(encounterRepository).save(any(Encounter.class));
    }

    @Test
    @DisplayName("D3: UYEON에서 2번째 스침 → PILYEON (destinyBadge=false, 기존 동작 유지)")
    void secondEncounter_uyeonToPilyeon_noDestinyBadge() {
        // given: UYEON relationship with encounterCount=1
        Relationship uyeonRel = Relationship.builder()
                .user1(user1).user2(user2)
                .status(RelationshipStatus.UYEON)
                .encounterCount(1)
                .firstEncounterAt(LocalDateTime.now().minusDays(1))
                .lastEncounterAt(LocalDateTime.now().minusDays(1))
                .statusChangedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(relationshipRepository.findByUser1UserIdAndUser2UserId(1L, 2L))
                .thenReturn(Optional.of(uyeonRel));

        setupRedisScanWithUsers("h3cell1", "1", "2");

        // when
        int result = encounterBatchService.detectEncounters();

        // then
        assertThat(result).isEqualTo(1);
        assertThat(uyeonRel.getStatus()).isEqualTo(RelationshipStatus.PILYEON);
        assertThat(uyeonRel.isDestinyBadge()).isFalse();
        assertThat(uyeonRel.getEncounterCount()).isEqualTo(2);

        // Verify ENCOUNTER_PILYEON notification (not destiny)
        verify(notificationService, times(2)).createAndPush(
                anyLong(),
                eq(NotificationType.ENCOUNTER_PILYEON),
                anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("D3: INYEON 상태에서는 스침이 무시되어야 한다 (early-return)")
    void inyeonStatus_shouldBeIgnored() {
        // given: INYEON relationship
        Relationship inyeonRel = Relationship.builder()
                .user1(user1).user2(user2)
                .status(RelationshipStatus.INYEON)
                .encounterCount(3)
                .destinyBadge(true)
                .statusChangedAt(LocalDateTime.now())
                .build();

        when(relationshipRepository.findByUser1UserIdAndUser2UserId(1L, 2L))
                .thenReturn(Optional.of(inyeonRel));

        setupRedisScanWithUsers("h3cell1", "1", "2");

        // when
        int result = encounterBatchService.detectEncounters();

        // then: no encounter processed
        assertThat(result).isEqualTo(0);
        assertThat(inyeonRel.getStatus()).isEqualTo(RelationshipStatus.INYEON);
    }

    /**
     * Helper to setup Redis scan returning one H3 cell with the given user IDs.
     */
    @SuppressWarnings("unchecked")
    private void setupRedisScanWithUsers(String h3Cell, String... userIds) {
        String h3Key = "location:h3:" + h3Cell;

        // Mock scan to return the h3Key
        Cursor<String> mockCursor = mock(Cursor.class);
        when(mockCursor.hasNext()).thenReturn(true, false);
        when(mockCursor.next()).thenReturn(h3Key);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(mockCursor);

        // Mock set members for this h3 cell
        when(setOperations.members(h3Key)).thenReturn(new HashSet<>(Arrays.asList(userIds)));
    }
}
