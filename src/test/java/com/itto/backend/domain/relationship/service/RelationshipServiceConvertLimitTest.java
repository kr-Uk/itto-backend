package com.itto.backend.domain.relationship.service;

import com.itto.backend.domain.hobby.repository.UserHobbyRepository;
import com.itto.backend.domain.payment.entity.MonthlyUsage;
import com.itto.backend.domain.payment.repository.MonthlyUsageRepository;
import com.itto.backend.domain.relationship.dto.ConvertResponse;
import com.itto.backend.domain.relationship.entity.Relationship;
import com.itto.backend.domain.relationship.repository.*;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserPhotoRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.global.enums.*;
import com.itto.backend.global.exception.BusinessException;
import com.itto.backend.global.exception.ErrorCode;
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
 * D12: Uyeon->Pilyeon convert free limit tests (regression)
 * Normal user: 1 free/month, Premium: 6 free/month, then 30 knots
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelationshipServiceConvertLimitTest {

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
    private MonthlyUsage normalUsage;
    private MonthlyUsage premiumUsage;

    @BeforeEach
    void setUp() {
        normalUser = User.builder()
                .userId(1L)
                .firebaseUid("firebase-normal")
                .provider(Provider.KAKAO)
                .nickname("normal")
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .isPremium(false)
                .knots(100)
                .build();

        premiumUser = User.builder()
                .userId(2L)
                .firebaseUid("firebase-premium")
                .provider(Provider.KAKAO)
                .nickname("premium")
                .gender(Gender.FEMALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .isPremium(true)
                .knots(100)
                .build();

        otherUser = User.builder()
                .userId(3L)
                .firebaseUid("firebase-other")
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

    private Relationship buildUyeonRelationship(User user1, User user2) {
        return Relationship.builder()
                .id(100L)
                .user1(user1)
                .user2(user2)
                .status(RelationshipStatus.UYEON)
                .encounterCount(1)
                .statusChangedAt(LocalDateTime.now())
                .build();
    }

    private void stubConvertMocks(User user, MonthlyUsage usage, Relationship rel) {
        when(relationshipRepository.findById(rel.getId())).thenReturn(Optional.of(rel));
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(monthlyUsageRepository.findByUserUserIdAndYearMonth(eq(user.getUserId()), anyString()))
                .thenReturn(Optional.of(usage));
    }

    @Nested
    @DisplayName("Normal user convert limits")
    class NormalUserConvertLimit {

        @Test
        @DisplayName("First convert is free")
        void firstConvert_shouldBeFree() {
            Relationship rel = buildUyeonRelationship(normalUser, otherUser);
            stubConvertMocks(normalUser, normalUsage, rel);

            ConvertResponse response = relationshipService.convertToPilyeon(1L, 100L);

            assertThat(response.getStatus()).isEqualTo("PILYEON");
            assertThat(response.getKnotsUsed()).isEqualTo(0);
            assertThat(normalUsage.getFreeConvertsUsed()).isEqualTo(1);
        }

        @Test
        @DisplayName("Second convert costs 30 knots")
        void secondConvert_shouldCostKnots() {
            normalUsage.incrementFreeConvertsUsed(); // freeConvertsUsed = 1
            Relationship rel = buildUyeonRelationship(normalUser, otherUser);
            stubConvertMocks(normalUser, normalUsage, rel);

            ConvertResponse response = relationshipService.convertToPilyeon(1L, 100L);

            assertThat(response.getKnotsUsed()).isEqualTo(30);
            assertThat(normalUser.getKnots()).isEqualTo(70); // 100 - 30
        }

        @Test
        @DisplayName("Second convert with insufficient knots throws exception")
        void secondConvert_insufficientKnots_shouldThrow() {
            normalUsage.incrementFreeConvertsUsed();
            normalUser.deductKnots(80); // 20 knots left

            Relationship rel = buildUyeonRelationship(normalUser, otherUser);
            stubConvertMocks(normalUser, normalUsage, rel);

            assertThatThrownBy(() -> relationshipService.convertToPilyeon(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INSUFFICIENT_KNOTS));
        }
    }

    @Nested
    @DisplayName("Premium user convert limits")
    class PremiumUserConvertLimit {

        @Test
        @DisplayName("Premium: 1~6 converts are free")
        void firstSixConverts_shouldBeFree() {
            for (int i = 0; i < 6; i++) {
                Relationship rel = buildUyeonRelationship(premiumUser, otherUser);
                stubConvertMocks(premiumUser, premiumUsage, rel);

                ConvertResponse response = relationshipService.convertToPilyeon(2L, 100L);
                assertThat(response.getKnotsUsed()).isEqualTo(0);
            }

            assertThat(premiumUsage.getFreeConvertsUsed()).isEqualTo(6);
            assertThat(premiumUser.getKnots()).isEqualTo(100); // No knots deducted
        }

        @Test
        @DisplayName("Premium: 7th convert costs 30 knots")
        void seventhConvert_shouldCostKnots() {
            for (int i = 0; i < 6; i++) {
                premiumUsage.incrementFreeConvertsUsed();
            }

            Relationship rel = buildUyeonRelationship(premiumUser, otherUser);
            stubConvertMocks(premiumUser, premiumUsage, rel);

            ConvertResponse response = relationshipService.convertToPilyeon(2L, 100L);

            assertThat(response.getKnotsUsed()).isEqualTo(30);
            assertThat(premiumUser.getKnots()).isEqualTo(70);
        }
    }
}
