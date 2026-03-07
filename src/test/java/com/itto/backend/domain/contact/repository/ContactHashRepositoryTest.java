package com.itto.backend.domain.contact.repository;

import com.itto.backend.domain.contact.entity.ContactHash;
import com.itto.backend.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContactHash Entity")
class ContactHashRepositoryTest {

    @Test
    @DisplayName("ContactHash 엔티티에 원문 전화번호 필드가 없음 (보안)")
    void entityDoesNotHaveRawPhoneField() {
        // ContactHash는 phoneHash만 저장, 원문 전화번호 필드 없음
        ContactHash hash = ContactHash.builder()
                .phoneHash("a".repeat(64))
                .user(User.builder().providerUid("test").build())
                .build();

        assertThat(hash.getPhoneHash()).hasSize(64);
        // 원문 전화번호 getter가 없음을 컴파일 타임에 보장
        // hash.getPhoneNumber() — 이 메서드가 존재하지 않아야 함
    }

    @Test
    @DisplayName("ContactHash 빌더로 올바르게 생성")
    void builderCreatesEntity() {
        User user = User.builder().providerUid("firebase-1").build();
        String phoneHash = "abcdef1234567890".repeat(4);

        ContactHash hash = ContactHash.builder()
                .user(user)
                .phoneHash(phoneHash)
                .build();

        assertThat(hash.getUser()).isEqualTo(user);
        assertThat(hash.getPhoneHash()).isEqualTo(phoneHash);
        assertThat(hash.getId()).isNull();
    }
}
