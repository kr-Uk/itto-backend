package com.itto.backend.domain.user.entity;

import com.itto.backend.global.entity.BaseEntity;
import com.itto.backend.global.enums.Gender;
import com.itto.backend.global.enums.Provider;
import com.itto.backend.global.enums.Role;
import com.itto.backend.global.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_provider_uid", columnList = "provider_uid"),
        @Index(name = "idx_users_deleted_at", columnList = "deleted_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "provider_uid", nullable = false, length = 128)
    private String providerUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private Provider provider;

    @Column(name = "nickname", length = 16)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "introduction", length = 30)
    private String introduction;

    @Column(name = "knots", nullable = false)
    @Builder.Default
    private Integer knots = 0;

    @Column(name = "is_premium", nullable = false)
    @Builder.Default
    private Boolean isPremium = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.NONE;

    @Column(name = "location_enabled", nullable = false)
    @Builder.Default
    private Boolean locationEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "ci", length = 256)
    private String ci;

    @Column(name = "di", length = 256)
    private String di;

    @Column(name = "identity_verified", nullable = false)
    @Builder.Default
    private Boolean identityVerified = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public boolean isProfileComplete() {
        return this.nickname != null && this.gender != null && this.birthDate != null;
    }

    public void updateProfile(String nickname, Gender gender, LocalDate birthDate, String introduction) {
        this.nickname = nickname;
        this.gender = gender;
        this.birthDate = birthDate;
        this.introduction = introduction;
    }

    public void updateVerificationStatus(VerificationStatus status) {
        this.verificationStatus = status;
    }

    public void deductKnots(int amount) {
        if (this.knots < amount) {
            throw new IllegalStateException("매듭 잔액 부족: 보유=" + this.knots + ", 필요=" + amount);
        }
        this.knots -= amount;
    }

    public void addKnots(int amount) {
        this.knots += amount;
    }

    public void updateLocationEnabled(boolean enabled) {
        this.locationEnabled = enabled;
    }

    public void updatePremium(boolean isPremium) {
        this.isPremium = isPremium;
    }

    public void updateIdentityVerification(String phoneNumber, String ci, String di) {
        this.phoneNumber = phoneNumber;
        this.ci = ci;
        this.di = di;
        this.identityVerified = true;
    }
}
