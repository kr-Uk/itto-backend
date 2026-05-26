package com.itto.backend.application.payment;

import com.itto.backend.presentation.dto.payment.*;
import com.itto.backend.presentation.dto.payment.PackageListResponse.PackageInfo;
import com.itto.backend.presentation.dto.payment.SubscriptionResponse.SubscriptionBenefits;
import com.itto.backend.domain.payment.entity.*;
import com.itto.backend.domain.payment.repository.*;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.domain.common.constants.BusinessConstants;
import com.itto.backend.domain.common.enums.KnotTransactionType;
import com.itto.backend.domain.common.enums.Store;
import com.itto.backend.domain.common.enums.SubscriptionPlan;
import com.itto.backend.domain.common.exception.BusinessException;
import com.itto.backend.domain.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MonthlyUsageRepository monthlyUsageRepository;
    private final FirstPurchaseFlagRepository firstPurchaseFlagRepository;
    private final PurchaseRepository purchaseRepository;
    private final KnotTransactionRepository knotTransactionRepository;
    private final ReceiptValidationService receiptValidationService;

    // 패키지 목록 (BusinessConstants 참조)
    private static final List<PackageData> PACKAGES = List.of(
            new PackageData(1L, "실마리", BusinessConstants.SILMARI_PRICE, BusinessConstants.SILMARI_KNOTS, BusinessConstants.SILMARI_FIRST_KNOTS),
            new PackageData(2L, "매듭 뭉치", BusinessConstants.MAEDUP_PRICE, BusinessConstants.MAEDUP_KNOTS, BusinessConstants.MAEDUP_FIRST_KNOTS),
            new PackageData(3L, "실타래", BusinessConstants.SILTARAE_PRICE, BusinessConstants.SILTARAE_KNOTS, BusinessConstants.SILTARAE_FIRST_KNOTS)
    );

    // 무료 횟수 (비프리미엄 기준)
    private static final int FREE_CONVERTS_PER_MONTH = BusinessConstants.FREE_CONVERTS_PER_MONTH;
    private static final int FREE_REQUESTS_PER_MONTH = BusinessConstants.FREE_REQUESTS_PER_MONTH;
    private static final int FREE_RESTORES_PER_MONTH = BusinessConstants.FREE_RESTORES_PER_MONTH;

    // 프리미엄 혜택
    private static final int PREMIUM_CONVERTS_PER_MONTH = BusinessConstants.PREMIUM_CONVERTS_PER_MONTH;
    private static final int PREMIUM_REQUESTS_PER_MONTH = BusinessConstants.PREMIUM_REQUESTS_PER_MONTH;
    private static final int PREMIUM_RESTORES_PER_MONTH = BusinessConstants.PREMIUM_RESTORES_PER_MONTH;
    private static final boolean PREMIUM_AD_FREE = BusinessConstants.PREMIUM_AD_FREE;

    /**
     * 매듭 잔액 및 무료 횟수 조회
     */
    public KnotsBalanceResponse getKnotsBalance(Long userId) {
        User user = getUserOrThrow(userId);

        String currentYearMonth = YearMonth.now().toString();
        MonthlyUsage monthlyUsage = monthlyUsageRepository
                .findByUserUserIdAndYearMonth(userId, currentYearMonth)
                .orElse(null);

        int freeConvertsUsed = monthlyUsage != null ? monthlyUsage.getFreeConvertsUsed() : 0;
        int freeRequestsUsed = monthlyUsage != null ? monthlyUsage.getFreeRequestsUsed() : 0;
        int freeRestoresUsed = monthlyUsage != null ? monthlyUsage.getFreeRestoresUsed() : 0;

        // 프리미엄 여부에 따라 무료 횟수 계산
        int freeConvertsLimit = user.getIsPremium() ? PREMIUM_CONVERTS_PER_MONTH : FREE_CONVERTS_PER_MONTH;
        int freeRequestsLimit = user.getIsPremium() ? PREMIUM_REQUESTS_PER_MONTH : FREE_REQUESTS_PER_MONTH;
        int freeRestoresLimit = user.getIsPremium() ? PREMIUM_RESTORES_PER_MONTH : FREE_RESTORES_PER_MONTH;

        return KnotsBalanceResponse.builder()
                .knots(user.getKnots())
                .freeConvertRemaining(Math.max(0, freeConvertsLimit - freeConvertsUsed))
                .freeRequestRemaining(Math.max(0, freeRequestsLimit - freeRequestsUsed))
                .freeRestoreRemaining(Math.max(0, freeRestoresLimit - freeRestoresUsed))
                .build();
    }

    /**
     * 매듭 패키지 목록 조회
     */
    public PackageListResponse getPackages(Long userId) {
        List<PackageInfo> packageInfos = new ArrayList<>();

        for (PackageData pkg : PACKAGES) {
            boolean isFirstPurchaseUsed = firstPurchaseFlagRepository
                    .existsByUserUserIdAndPackageName(userId, pkg.name);

            packageInfos.add(PackageInfo.builder()
                    .id(pkg.id)
                    .name(pkg.name)
                    .price(pkg.price)
                    .knots(pkg.knots)
                    .firstPurchaseKnots(pkg.firstPurchaseKnots)
                    .isFirstPurchaseUsed(isFirstPurchaseUsed)
                    .build());
        }

        return PackageListResponse.builder()
                .packages(packageInfos)
                .build();
    }

    /**
     * 매듭 구매
     */
    @Transactional
    public PurchaseResponse purchase(Long userId, PurchaseRequest request) {
        User user = getUserOrThrow(userId);

        // 패키지 정보 조회
        PackageData packageData = PACKAGES.stream()
                .filter(pkg -> pkg.id.equals(request.getPackageId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 영수증 검증
        ReceiptValidationService.ReceiptValidationResult validationResult =
                receiptValidationService.validatePurchaseReceipt(request.getStore(), request.getReceiptToken());

        if (!validationResult.isValid()) {
            throw new BusinessException(ErrorCode.INVALID_RECEIPT);
        }

        // 첫 구매 여부 확인
        boolean isFirstPurchase = !firstPurchaseFlagRepository
                .existsByUserUserIdAndPackageName(userId, packageData.name);

        // 매듭 지급량 계산
        int knotsGranted = isFirstPurchase ? packageData.firstPurchaseKnots : packageData.knots;

        // 매듭 지급
        user.addKnots(knotsGranted);
        userRepository.save(user);

        // 구매 기록 저장
        Purchase purchase = Purchase.builder()
                .user(user)
                .packageName(packageData.name)
                .price(packageData.price)
                .knotsGranted(knotsGranted)
                .isFirstPurchase(isFirstPurchase)
                .store(Store.valueOf(request.getStore()))
                .receiptToken(request.getReceiptToken())
                .receiptValidated(validationResult.isValid())
                .build();
        purchaseRepository.save(purchase);

        // 첫 구매 플래그 저장
        if (isFirstPurchase) {
            FirstPurchaseFlag flag = FirstPurchaseFlag.builder()
                    .user(user)
                    .packageName(packageData.name)
                    .build();
            firstPurchaseFlagRepository.save(flag);

            // 첫 구매 보너스 트랜잭션 기록
            recordKnotTransaction(user, KnotTransactionType.FIRST_PURCHASE_BONUS,
                    knotsGranted, user.getKnots(),
                    purchase.getId().toString(), "첫 구매 보너스: " + packageData.name);
        } else {
            // 일반 구매 트랜잭션 기록
            recordKnotTransaction(user, KnotTransactionType.PURCHASE,
                    knotsGranted, user.getKnots(),
                    purchase.getId().toString(), "매듭 구매: " + packageData.name);
        }

        return PurchaseResponse.builder()
                .knotsAdded(knotsGranted)
                .totalKnots(user.getKnots())
                .isFirstPurchaseBonus(isFirstPurchase)
                .message("구매가 완료되었습니다")
                .build();
    }

    /**
     * 구독 상태 조회
     */
    public SubscriptionResponse getSubscription(Long userId) {
        User user = getUserOrThrow(userId);

        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .findByUserUserIdAndIsActiveTrue(userId);

        if (subscriptionOpt.isEmpty() || !user.getIsPremium()) {
            // 구독 없음 또는 비프리미엄 상태
            return null;
        }

        Subscription subscription = subscriptionOpt.get();

        return SubscriptionResponse.builder()
                .isPremium(true)
                .plan(subscription.getPlan().name())
                .price(subscription.getPrice())
                .startedAt(subscription.getStartedAt())
                .expiresAt(subscription.getExpiresAt())
                .isPromotion(subscription.getIsPromotion())
                .benefits(SubscriptionBenefits.builder()
                        .convertPerMonth(PREMIUM_CONVERTS_PER_MONTH)
                        .unlimitedRequests(false)
                        .requestsPerMonth(PREMIUM_REQUESTS_PER_MONTH)
                        .freeRestorePerMonth(PREMIUM_RESTORES_PER_MONTH)
                        .adFree(PREMIUM_AD_FREE)
                        .build())
                .build();
    }

    /**
     * 구독 시작
     */
    @Transactional
    public SubscriptionStartResponse startSubscription(Long userId, SubscriptionStartRequest request) {
        User user = getUserOrThrow(userId);

        // 이미 활성 구독이 있는지 확인
        Optional<Subscription> existingSubscription = subscriptionRepository
                .findByUserUserIdAndIsActiveTrue(userId);

        if (existingSubscription.isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        // 영수증 검증
        ReceiptValidationService.ReceiptValidationResult validationResult =
                receiptValidationService.validateSubscriptionReceipt(request.getStore(), request.getReceiptToken());

        if (!validationResult.isValid()) {
            throw new BusinessException(ErrorCode.INVALID_RECEIPT);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMonths(1);

        // 구독 정보 저장
        Subscription subscription = Subscription.builder()
                .user(user)
                .store(Store.valueOf(request.getStore()))
                .storeSubscriptionId(validationResult.getTransactionId()) // 검증된 transactionId 사용
                .plan(SubscriptionPlan.MONTHLY)
                .price(BusinessConstants.SUBSCRIPTION_LAUNCH_PRICE)
                .isPromotion(false) // 프로모션 시스템 미구현 — 기본값 false
                .startedAt(now)
                .expiresAt(expiresAt)
                .isActive(true)
                .build();
        subscriptionRepository.save(subscription);

        // 사용자 프리미엄 상태 업데이트
        user.updatePremium(true);
        userRepository.save(user);

        return SubscriptionStartResponse.builder()
                .isPremium(true)
                .expiresAt(expiresAt)
                .message("프리미엄 구독이 시작되었습니다")
                .build();
    }

    /**
     * 구독 해지
     */
    @Transactional
    public SubscriptionCancelResponse cancelSubscription(Long userId) {
        User user = getUserOrThrow(userId);

        Subscription subscription = subscriptionRepository
                .findByUserUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 구독 해지 (만료일까지는 유지)
        subscription.cancel();
        subscriptionRepository.save(subscription);

        // 주의: User.isPremium은 만료일까지 유지 (별도 스케줄러에서 만료 시점에 false로 변경)

        return SubscriptionCancelResponse.builder()
                .message("구독이 해지되었습니다. 만료일까지 혜택이 유지됩니다")
                .expiresAt(subscription.getExpiresAt())
                .build();
    }

    // ========== Helper Methods ==========

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void recordKnotTransaction(User user, KnotTransactionType type, int amount,
                                       int balanceAfter, String referenceId, String description) {
        KnotTransaction transaction = KnotTransaction.builder()
                .user(user)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .referenceId(referenceId)
                .description(description)
                .build();
        knotTransactionRepository.save(transaction);
    }

    // 내부용 패키지 데이터 클래스
    private static class PackageData {
        Long id;
        String name;
        Integer price;
        Integer knots;
        Integer firstPurchaseKnots;

        PackageData(Long id, String name, Integer price, Integer knots, Integer firstPurchaseKnots) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.knots = knots;
            this.firstPurchaseKnots = firstPurchaseKnots;
        }
    }
}
