package com.itto.backend.domain.relationship.service;
import com.itto.backend.application.relationship.RelationshipService;

import com.itto.backend.domain.hobby.repository.UserHobbyRepository;
import com.itto.backend.domain.payment.entity.MonthlyUsage;
import com.itto.backend.domain.payment.repository.MonthlyUsageRepository;
import com.itto.backend.presentation.dto.relationship.InyeonRequestResponse;
import com.itto.backend.domain.relationship.entity.InyeonRequest;
import com.itto.backend.domain.relationship.entity.Relationship;
import com.itto.backend.domain.relationship.repository.*;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserPhotoRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.common.enums.*;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * D12: Inyeon request free limit tests
 * Normal user: 1 free/month, Premium: 6 free/month, then 30 knots
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelationshipServiceRequestLimitTest {

    @Mock private RelationshipRepository relationshipRepository;
    @Mock private InyeonRequestRepository inyeonRequestRepository;
    @Mock private RestoreRequestRepository restoreRequestRepository;
    @Mock private EncounterRepository encounterRepository;
    @Mock private RelationshipLogRepository relationshipLogRepository;
    @Mock private MonthlyUsageRepository monthlyUsageRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserPhotoRepository userPhotoRepository;
    @Mock private UserHobbyRepository userHobbyRepository;

    @InjectMocks
    private RelationshipService relationshipService;

    private User normalUser;
    private User premiumUser;
    private User otherUser;
    private Relationship pilyeonRelationship;
    private MonthlyUsage normalUsage;
    private MonthlyUsage premiumUsage;

    @BeforeEach
    void setUp() {
        normalUser = User.builder()
                .userId(1L)
                .providerUid("firebase-normal")
                .provider(Provider.KAKAO)
                .nickname("normal")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .isPremium(false)
                .knots(100)
                .build();

        premiumUser = User.builder()
                .userId(2L)
                .providerUid("firebase-premium")
                .provider(Provider.KAKAO)
                .nickname("premium")
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .isPremium(true)
                .knots(100)
                .build();

        otherUser = User.builder()
                .userId(3L)
                .providerUid("firebase-other")
                .provider(Provider.KAKAO)
                .nickname("other")
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .isPremium(false)
                .knots(0)
                .build();

        normalUsage = MonthlyUsage.builder()
                .user(normalUser)
                .yearMonth(YearMonth.now().toString())
                .freeRequestsUsed(0)
                .freeConvertsUsed(0)
                .freeRestoresUsed(0)
                .build();

        premiumUsage = MonthlyUsage.builder()
                .user(premiumUser)
                .yearMonth(YearMonth.now().toString())
                .freeRequestsUsed(0)
                .freeConvertsUsed(0)
                .freeRestoresUsed(0)
                .build();
    }

    private Relationship buildPilyeonRelationship(User user1, User user2) {
        return Relationship.builder()
                .id(100L)
                .user1(user1)
                .user2(user2)
                .status(RelationshipStatus.PILYEON)
                .encounterCount(2)
                .statusChangedAt(LocalDateTime.now())
                .build();
    }

    private void stubCommonMocks(User requester, MonthlyUsage usage, Relationship rel) {
        when(relationshipRepository.findById(rel.getId())).thenReturn(Optional.of(rel));
        when(inyeonRequestRepository.findByRelationshipIdAndStatus(rel.getId(), InyeonRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(userRepository.findById(requester.getUserId())).thenReturn(Optional.of(requester));
        when(monthlyUsageRepository.findByUserUserIdAndYearMonth(eq(requester.getUserId()), anyString()))
                .thenReturn(Optional.of(usage));
        when(inyeonRequestRepository.save(any(InyeonRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("Normal user inyeon request limits")
    class NormalUserRequestLimit {

        @Test
        @DisplayName("First request is free (freeRequestsUsed: 0 -> 1)")
        void firstRequest_shouldBeFree() {
            Relationship rel = buildPilyeonRelationship(normalUser, otherUser);
            stubCommonMocks(normalUser, normalUsage, rel);

            InyeonRequestResponse response = relationshipService.requestInyeon(1L, 100L);

            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(normalUsage.getFreeRequestsUsed()).isEqualTo(1);
            // Normal user: 1 free/month, used 1 => 0 remaining
            assertThat(response.getRemainingFreeRequests()).isEqualTo(0);
        }

        @Test
        @DisplayName("Second request costs 30 knots (free limit = 1)")
        void secondRequest_shouldCostKnots() {
            // Already used 1 free request
            normalUsage.incrementFreeRequestsUsed(); // freeRequestsUsed = 1
            Relationship rel = buildPilyeonRelationship(normalUser, otherUser);
            stubCommonMocks(normalUser, normalUsage, rel);

            InyeonRequestResponse response = relationshipService.requestInyeon(1L, 100L);

            assertThat(response.getStatus()).isEqualTo("PENDING");
            // Should have deducted 30 knots
            assertThat(normalUser.getKnots()).isEqualTo(70); // 100 - 30
        }

        @Test
        @DisplayName("Second request with insufficient knots throws INSUFFICIENT_KNOTS")
        void secondRequest_insufficientKnots_shouldThrow() {
            normalUsage.incrementFreeRequestsUsed(); // freeRequestsUsed = 1
            // Set knots to 20 (less than 30)
            normalUser.deductKnots(80); // 100 - 80 = 20

            Relationship rel = buildPilyeonRelationship(normalUser, otherUser);
            stubCommonMocks(normalUser, normalUsage, rel);

            assertThatThrownBy(() -> relationshipService.requestInyeon(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INSUFFICIENT_KNOTS));
        }
    }

    @Nested
    @DisplayName("Premium user inyeon request limits")
    class PremiumUserRequestLimit {

        @Test
        @DisplayName("Premium: 1~6 requests are free")
        void firstSixRequests_shouldBeFree() {
            Relationship rel = buildPilyeonRelationship(premiumUser, otherUser);

            for (int i = 0; i < 6; i++) {
                // Reset relationship status to PILYEON for each iteration
                rel.updateStatus(RelationshipStatus.PILYEON);
                stubCommonMocks(premiumUser, premiumUsage, rel);
                when(inyeonRequestRepository.findByRelationshipIdAndStatus(rel.getId(), InyeonRequestStatus.PENDING))
                        .thenReturn(Optional.empty());

                InyeonRequestResponse response = relationshipService.requestInyeon(2L, 100L);
                assertThat(response.getStatus()).isEqualTo("PENDING");
            }

            // After 6 free requests, all 6 should have been free
            assertThat(premiumUsage.getFreeRequestsUsed()).isEqualTo(6);
            assertThat(premiumUser.getKnots()).isEqualTo(100); // No knots deducted
        }

        @Test
        @DisplayName("Premium: 7th request costs 30 knots")
        void seventhRequest_shouldCostKnots() {
            // Simulate 6 free requests already used
            for (int i = 0; i < 6; i++) {
                premiumUsage.incrementFreeRequestsUsed();
            }

            Relationship rel = buildPilyeonRelationship(premiumUser, otherUser);
            stubCommonMocks(premiumUser, premiumUsage, rel);

            InyeonRequestResponse response = relationshipService.requestInyeon(2L, 100L);

            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(premiumUser.getKnots()).isEqualTo(70); // 100 - 30
            assertThat(response.getRemainingFreeRequests()).isEqualTo(0);
        }

        @Test
        @DisplayName("Premium: remaining free requests count is correct")
        void remainingFreeRequests_shouldBeCorrect() {
            // Used 3 of 6 free requests
            for (int i = 0; i < 3; i++) {
                premiumUsage.incrementFreeRequestsUsed();
            }

            Relationship rel = buildPilyeonRelationship(premiumUser, otherUser);
            stubCommonMocks(premiumUser, premiumUsage, rel);

            InyeonRequestResponse response = relationshipService.requestInyeon(2L, 100L);

            // Used 3, using 1 more = 4 used, 6 - 4 = 2 remaining
            assertThat(response.getRemainingFreeRequests()).isEqualTo(2);
        }
    }
}
