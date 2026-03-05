package com.itto.backend.domain.contact.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PhoneNumberHashUtil")
class PhoneNumberHashUtilTest {

    @Nested
    @DisplayName("전화번호 정규화")
    class NormalizeTest {

        @Test
        @DisplayName("010-1234-5678 → +821012345678")
        void normalizeWithHyphens() {
            assertThat(PhoneNumberHashUtil.normalize("010-1234-5678"))
                    .isEqualTo("+821012345678");
        }

        @Test
        @DisplayName("01012345678 → +821012345678")
        void normalizeWithoutHyphens() {
            assertThat(PhoneNumberHashUtil.normalize("01012345678"))
                    .isEqualTo("+821012345678");
        }

        @Test
        @DisplayName("+82-10-1234-5678 → +821012345678")
        void normalizeWithCountryCodeAndHyphens() {
            assertThat(PhoneNumberHashUtil.normalize("+82-10-1234-5678"))
                    .isEqualTo("+821012345678");
        }

        @Test
        @DisplayName("+82 10 1234 5678 → +821012345678")
        void normalizeWithSpaces() {
            assertThat(PhoneNumberHashUtil.normalize("+82 10 1234 5678"))
                    .isEqualTo("+821012345678");
        }

        @Test
        @DisplayName("null → IllegalArgumentException")
        void normalizeNull() {
            assertThatThrownBy(() -> PhoneNumberHashUtil.normalize(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("빈 문자열 → IllegalArgumentException")
        void normalizeEmpty() {
            assertThatThrownBy(() -> PhoneNumberHashUtil.normalize(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("SHA-256 해시 생성")
    class HashTest {

        @Test
        @DisplayName("동일 입력 → 동일 해시")
        void sameInputSameHash() {
            String hash1 = PhoneNumberHashUtil.hash("+821012345678");
            String hash2 = PhoneNumberHashUtil.hash("+821012345678");
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("다른 입력 → 다른 해시")
        void differentInputDifferentHash() {
            String hash1 = PhoneNumberHashUtil.hash("+821012345678");
            String hash2 = PhoneNumberHashUtil.hash("+821098765432");
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("해시 길이 64자 hex string")
        void hashLength64() {
            String hash = PhoneNumberHashUtil.hash("+821012345678");
            assertThat(hash).hasSize(64);
            assertThat(hash).matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("null → IllegalArgumentException")
        void hashNull() {
            assertThatThrownBy(() -> PhoneNumberHashUtil.hash(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("정규화 + 해시 통합")
    class NormalizeAndHashTest {

        @Test
        @DisplayName("하이픈 있는 번호와 없는 번호가 동일 해시")
        void sameHashForDifferentFormats() {
            String hash1 = PhoneNumberHashUtil.normalizeAndHash("010-1234-5678");
            String hash2 = PhoneNumberHashUtil.normalizeAndHash("01012345678");
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("+82 국가코드 포함 번호도 동일 해시")
        void sameHashWithCountryCode() {
            String hash1 = PhoneNumberHashUtil.normalizeAndHash("010-1234-5678");
            String hash2 = PhoneNumberHashUtil.normalizeAndHash("+82-10-1234-5678");
            assertThat(hash1).isEqualTo(hash2);
        }
    }
}
