package com.itto.backend.domain.contact.service;

import com.itto.backend.domain.contact.entity.ContactHash;
import com.itto.backend.domain.contact.repository.ContactHashRepository;
import com.itto.backend.domain.contact.util.PhoneNumberHashUtil;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.global.exception.BusinessException;
import com.itto.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContactHashService")
class ContactHashServiceTest {

    @Mock
    private ContactHashRepository contactHashRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContactHashService contactHashService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .firebaseUid("firebase-test")
                .build();
    }

    @Nested
    @DisplayName("uploadHashes")
    class UploadHashesTest {

        @Test
        @DisplayName("기존 해시 삭제 후 새 해시 저장")
        void deleteAndSaveNew() {
            Long userId = 1L;
            List<String> hashes = List.of("a".repeat(64), "b".repeat(64));

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            contactHashService.uploadHashes(userId, hashes);

            verify(contactHashRepository).deleteByUserUserId(userId);
            verify(contactHashRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("빈 리스트 → 기존 해시 전체 삭제")
        void emptyListDeletesAll() {
            Long userId = 1L;

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            contactHashService.uploadHashes(userId, List.of());

            verify(contactHashRepository).deleteByUserUserId(userId);
            verify(contactHashRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("잘못된 해시 형식 → BusinessException")
        void invalidHashFormat() {
            Long userId = 1L;
            List<String> hashes = List.of("too-short");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> contactHashService.uploadHashes(userId, hashes))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("해시 형식 검증: 64자 hex string")
        void validHashFormat() {
            Long userId = 1L;
            String validHash = "a1b2c3d4e5f6".repeat(5) + "a1b2";  // 64 chars
            List<String> hashes = List.of(validHash);

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            contactHashService.uploadHashes(userId, hashes);

            verify(contactHashRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("업로드 제한 초과 (5000개 초과) → BusinessException")
        void hashLimitExceeded() {
            Long userId = 1L;
            List<String> hashes = java.util.Collections.nCopies(5001, "a".repeat(64));

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> contactHashService.uploadHashes(userId, hashes))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("getMutualBlockUserIds")
    class GetMutualBlockTest {

        @Test
        @DisplayName("상호 연락처 매칭 userId 목록 반환")
        void returnMutualUserIds() {
            Long userId = 1L;
            List<Long> expected = List.of(2L, 3L);

            when(contactHashRepository.findMutualBlockUserIds(userId)).thenReturn(expected);

            List<Long> result = contactHashService.getMutualBlockUserIds(userId);

            assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
        }
    }

    @Nested
    @DisplayName("isContactBlocked")
    class IsContactBlockedTest {

        @Test
        @DisplayName("상호 연락처 차단 여부 확인 - 차단됨")
        void blocked() {
            when(contactHashRepository.findMutualBlockUserIds(1L)).thenReturn(List.of(2L, 3L));

            assertThat(contactHashService.isContactBlocked(1L, 2L)).isTrue();
        }

        @Test
        @DisplayName("상호 연락처 차단 여부 확인 - 차단 아님")
        void notBlocked() {
            when(contactHashRepository.findMutualBlockUserIds(1L)).thenReturn(List.of(3L));

            assertThat(contactHashService.isContactBlocked(1L, 2L)).isFalse();
        }
    }

    @Nested
    @DisplayName("addManualHash")
    class AddManualHashTest {

        @Test
        @DisplayName("직접 입력 번호 해시 후 저장")
        void addManualHash() {
            Long userId = 1L;
            String phoneNumber = "010-1234-5678";

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            contactHashService.addManualHash(userId, phoneNumber);

            ArgumentCaptor<ContactHash> captor = ArgumentCaptor.forClass(ContactHash.class);
            verify(contactHashRepository).save(captor.capture());

            ContactHash saved = captor.getValue();
            String expectedHash = PhoneNumberHashUtil.normalizeAndHash(phoneNumber);
            assertThat(saved.getPhoneHash()).isEqualTo(expectedHash);
            assertThat(saved.getPhoneHash()).hasSize(64);
        }

        @Test
        @DisplayName("정규화 + 해시 처리 검증")
        void normalizeAndHash() {
            Long userId = 1L;

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            contactHashService.addManualHash(userId, "010-1234-5678");
            contactHashService.addManualHash(userId, "+82-10-1234-5678");

            ArgumentCaptor<ContactHash> captor = ArgumentCaptor.forClass(ContactHash.class);
            verify(contactHashRepository, times(2)).save(captor.capture());

            List<ContactHash> saved = captor.getAllValues();
            assertThat(saved.get(0).getPhoneHash()).isEqualTo(saved.get(1).getPhoneHash());
        }
    }

    @Nested
    @DisplayName("removeHash")
    class RemoveHashTest {

        @Test
        @DisplayName("특정 해시 삭제")
        void removeHash() {
            Long userId = 1L;
            Long hashId = 10L;
            ContactHash hash = ContactHash.builder()
                    .user(testUser)
                    .phoneHash("a".repeat(64))
                    .build();

            when(contactHashRepository.findByIdAndUserUserId(hashId, userId))
                    .thenReturn(Optional.of(hash));

            contactHashService.removeHash(userId, hashId);

            verify(contactHashRepository).delete(hash);
        }

        @Test
        @DisplayName("존재하지 않는 해시 삭제 → BusinessException")
        void removeNotFound() {
            Long userId = 1L;
            Long hashId = 999L;

            when(contactHashRepository.findByIdAndUserUserId(hashId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> contactHashService.removeHash(userId, hashId))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
