package com.itto.backend.domain.identity.entity;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "identity_verifications", indexes = {
    @Index(name = "idx_identity_verifications_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class IdentityVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long verificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "request_no", nullable = false, unique = true, length = 64)
    private String requestNo;

    @Column(name = "enc_data", columnDefinition = "TEXT")
    private String encData;

    @Column(name = "token_version_id", length = 64)
    private String tokenVersionId;

    @Column(name = "integrity_value", columnDefinition = "TEXT")
    private String integrityValue;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "REQUESTED";

    @Column(name = "verified_name", length = 50)
    private String verifiedName;

    @Column(name = "verified_phone", length = 20)
    private String verifiedPhone;

    @Column(name = "verified_gender", length = 10)
    private String verifiedGender;

    @Column(name = "verified_birth_date")
    private LocalDate verifiedBirthDate;

    @Column(name = "ci", length = 256)
    private String ci;

    @Column(name = "di", length = 256)
    private String di;

    @Column(name = "carrier", length = 20)
    private String carrier;

    @Column(name = "error_code", length = 20)
    private String errorCode;

    @Column(name = "error_message", length = 200)
    private String errorMessage;

    public void markSuccess(String verifiedName, String verifiedPhone, String verifiedGender,
                            LocalDate verifiedBirthDate, String ci, String di, String carrier) {
        this.status = "SUCCESS";
        this.verifiedName = verifiedName;
        this.verifiedPhone = verifiedPhone;
        this.verifiedGender = verifiedGender;
        this.verifiedBirthDate = verifiedBirthDate;
        this.ci = ci;
        this.di = di;
        this.carrier = carrier;
    }

    public void markFailed(String errorCode, String errorMessage) {
        this.status = "FAILED";
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public void updateCryptoData(String tokenVersionId, String encData, String integrityValue) {
        this.tokenVersionId = tokenVersionId;
        this.encData = encData;
        this.integrityValue = integrityValue;
    }
}
