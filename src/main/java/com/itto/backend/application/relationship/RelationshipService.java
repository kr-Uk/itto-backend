package com.itto.backend.application.relationship;

import com.itto.backend.domain.payment.entity.MonthlyUsage;
import com.itto.backend.domain.payment.repository.MonthlyUsageRepository;
import com.itto.backend.presentation.dto.relationship.*;
import com.itto.backend.domain.relationship.entity.*;
import com.itto.backend.domain.relationship.repository.*;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.entity.UserPhoto;
import com.itto.backend.domain.user.repository.UserPhotoRepository;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.hobby.entity.UserHobby;
import com.itto.backend.domain.hobby.repository.UserHobbyRepository;
import com.itto.backend.domain.common.enums.*;
import com.itto.backend.domain.common.constants.BusinessConstants;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelationshipService {

    private static final int FREE_REQUESTS_PER_MONTH = BusinessConstants.FREE_REQUESTS_PER_MONTH;
    private static final int PREMIUM_REQUESTS_PER_MONTH = BusinessConstants.PREMIUM_REQUESTS_PER_MONTH;
    private static final int FREE_CONVERTS_PER_MONTH = BusinessConstants.FREE_CONVERTS_PER_MONTH;
    private static final int PREMIUM_CONVERTS_PER_MONTH = BusinessConstants.PREMIUM_CONVERTS_PER_MONTH;
    private static final int KNOTS_COST = BusinessConstants.KNOTS_COST;

    private final RelationshipRepository relationshipRepository;
    private final InyeonRequestRepository inyeonRequestRepository;
    private final RestoreRequestRepository restoreRequestRepository;
    private final EncounterRepository encounterRepository;
    private final RelationshipLogRepository relationshipLogRepository;
    private final MonthlyUsageRepository monthlyUsageRepository;
    private final UserRepository userRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final UserHobbyRepository userHobbyRepository;

    // --- 5-1. 관계 목록 조회 (페이지네이션) ---
    public RelationshipListResponse getRelationships(Long userId, String status, int page, int size) {
        RelationshipStatus relStatus = RelationshipStatus.valueOf(status.toUpperCase());
        Page<Relationship> relPage = relationshipRepository.findAllByUserIdAndStatus(
                userId, relStatus, PageRequest.of(page, size, Sort.by("lastEncounterAt").descending()));

        List<RelationshipListResponse.RelationshipSummary> content = relPage.getContent().stream()
                .map(r -> {
                    User other = r.getOtherUser(userId);
                    // 모자이크 여부: UYEON이면 모자이크
                    boolean isMosaic = r.getStatus() == RelationshipStatus.UYEON;
                    String representativePhoto = getRepresentativePhotoUrl(other.getUserId());

                    return RelationshipListResponse.RelationshipSummary.builder()
                            .relationshipId(r.getId())
                            .otherUser(RelationshipListResponse.OtherUserSummary.builder()
                                    .userId(other.getUserId())
                                    .nickname(other.getNickname())
                                    .representativePhoto(representativePhoto)
                                    .isMosaic(isMosaic)
                                    .build())
                            .status(r.getStatus().name())
                            .encounterCount(r.getEncounterCount())
                            .lastEncounterAt(r.getLastEncounterAt())
                            .expiresAt(r.getExpiresAt())
                            .createdAt(r.getCreatedAt())
                            .chatRoomId(r.getChatRoomId())
                            .destinyBadge(r.isDestinyBadge())
                            .build();
                }).toList();

        return RelationshipListResponse.builder()
                .content(content)
                .page(relPage.getNumber())
                .size(relPage.getSize())
                .totalElements(relPage.getTotalElements())
                .totalPages(relPage.getTotalPages())
                .build();
    }

    // --- 5-2. 관계 상세 조회 ---
    public RelationshipDetailResponse getRelationshipDetail(Long userId, Long relationshipId) {
        Relationship rel = getRelationshipAndValidate(relationshipId, userId);
        User other = rel.getOtherUser(userId);

        // 관계 상태에 따른 프로필 공개 범위
        boolean isMosaic = rel.getStatus() == RelationshipStatus.UYEON;
        boolean showPhotos = rel.getStatus() != RelationshipStatus.PAST;
        boolean showAllInfo = rel.getStatus() == RelationshipStatus.INYEON;

        List<RelationshipDetailResponse.PhotoInfo> photos = List.of();
        if (showPhotos) {
            photos = userPhotoRepository.findByUserUserIdOrderByDisplayOrderAsc(other.getUserId()).stream()
                    .map(p -> RelationshipDetailResponse.PhotoInfo.builder()
                            .url(p.getPhotoUrl())
                            .isMosaic(isMosaic)
                            .isRepresentative(p.getIsRepresentative())
                            .build())
                    .toList();
        } else {
            // PAST: 대표사진 1장만
            String repPhoto = getRepresentativePhotoUrl(other.getUserId());
            if (repPhoto != null) {
                photos = List.of(RelationshipDetailResponse.PhotoInfo.builder()
                        .url(repPhoto).isMosaic(false).isRepresentative(true).build());
            }
        }

        List<RelationshipDetailResponse.HobbyInfo> hobbies = List.of();
        if (rel.getStatus() != RelationshipStatus.PAST) {
            hobbies = userHobbyRepository.findByUserUserId(other.getUserId()).stream()
                    .map(uh -> RelationshipDetailResponse.HobbyInfo.builder()
                            .id(uh.getHobby().getHobbyId())
                            .name(uh.getHobby().getName())
                            .build())
                    .toList();
        }

        int age = other.getBirthDate() != null
                ? Period.between(other.getBirthDate(), LocalDate.now()).getYears()
                : 0;

        return RelationshipDetailResponse.builder()
                .relationshipId(rel.getId())
                .otherUser(RelationshipDetailResponse.OtherUserDetail.builder()
                        .userId(other.getUserId())
                        .nickname(other.getNickname())
                        .gender(other.getGender() != null ? other.getGender().name() : null)
                        .age(age)
                        .hobbies(hobbies)
                        .introduction(showAllInfo ? other.getIntroduction() : null)
                        .photos(photos)
                        .build())
                .status(rel.getStatus().name())
                .encounterCount(rel.getEncounterCount())
                .firstEncounterAt(rel.getFirstEncounterAt())
                .lastEncounterAt(rel.getLastEncounterAt())
                .expiresAt(rel.getExpiresAt())
                .createdAt(rel.getCreatedAt())
                .chatRoomId(rel.getChatRoomId())
                .destinyBadge(rel.isDestinyBadge())
                .build();
    }

    // --- 5-3. 인연 신청 ---
    @Transactional
    public InyeonRequestResponse requestInyeon(Long userId, Long relationshipId) {
        Relationship rel = getRelationshipAndValidate(relationshipId, userId);

        if (rel.getStatus() != RelationshipStatus.PILYEON) {
            throw new BusinessException(ErrorCode.NOT_PILYEON);
        }

        // 이미 대기 중인 신청이 있는지 확인
        inyeonRequestRepository.findByRelationshipIdAndStatus(relationshipId, InyeonRequestStatus.PENDING)
                .ifPresent(r -> { throw new BusinessException(ErrorCode.ALREADY_PENDING); });

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User receiver = rel.getOtherUser(userId);

        // 무료 횟수 확인 및 매듭 차감
        MonthlyUsage usage = getOrCreateMonthlyUsage(requester);
        int freeLimit = requester.getIsPremium() ? PREMIUM_REQUESTS_PER_MONTH : FREE_REQUESTS_PER_MONTH;
        int knotsUsed = 0;

        if (usage.getFreeRequestsUsed() < freeLimit) {
            // 무료 횟수 사용
            usage.incrementFreeRequestsUsed();
        } else {
            // 매듭 차감
            if (requester.getKnots() < KNOTS_COST) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_KNOTS);
            }
            requester.deductKnots(KNOTS_COST);
            knotsUsed = KNOTS_COST;
        }

        int freeRemaining = Math.max(0, freeLimit - usage.getFreeRequestsUsed());

        // 인연 신청 생성
        InyeonRequest request = InyeonRequest.builder()
                .relationship(rel)
                .requester(requester)
                .receiver(receiver)
                .status(InyeonRequestStatus.PENDING)
                .knotsUsed(knotsUsed)
                .build();
        inyeonRequestRepository.save(request);

        // 관계 상태를 PENDING으로 변경
        rel.updateStatus(RelationshipStatus.PENDING);

        // 로그 기록
        saveLog(rel, RelationshipStatus.PILYEON, RelationshipStatus.PENDING, RelationshipTrigger.REQUEST, requester);

        return InyeonRequestResponse.builder()
                .status("PENDING")
                .message("인연 신청을 보냈습니다")
                .remainingFreeRequests(freeRemaining)
                .build();
    }

    // --- 5-4. 인연 신청 취소 ---
    @Transactional
    public InyeonCancelResponse cancelInyeon(Long userId, Long relationshipId) {
        Relationship rel = getRelationshipAndValidate(relationshipId, userId);

        if (rel.getStatus() != RelationshipStatus.PENDING) {
            throw new BusinessException(ErrorCode.NOT_PENDING);
        }

        InyeonRequest request = inyeonRequestRepository.findByRelationshipIdAndStatus(
                        relationshipId, InyeonRequestStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        request.respond(InyeonRequestStatus.CANCELLED);
        rel.updateStatusWithExpiry(RelationshipStatus.PILYEON, rel.getExpiresAt());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        saveLog(rel, RelationshipStatus.PENDING, RelationshipStatus.PILYEON, RelationshipTrigger.CANCEL, user);

        return InyeonCancelResponse.builder()
                .status("PILYEON")
                .message("인연 신청이 취소되었습니다")
                .build();
    }

    // --- 5-5. 인연 신청 수락 ---
    @Transactional
    public InyeonAcceptResponse acceptInyeon(Long userId, Long relationshipId) {
        Relationship rel = getRelationshipAndValidate(relationshipId, userId);

        if (rel.getStatus() != RelationshipStatus.PENDING) {
            throw new BusinessException(ErrorCode.NOT_PENDING);
        }

        InyeonRequest request = inyeonRequestRepository.findByRelationshipIdAndStatus(
                        relationshipId, InyeonRequestStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        request.respond(InyeonRequestStatus.ACCEPTED);

        // INYEON으로 전환 (7일 만료)
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        rel.updateStatusWithExpiry(RelationshipStatus.INYEON, expiresAt);

        // 채팅방 ID 생성
        String chatRoomId = UUID.randomUUID().toString();
        rel.setChatRoomId(chatRoomId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        saveLog(rel, RelationshipStatus.PENDING, RelationshipStatus.INYEON, RelationshipTrigger.ACCEPT, user);

        return InyeonAcceptResponse.builder()
                .status("INYEON")
                .chatRoomId(chatRoomId)
                .message("인연이 성립되었습니다")
                .expiresAt(expiresAt)
                .build();
    }

    // --- 5-6. 인연 신청 거절 ---
    @Transactional
    public InyeonRejectResponse rejectInyeon(Long userId, Long relationshipId) {
        Relationship rel = getRelationshipAndValidate(relationshipId, userId);

        if (rel.getStatus() != RelationshipStatus.PENDING) {
            throw new BusinessException(ErrorCode.NOT_PENDING);
        }

        InyeonRequest request = inyeonRequestRepository.findByRelationshipIdAndStatus(
                        relationshipId, InyeonRequestStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        request.respond(InyeonRequestStatus.REJECTED);

        // PAST로 전환 (7일 후 삭제)
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        rel.updateStatusWithExpiry(RelationshipStatus.PAST, expiresAt);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        saveLog(rel, RelationshipStatus.PENDING, RelationshipStatus.PAST, RelationshipTrigger.REJECT, user);

        return InyeonRejectResponse.builder()
                .status("PAST")
                .message("인연 신청을 거절했습니다")
                .expiresAt(expiresAt)
                .build();
    }

    // --- 5-7. 인연 삭제 ---
    @Transactional
    public RelationshipDeleteResponse deleteRelationship(Long userId, Long relationshipId) {
        Relationship rel = getRelationshipAndValidate(relationshipId, userId);

        RelationshipStatus prevStatus = rel.getStatus();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        rel.updateStatusWithExpiry(RelationshipStatus.PAST, expiresAt);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        saveLog(rel, prevStatus, RelationshipStatus.PAST, RelationshipTrigger.DELETE, user);

        return RelationshipDeleteResponse.builder()
                .status("PAST")
                .message("인연이 삭제되었습니다")
                .expiresAt(expiresAt)
                .build();
    }

    // --- 5-8. 지난인연 복원 요청 ---
    @Transactional
    public RestoreRequestResponse requestRestore(Long userId, Long relationshipId) {
        Relationship rel = getRelationshipAndValidate(relationshipId, userId);

        if (rel.getStatus() != RelationshipStatus.PAST) {
            throw new BusinessException(ErrorCode.NOT_PAST);
        }

        int restoreCost = BusinessConstants.RESTORE_KNOTS_COST;
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (requester.getKnots() < restoreCost) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_KNOTS);
        }

        requester.deductKnots(restoreCost);

        User receiver = rel.getOtherUser(userId);
        RestoreRequest request = RestoreRequest.builder()
                .relationship(rel)
                .requester(requester)
                .receiver(receiver)
                .status(RestoreRequestStatus.PENDING)
                .knotsUsed(restoreCost)
                .build();
        restoreRequestRepository.save(request);

        return RestoreRequestResponse.builder()
                .message("복원 요청을 보냈습니다")
                .knotsUsed(restoreCost)
                .remainingKnots(requester.getKnots())
                .build();
    }

    // --- 5-9. 복원 수락 ---
    @Transactional
    public RestoreAcceptResponse acceptRestore(Long userId, Long relationshipId) {
        Relationship rel = getRelationshipAndValidate(relationshipId, userId);

        RestoreRequest request = restoreRequestRepository.findByRelationshipIdAndStatus(
                        relationshipId, RestoreRequestStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        request.respond(RestoreRequestStatus.ACCEPTED);

        // PILYEON으로 복귀 — 무기한 유지 (D11)
        rel.updateStatusWithExpiry(RelationshipStatus.PILYEON, null);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        saveLog(rel, RelationshipStatus.PAST, RelationshipStatus.PILYEON, RelationshipTrigger.RESTORE, user);

        return RestoreAcceptResponse.builder()
                .status("PILYEON")
                .message("복원이 완료되었습니다")
                .build();
    }

    // --- 5-10. 복원 거절 ---
    @Transactional
    public void rejectRestore(Long userId, Long relationshipId) {
        Relationship rel = getRelationshipAndValidate(relationshipId, userId);

        RestoreRequest request = restoreRequestRepository.findByRelationshipIdAndStatus(
                        relationshipId, RestoreRequestStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        request.respond(RestoreRequestStatus.REJECTED);

        // ENDED로 완전 종료
        rel.updateStatus(RelationshipStatus.ENDED);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        saveLog(rel, RelationshipStatus.PAST, RelationshipStatus.ENDED, RelationshipTrigger.REJECT, user);
    }

    // --- 5-11. 2번째 조우 장소 조회 ---
    public EncounterLocationResponse getEncounterLocation(Long userId, Long relationshipId) {
        Relationship rel = getRelationshipAndValidate(relationshipId, userId);

        if (rel.getStatus() != RelationshipStatus.INYEON) {
            throw new BusinessException(ErrorCode.NOT_INYEON);
        }

        // 2번째 조우 (encounterNumber = 2)
        Encounter encounter = encounterRepository.findByRelationshipIdAndEncounterNumber(relationshipId, 2)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENCOUNTER_NOT_FOUND));

        // 위도/경도 100m 단위 라운딩 (소수점 3자리)
        double roundedLat = Math.round(encounter.getLatitude() * 1000.0) / 1000.0;
        double roundedLng = Math.round(encounter.getLongitude() * 1000.0) / 1000.0;

        return EncounterLocationResponse.builder()
                .latitude(roundedLat)
                .longitude(roundedLng)
                .encounterAt(encounter.getCreatedAt())
                .build();
    }

    // --- 5-12. 우연→필연 전환 ---
    @Transactional
    public ConvertResponse convertToPilyeon(Long userId, Long relationshipId) {
        Relationship rel = getRelationshipAndValidate(relationshipId, userId);

        if (rel.getStatus() != RelationshipStatus.UYEON) {
            throw new BusinessException(ErrorCode.NOT_UYEON);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 무료 전환 횟수 확인 및 매듭 차감
        MonthlyUsage usage = getOrCreateMonthlyUsage(user);
        int freeLimit = user.getIsPremium() ? PREMIUM_CONVERTS_PER_MONTH : FREE_CONVERTS_PER_MONTH;
        int knotsUsed = 0;

        if (usage.getFreeConvertsUsed() < freeLimit) {
            // 무료 횟수 사용
            usage.incrementFreeConvertsUsed();
        } else {
            // 매듭 차감
            if (user.getKnots() < KNOTS_COST) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_KNOTS);
            }
            user.deductKnots(KNOTS_COST);
            knotsUsed = KNOTS_COST;
        }

        // 필연은 무기한 유지 (D11)
        rel.updateStatusWithExpiry(RelationshipStatus.PILYEON, null);

        saveLog(rel, RelationshipStatus.UYEON, RelationshipStatus.PILYEON, RelationshipTrigger.REQUEST, user);

        return ConvertResponse.builder()
                .status("PILYEON")
                .message("필연으로 전환되었습니다")
                .knotsUsed(knotsUsed)
                .remainingKnots(user.getKnots())
                .build();
    }

    // --- Helper Methods ---

    private MonthlyUsage getOrCreateMonthlyUsage(User user) {
        String yearMonth = YearMonth.now().toString();
        return monthlyUsageRepository.findByUserUserIdAndYearMonth(user.getUserId(), yearMonth)
                .orElseGet(() -> monthlyUsageRepository.save(
                        MonthlyUsage.builder()
                                .user(user)
                                .yearMonth(yearMonth)
                                .build()
                ));
    }

    private Relationship getRelationshipAndValidate(Long relationshipId, Long userId) {
        Relationship rel = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RELATIONSHIP_NOT_FOUND));
        if (!rel.isParticipant(userId)) {
            throw new BusinessException(ErrorCode.NOT_RELATIONSHIP_MEMBER);
        }
        return rel;
    }

    private String getRepresentativePhotoUrl(Long userId) {
        return userPhotoRepository.findByUserUserIdOrderByDisplayOrderAsc(userId).stream()
                .filter(UserPhoto::getIsRepresentative)
                .findFirst()
                .map(UserPhoto::getPhotoUrl)
                .orElse(null);
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
