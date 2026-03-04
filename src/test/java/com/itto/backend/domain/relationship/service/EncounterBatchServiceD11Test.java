package com.itto.backend.domain.relationship.service;

import com.itto.backend.domain.block.repository.BlockRepository;
import com.itto.backend.domain.location.repository.ExcludedAreaH3IndexRepository;
import com.itto.backend.domain.notification.service.NotificationService;
import com.itto.backend.domain.relationship.entity.Relationship;
import com.itto.backend.domain.relationship.repository.EncounterRepository;
import com.itto.backend.domain.relationship.repository.RelationshipLogRepository;
import com.itto.backend.domain.relationship.repository.RelationshipRepository;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.user.repository.UserSettingRepository;
import com.itto.backend.global.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * D11: PILYEON relationships should NOT expire (infinite duration).
 * PILYEON expiration logic must be removed from processExpirations().
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EncounterBatchServiceD11Test {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private UserRepository userRepository;
    @Mock private UserSettingRepository userSettingRepository;
    @Mock private RelationshipRepository relationshipRepository;
    @Mock private EncounterRepository encounterRepository;
    @Mock private RelationshipLogRepository relationshipLogRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private ExcludedAreaH3IndexRepository excludedAreaH3IndexRepository;
    @Mock private NotificationService notificationService;

    private EncounterBatchService encounterBatchService;

    @BeforeEach
    void setUp() {
        encounterBatchService = new EncounterBatchService(
                redisTemplate, userRepository, userSettingRepository,
                relationshipRepository, encounterRepository, relationshipLogRepository,
                blockRepository, excludedAreaH3IndexRepository, notificationService);
    }

    @Test
    @DisplayName("D11: PILYEON 관계는 만료 처리에서 제외되어야 한다")
    void processExpirations_pilyeonShouldNotExpire() {
        // given: expired PILYEON relationships exist
        User user1 = User.builder().userId(1L).firebaseUid("fb-1").provider(Provider.KAKAO).build();
        User user2 = User.builder().userId(2L).firebaseUid("fb-2").provider(Provider.KAKAO).build();

        Relationship pilyeonRel = Relationship.builder()
                .user1(user1).user2(user2)
                .status(RelationshipStatus.PILYEON)
                .encounterCount(2)
                .statusChangedAt(LocalDateTime.now().minusHours(25))
                .expiresAt(LocalDateTime.now().minusHours(1)) // expired
                .build();

        // PILYEON should NOT be queried for expiration at all
        when(relationshipRepository.findByStatusAndExpiresAtBefore(
                eq(RelationshipStatus.PILYEON), any(LocalDateTime.class)))
                .thenReturn(List.of(pilyeonRel));

        // Other statuses return empty
        when(relationshipRepository.findByStatusAndExpiresAtBefore(
                eq(RelationshipStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(relationshipRepository.findByStatusAndExpiresAtBefore(
                eq(RelationshipStatus.INYEON), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(relationshipRepository.findByStatusAndExpiresAtBefore(
                eq(RelationshipStatus.PAST), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // when
        int processed = encounterBatchService.processExpirations();

        // then: PILYEON should NOT be processed (0 expirations)
        assertThat(processed).isEqualTo(0);
        // Status should remain PILYEON, not changed to PAST
        assertThat(pilyeonRel.getStatus()).isEqualTo(RelationshipStatus.PILYEON);
    }

    @Test
    @DisplayName("D11: PENDING 24h 만료는 여전히 동작해야 한다")
    void processExpirations_pendingShouldStillExpire() {
        // given
        User user1 = User.builder().userId(1L).firebaseUid("fb-1").provider(Provider.KAKAO).build();
        User user2 = User.builder().userId(2L).firebaseUid("fb-2").provider(Provider.KAKAO).build();

        Relationship pendingRel = Relationship.builder()
                .user1(user1).user2(user2)
                .status(RelationshipStatus.PENDING)
                .encounterCount(2)
                .statusChangedAt(LocalDateTime.now().minusHours(25))
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(relationshipRepository.findByStatusAndExpiresAtBefore(
                eq(RelationshipStatus.PILYEON), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(relationshipRepository.findByStatusAndExpiresAtBefore(
                eq(RelationshipStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(pendingRel));
        when(relationshipRepository.findByStatusAndExpiresAtBefore(
                eq(RelationshipStatus.INYEON), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(relationshipRepository.findByStatusAndExpiresAtBefore(
                eq(RelationshipStatus.PAST), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // when
        int processed = encounterBatchService.processExpirations();

        // then: PENDING should still be expired to PAST
        assertThat(processed).isEqualTo(1);
        assertThat(pendingRel.getStatus()).isEqualTo(RelationshipStatus.PAST);
    }

    @Test
    @DisplayName("D11: PAST 7일 만료 → ENDED는 여전히 동작해야 한다")
    void processExpirations_pastShouldStillExpireToEnded() {
        // given
        User user1 = User.builder().userId(1L).firebaseUid("fb-1").provider(Provider.KAKAO).build();
        User user2 = User.builder().userId(2L).firebaseUid("fb-2").provider(Provider.KAKAO).build();

        Relationship pastRel = Relationship.builder()
                .user1(user1).user2(user2)
                .status(RelationshipStatus.PAST)
                .encounterCount(2)
                .statusChangedAt(LocalDateTime.now().minusDays(8))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(relationshipRepository.findByStatusAndExpiresAtBefore(
                eq(RelationshipStatus.PILYEON), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(relationshipRepository.findByStatusAndExpiresAtBefore(
                eq(RelationshipStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(relationshipRepository.findByStatusAndExpiresAtBefore(
                eq(RelationshipStatus.INYEON), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(relationshipRepository.findByStatusAndExpiresAtBefore(
                eq(RelationshipStatus.PAST), any(LocalDateTime.class)))
                .thenReturn(List.of(pastRel));

        // when
        int processed = encounterBatchService.processExpirations();

        // then
        assertThat(processed).isEqualTo(1);
        assertThat(pastRel.getStatus()).isEqualTo(RelationshipStatus.ENDED);
    }
}
