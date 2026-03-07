# Backend Clean Architecture Refactoring Plan

## Progress

### Phase 1 (In Progress)
- [x] Created `domain/common/constants/BusinessConstants.java` — centralized all magic numbers
- [x] Updated 6 services to use `BusinessConstants` (RelationshipService, EncounterBatchService, LocationService, ShopService, ContactHashService, AuthService)
- [x] Verified `compileJava` passes
- [ ] Package moves (global/entity -> domain/common/entity, etc.) — requires IDE refactoring for safe import updates across 100+ files

### Phase 2 (In Progress)
- [x] Created 5 port interfaces in `application/` layer:
  - `auth/port/RefreshTokenPort` — Redis refresh token abstraction
  - `location/port/LocationCachePort` — Redis location caching abstraction
  - `encounter/port/LocationDataPort` — Redis encounter data access abstraction
  - `notification/port/PushNotificationPort` — FCM push abstraction
  - `payment/port/ReceiptValidationPort` — Receipt validation abstraction
- [x] Created 3 infrastructure adapters in `infrastructure/redis/`:
  - `RedisRefreshTokenAdapter` (implements RefreshTokenPort)
  - `RedisLocationCacheAdapter` (implements LocationCachePort)
  - `RedisLocationDataAdapter` (implements LocationDataPort)
- [x] Verified `compileJava` passes
- [ ] Wire adapters into existing services (replace direct StringRedisTemplate usage)
- [ ] Extract use cases from services
- [ ] Move OAuth, NICE, Firebase, MinIO configs to `infrastructure/`

### Pre-existing Test Issues
- 10 test files reference `User.builder().firebaseUid()` which no longer exists in User entity
- These test compilation errors predate this refactoring

---

## 1. Current Structure Analysis

### 1.1 Current Package Layout

```
com.itto.backend/
├── BackendApplication.java
├── controller/
│   └── HealthController.java
├── domain/                          # Feature-based packages (mixed layers)
│   ├── admin/     {controller, dto, service}
│   ├── auth/      {config, controller, dto, service, service/oauth}
│   ├── block/     {controller, dto, entity, repository, service}
│   ├── chat/      {entity, repository}
│   ├── contact/   {controller, dto, entity, repository, service, util}
│   ├── hobby/     {entity, repository}
│   ├── identity/  {config, controller, crypto, dto, entity, repository, service}
│   ├── location/  {controller, dto, entity, repository, service}
│   ├── notification/ {controller, dto, entity, repository, service}
│   ├── payment/   {controller, dto, entity, repository, scheduler, service}
│   ├── profile/   {controller, dto, service}
│   ├── relationship/ {controller, dto, entity, repository, scheduler, service}
│   ├── settings/  {controller, dto, service}
│   ├── signup/    {controller, dto, service}
│   ├── system/    {controller, dto, entity, repository, service}
│   └── user/      {entity, repository}
└── global/
    ├── config/    {Firebase, JpaAuditing, Minio, Scheduling, Security, Swagger}
    ├── dto/       {ApiResponse}
    ├── entity/    {BaseEntity, BaseTimeEntity}
    ├── enums/     {Gender, InyeonRequestStatus, KnotTransactionType, ...}
    ├── exception/ {BusinessException, ErrorCode, GlobalExceptionHandler}
    ├── security/  {JwtAuthFilter, JwtProperties, JwtProvider}
    └── service/   {MinioStorageService, StorageService}
```

### 1.2 Issues Identified

#### SRP Violations
| Service | Lines | Issue |
|---------|-------|-------|
| `RelationshipService` | 510 | Handles listing, detail, inyeon request/accept/reject/cancel, delete, restore, convert - 12 distinct operations mixing query and command |
| `EncounterBatchService` | 538 | Contains Redis access, H3 calculation, matching filter, distance calculation, relationship state machine, notification dispatch, expiration processing |
| `LocationService` | 353 | Manages Redis location storage, H3 grid computation, excluded area CRUD - 3 separate concerns |
| `ShopService` | 324 | Mixes package listing, purchase processing, subscription management, receipt validation orchestration |
| `SignupService` | 192 | Handles terms, profile, photos, verification, completion - 5 separate steps |

#### Cross-Domain Coupling
| From | To | Via |
|------|----|-----|
| `RelationshipService` | `payment` | MonthlyUsageRepository (free usage check) |
| `RelationshipService` | `user`, `hobby` | UserPhotoRepository, UserHobbyRepository (profile assembly) |
| `EncounterBatchService` | `block`, `location`, `notification`, `user` | Direct repository/service dependencies |
| `ShopService` | `user` | User.addKnots(), User.updatePremium() |
| `SignupService` | `hobby`, `user` | HobbyRepository, multiple user repositories |

#### Infrastructure Leakage
- `EncounterBatchService` directly uses `StringRedisTemplate` (Redis keys, SCAN, Hash operations)
- `LocationService` directly uses `StringRedisTemplate` (location caching)
- `AuthService` directly uses `StringRedisTemplate` (refresh token storage)
- `H3Core` instantiated inline in `LocationService` and `EncounterBatchService`

#### Magic Numbers & Duplicated Constants
- `KNOTS_COST = 30` in `RelationshipService` (also implied in `requestRestore`)
- `FREE_REQUESTS_PER_MONTH`, `PREMIUM_REQUESTS_PER_MONTH` duplicated between `RelationshipService` and `ShopService`
- Package prices hardcoded in `ShopService.PACKAGES`
- `ENCOUNTER_DISTANCE_METERS = 100.0`, `COOLDOWN_MINUTES = 30` in `EncounterBatchService`

---

## 2. Target Structure

```
com.itto.backend/
├── domain/                          # Enterprise Business Rules (pure Java, no Spring)
│   ├── user/
│   │   ├── entity/                  User, UserPhoto, UserSetting, UserTerm, FcmToken, Verification
│   │   ├── repository/              UserRepository, UserPhotoRepository, ... (interfaces)
│   │   └── vo/                      (value objects if needed)
│   ├── relationship/
│   │   ├── entity/                  Relationship, Encounter, InyeonRequest, RestoreRequest, RelationshipLog
│   │   ├── repository/              RelationshipRepository, EncounterRepository, ...
│   │   └── vo/                      EncounterDistance, RelationshipStatusTransition
│   ├── encounter/
│   │   ├── entity/                  (shared with relationship for now)
│   │   └── repository/              (port interface for location data access)
│   ├── payment/
│   │   ├── entity/                  Purchase, Subscription, KnotTransaction, MonthlyUsage, FirstPurchaseFlag
│   │   ├── repository/              PurchaseRepository, SubscriptionRepository, ...
│   │   └── vo/                      KnotsPackage, SubscriptionBenefits
│   ├── contact/
│   │   ├── entity/                  ContactHash
│   │   ├── repository/              ContactHashRepository
│   │   └── util/                    PhoneNumberHashUtil
│   ├── location/
│   │   ├── entity/                  ExcludedArea, ExcludedAreaH3Index
│   │   └── repository/              ExcludedAreaRepository, ExcludedAreaH3IndexRepository
│   ├── notification/
│   │   ├── entity/                  Notification
│   │   └── repository/              NotificationRepository
│   ├── block/
│   │   ├── entity/                  Block, Report
│   │   └── repository/              BlockRepository, ReportRepository
│   ├── chat/
│   │   ├── entity/                  ChatRoom
│   │   └── repository/              ChatRoomRepository
│   ├── identity/
│   │   ├── entity/                  IdentityVerification
│   │   └── repository/              IdentityVerificationRepository
│   ├── hobby/
│   │   ├── entity/                  Hobby, UserHobby
│   │   └── repository/              HobbyRepository, UserHobbyRepository
│   ├── system/
│   │   ├── entity/                  AppVersion, ProfanityWord
│   │   └── repository/              AppVersionRepository, ProfanityWordRepository
│   └── common/
│       ├── entity/                  BaseEntity, BaseTimeEntity
│       ├── enums/                   Gender, RelationshipStatus, ... (all enums)
│       ├── exception/               BusinessException, ErrorCode
│       └── constants/               BusinessConstants (centralized magic numbers)
│
├── application/                     # Application Business Rules (Use Cases)
│   ├── auth/
│   │   ├── usecase/                 LoginUseCase, RefreshUseCase, LogoutUseCase, WithdrawUseCase
│   │   ├── dto/                     LoginCommand, LoginResult, RefreshCommand, RefreshResult
│   │   └── port/                    RefreshTokenPort (interface for Redis)
│   ├── signup/
│   │   ├── usecase/                 SaveTermsUseCase, SaveProfileUseCase, UploadPhotosUseCase,
│   │   │                            UploadVerificationUseCase, CompleteSignupUseCase
│   │   └── dto/                     (command/result DTOs)
│   ├── relationship/
│   │   ├── usecase/                 GetRelationshipsUseCase, GetRelationshipDetailUseCase,
│   │   │                            RequestInyeonUseCase, AcceptInyeonUseCase, RejectInyeonUseCase,
│   │   │                            CancelInyeonUseCase, DeleteRelationshipUseCase,
│   │   │                            RequestRestoreUseCase, AcceptRestoreUseCase, RejectRestoreUseCase,
│   │   │                            ConvertToPilyeonUseCase, GetEncounterLocationUseCase
│   │   ├── facade/                  RelationshipFacade (thin orchestration if needed)
│   │   └── dto/
│   ├── encounter/
│   │   ├── usecase/                 DetectEncountersUseCase, ProcessExpirationsUseCase
│   │   ├── port/                    LocationDataPort (interface for Redis location access)
│   │   └── dto/
│   ├── location/
│   │   ├── usecase/                 SendLocationUseCase, ToggleLocationUseCase,
│   │   │                            CreateExcludedAreaUseCase, DeleteExcludedAreaUseCase,
│   │   │                            GetH3GridUseCase, SaveExcludedCellsUseCase
│   │   ├── port/                    LocationCachePort (interface for Redis)
│   │   └── dto/
│   ├── payment/
│   │   ├── usecase/                 GetKnotsBalanceUseCase, GetPackagesUseCase, PurchaseKnotsUseCase,
│   │   │                            GetSubscriptionUseCase, StartSubscriptionUseCase, CancelSubscriptionUseCase
│   │   ├── port/                    ReceiptValidationPort
│   │   └── dto/
│   ├── notification/
│   │   ├── usecase/                 GetNotificationsUseCase, MarkAsReadUseCase, DeleteAllNotificationsUseCase,
│   │   │                            RegisterFcmTokenUseCase, CreateAndPushNotificationUseCase
│   │   ├── port/                    PushNotificationPort (interface for FCM)
│   │   └── dto/
│   ├── profile/
│   │   ├── usecase/                 GetMyProfileUseCase, GetOtherProfileUseCase, UpdateProfileUseCase
│   │   └── dto/
│   ├── block/
│   │   ├── usecase/                 BlockUserUseCase, UnblockUserUseCase, ReportUserUseCase,
│   │   │                            GetBlockListUseCase
│   │   └── dto/
│   ├── contact/
│   │   ├── usecase/                 UploadHashesUseCase, AddManualHashUseCase, RemoveHashUseCase,
│   │   │                            GetHashesUseCase, GetMutualBlocksUseCase
│   │   └── dto/
│   ├── settings/
│   │   ├── usecase/                 GetMatchingSettingsUseCase, UpdateMatchingSettingsUseCase,
│   │   │                            GetNotificationSettingsUseCase, UpdateNotificationSettingsUseCase
│   │   └── dto/
│   ├── admin/
│   │   ├── usecase/                 GetVerificationListUseCase, ApproveVerificationUseCase, RejectVerificationUseCase
│   │   └── dto/
│   ├── identity/
│   │   ├── usecase/                 RequestIdentityUseCase, GetIdentityStatusUseCase, ProcessCallbackUseCase
│   │   └── dto/
│   └── system/
│       ├── usecase/                 ValidateNicknameUseCase, GetHobbiesUseCase, GetAppVersionUseCase
│       └── dto/
│
├── infrastructure/                  # Frameworks & Drivers
│   ├── persistence/                 # JPA Repository implementations
│   │   ├── user/                    JpaUserRepository, JpaUserPhotoRepository, ...
│   │   ├── relationship/            JpaRelationshipRepository, JpaEncounterRepository, ...
│   │   ├── payment/                 JpaPurchaseRepository, ...
│   │   └── .../                     (mirrors domain repos)
│   ├── redis/
│   │   ├── RedisRefreshTokenAdapter.java    (implements RefreshTokenPort)
│   │   ├── RedisLocationCacheAdapter.java   (implements LocationCachePort)
│   │   └── RedisLocationDataAdapter.java    (implements LocationDataPort)
│   ├── firebase/
│   │   ├── FirebaseConfig.java
│   │   └── FcmPushNotificationAdapter.java  (implements PushNotificationPort)
│   ├── minio/
│   │   ├── MinioConfig.java
│   │   └── MinioStorageAdapter.java         (implements StoragePort)
│   ├── nice/
│   │   ├── NiceProperties.java
│   │   ├── NiceCryptoUtil.java
│   │   └── NiceIdentityAdapter.java
│   ├── receipt/
│   │   └── ReceiptValidationAdapter.java    (implements ReceiptValidationPort)
│   ├── oauth/
│   │   ├── OAuthProperties.java
│   │   ├── AppleOAuthVerifier.java
│   │   ├── GoogleOAuthVerifier.java
│   │   ├── KakaoOAuthVerifier.java
│   │   └── OAuthVerifierFactory.java
│   └── config/
│       ├── JpaAuditingConfig.java
│       ├── SchedulingConfig.java
│       ├── SecurityConfig.java
│       ├── SwaggerConfig.java
│       └── RestTemplateConfig.java
│
├── presentation/                    # Interface Adapters
│   ├── api/                         # REST Controllers
│   │   ├── HealthController.java
│   │   ├── AuthController.java
│   │   ├── SignupController.java
│   │   ├── RelationshipController.java
│   │   ├── LocationController.java
│   │   ├── ShopController.java
│   │   ├── ProfileController.java
│   │   ├── NotificationController.java
│   │   ├── ContactHashController.java
│   │   ├── BlockController.java
│   │   ├── SettingsController.java
│   │   ├── IdentityController.java
│   │   ├── SystemController.java
│   │   └── AdminController.java
│   ├── dto/                         # Request/Response DTOs (presentation layer)
│   │   ├── auth/                    LoginRequest, LoginResponse, ...
│   │   ├── relationship/            RelationshipListResponse, ...
│   │   └── .../                     (all current DTOs move here)
│   ├── scheduler/
│   │   ├── EncounterScheduler.java
│   │   └── SubscriptionScheduler.java
│   └── security/
│       ├── JwtAuthFilter.java
│       ├── JwtProperties.java
│       └── JwtProvider.java
│
└── global/                          # Cross-cutting (minimal)
    └── dto/
        └── ApiResponse.java
```

---

## 3. Refactoring Phases

### Phase 1: Domain Layer Separation (Low Risk)

**Goal**: Move entities, repository interfaces, enums, and base classes into the clean `domain/` package without changing any logic.

**Order** (by dependency — least dependent first):

| Step | Domain | Files to Move | Dependencies |
|------|--------|---------------|--------------|
| 1 | `common` | BaseEntity, BaseTimeEntity, all enums, BusinessException, ErrorCode | None |
| 2 | `user` | User, UserPhoto, UserSetting, UserTerm, FcmToken, Verification + repos | common |
| 3 | `hobby` | Hobby, UserHobby + repos | common, user |
| 4 | `block` | Block, Report + repos | common, user |
| 5 | `chat` | ChatRoom + repo | common, user |
| 6 | `contact` | ContactHash + repo + PhoneNumberHashUtil | common, user |
| 7 | `system` | AppVersion, ProfanityWord + repos | common |
| 8 | `notification` | Notification + repo | common, user |
| 9 | `location` | ExcludedArea, ExcludedAreaH3Index + repos | common, user |
| 10 | `identity` | IdentityVerification + repo | common, user |
| 11 | `payment` | Purchase, Subscription, KnotTransaction, MonthlyUsage, FirstPurchaseFlag + repos | common, user |
| 12 | `relationship` | Relationship, Encounter, InyeonRequest, RestoreRequest, RelationshipLog + repos | common, user |

**Key rule**: Repository interfaces stay in domain. JPA implementations go to infrastructure (Phase 2).

**Centralized Constants** (`domain/common/constants/BusinessConstants.java`):
```java
public final class BusinessConstants {
    // Encounter
    public static final double ENCOUNTER_DISTANCE_METERS = 100.0;
    public static final int ENCOUNTER_COOLDOWN_MINUTES = 30;
    public static final int H3_RESOLUTION = 9;

    // Relationship monetization
    public static final int KNOTS_COST = 30;
    public static final int FREE_REQUESTS_PER_MONTH = 1;
    public static final int PREMIUM_REQUESTS_PER_MONTH = 6;
    public static final int FREE_CONVERTS_PER_MONTH = 1;
    public static final int PREMIUM_CONVERTS_PER_MONTH = 6;
    public static final int FREE_RESTORES_PER_MONTH = 0;
    public static final int PREMIUM_RESTORES_PER_MONTH = 3;
    public static final int RESTORE_KNOTS_COST = 30;

    // Signup
    public static final int MAX_PHOTOS = 3;
    public static final int MIN_AGE = 19;

    // Contact hash
    public static final int MAX_HASH_COUNT = 5000;

    // Location
    public static final int MAX_EXCLUDED_AREAS = 3;
    public static final int MAX_EXCLUDED_CELLS = 20;

    // Packages
    public static final int SILMARI_PRICE = 7900;
    public static final int SILMARI_KNOTS = 80;
    // ... etc
}
```

### Phase 2: Application & Infrastructure Layer

**Goal**: Extract use cases from services, define port interfaces, move infrastructure adapters.

#### 2A: Port Interfaces (application layer)

| Port | Methods | Implementations |
|------|---------|-----------------|
| `RefreshTokenPort` | `save(userId, token, ttl)`, `get(userId)`, `delete(userId)` | `RedisRefreshTokenAdapter` |
| `LocationCachePort` | `saveLocation(userId, h3Index, lat, lng)`, `removeLocation(userId)`, `getLocation(userId)` | `RedisLocationCacheAdapter` |
| `LocationDataPort` | `getActiveH3Cells()`, `getUserIdsInCell(h3Index)`, `getUserLocation(userId)`, `setCooldown(a,b)`, `isOnCooldown(a,b)` | `RedisLocationDataAdapter` |
| `PushNotificationPort` | `send(userId, title, body, data)` | `FcmPushNotificationAdapter` |
| `StoragePort` | `upload(file, path)`, `delete(url)` | `MinioStorageAdapter` (already exists as interface) |
| `ReceiptValidationPort` | `validatePurchase(store, token)`, `validateSubscription(store, token)` | `ReceiptValidationAdapter` |
| `H3GeoPort` | `latLngToCell(lat, lng, res)`, `gridDisk(cell, k)`, `cellToLatLng(cell)`, `cellToBoundary(cell)` | `H3Adapter` |

#### 2B: Use Case Extraction (key examples)

**EncounterBatchService decomposition**:
- `DetectEncountersUseCase` — orchestrates encounter detection using ports
- `ProcessExpirationsUseCase` — handles relationship expiration
- Domain logic (matching filter, distance calc, state transitions) moves to entity/domain service

**RelationshipService decomposition**:
- Query use cases: `GetRelationshipsUseCase`, `GetRelationshipDetailUseCase`, `GetEncounterLocationUseCase`
- Command use cases: `RequestInyeonUseCase`, `AcceptInyeonUseCase`, `RejectInyeonUseCase`, `CancelInyeonUseCase`, `DeleteRelationshipUseCase`, `RequestRestoreUseCase`, `AcceptRestoreUseCase`, `RejectRestoreUseCase`, `ConvertToPilyeonUseCase`
- Shared domain logic: `MonthlyUsageService` (manages free usage checks + knot deduction)

**LocationService decomposition**:
- `SendLocationUseCase` — uses `LocationCachePort` + `H3GeoPort`
- `ToggleLocationUseCase`
- Excluded area CRUD: `CreateExcludedAreaUseCase`, `DeleteExcludedAreaUseCase`, `GetExcludedAreasUseCase`
- H3 grid: `GetH3GridUseCase`, `SaveExcludedCellsUseCase`

#### 2C: Infrastructure Adapters

Move from current location to `infrastructure/`:
- `global/config/FirebaseConfig` -> `infrastructure/firebase/`
- `global/config/MinioConfig` -> `infrastructure/minio/`
- `global/service/MinioStorageService` -> `infrastructure/minio/MinioStorageAdapter`
- `domain/auth/config/OAuthProperties` -> `infrastructure/oauth/`
- `domain/auth/service/oauth/*` -> `infrastructure/oauth/`
- `domain/identity/config/*` -> `infrastructure/nice/`
- `domain/identity/crypto/*` -> `infrastructure/nice/`
- `domain/payment/service/ReceiptValidationService` -> `infrastructure/receipt/`

### Phase 3: Presentation Layer

**Goal**: Move controllers, request/response DTOs, schedulers, and security filters.

- All controllers -> `presentation/api/`
- All DTOs -> `presentation/dto/<feature>/`
- Schedulers -> `presentation/scheduler/`
- JWT security -> `presentation/security/`

---

## 4. Domain Logic Enrichment

Move business logic from services into entities where appropriate:

### Relationship Entity
```java
// Already has: updateStatus(), updateStatusWithExpiry(), recordEncounter(), markDestinyBadge()
// Add: canRequestInyeon(), canConvert(), canRestore() — state validation
```

### User Entity
```java
// Already has: deductKnots(), addKnots(), updatePremium()
// Add: hasSufficientKnots(amount), isPremiumUser() — guard methods
```

### MonthlyUsage Entity
```java
// Add: canUseFreeRequest(isPremium), useFreeRequest()
// Add: canUseFreeConvert(isPremium), useFreeConvert()
```

---

## 5. Migration Strategy

### Approach: Incremental Rename with Backward Compatibility

Each phase follows this process:
1. Create new package structure
2. Move files to new packages (IntelliJ refactor or manual)
3. Update imports across all files
4. Run existing tests to verify nothing broke
5. Commit

### Test Strategy
- All 17 existing test files must pass after each phase
- No new tests required during refactoring (structure change only)
- Phase 2 may require test adjustments for new class names/packages

### Risk Mitigation
- **Single feature branch**: `refactor/backend-clean-arch`
- **Atomic commits per step**: e.g., "Move common entities to domain/common"
- **No logic changes in Phase 1**: Pure package moves
- **Phase 2 logic extraction**: Each use case extraction verified by existing tests

---

## 6. File Count Summary

| Layer | Current | Target | Notes |
|-------|---------|--------|-------|
| Domain (entities+repos+enums) | ~65 files scattered | ~65 files organized | Same files, clean packages |
| Application (use cases) | 0 (logic in services) | ~50 use case classes | Extracted from ~15 services |
| Infrastructure (adapters) | ~10 files scattered | ~15 adapter files | + port interfaces |
| Presentation (controllers+DTOs) | ~45 files scattered | ~45 files organized | Same files, clean packages |
| Global (cross-cutting) | ~15 files | ~3 files | Most move to infra/presentation |

---

## 7. Dependency Rules

```
presentation -> application -> domain
                    |
                    v
             infrastructure
```

- `domain` depends on NOTHING (pure Java)
- `application` depends on `domain` only (uses port interfaces)
- `infrastructure` implements port interfaces (depends on domain + frameworks)
- `presentation` depends on `application` (controllers call use cases)
- NO circular dependencies between layers
