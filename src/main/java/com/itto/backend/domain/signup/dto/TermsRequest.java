package com.itto.backend.domain.signup.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TermsRequest {

    @AssertTrue(message = "서비스 이용약관에 동의해야 합니다")
    private boolean serviceTerms;

    @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다")
    private boolean privacyPolicy;

    @AssertTrue(message = "위치정보 이용약관에 동의해야 합니다")
    private boolean locationTerms;

    private boolean marketingConsent;
}
