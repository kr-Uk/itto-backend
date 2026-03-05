package com.itto.backend.domain.contact.service;

import com.itto.backend.domain.contact.entity.ContactHash;
import com.itto.backend.domain.contact.repository.ContactHashRepository;
import com.itto.backend.domain.contact.util.PhoneNumberHashUtil;
import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.user.repository.UserRepository;
import com.itto.backend.global.exception.BusinessException;
import com.itto.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactHashService {

    private static final int MAX_HASH_COUNT = 5000;
    private static final Pattern HEX_64_PATTERN = Pattern.compile("[0-9a-f]{64}");

    private final ContactHashRepository contactHashRepository;
    private final UserRepository userRepository;

    @Transactional
    public void uploadHashes(Long userId, List<String> hashes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (hashes.size() > MAX_HASH_COUNT) {
            throw new BusinessException(ErrorCode.HASH_LIMIT_EXCEEDED);
        }

        for (String hash : hashes) {
            if (!HEX_64_PATTERN.matcher(hash).matches()) {
                throw new BusinessException(ErrorCode.INVALID_HASH_FORMAT);
            }
        }

        contactHashRepository.deleteByUserUserId(userId);

        if (!hashes.isEmpty()) {
            List<ContactHash> entities = hashes.stream()
                    .map(hash -> ContactHash.builder()
                            .user(user)
                            .phoneHash(hash)
                            .build())
                    .toList();
            contactHashRepository.saveAll(entities);
        }
    }

    public List<Long> getMutualBlockUserIds(Long userId) {
        return contactHashRepository.findMutualBlockUserIds(userId);
    }

    public boolean isContactBlocked(Long userA, Long userB) {
        List<Long> mutualIds = contactHashRepository.findMutualBlockUserIds(userA);
        return mutualIds.contains(userB);
    }

    @Transactional
    public void addManualHash(Long userId, String phoneNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String hash = PhoneNumberHashUtil.normalizeAndHash(phoneNumber);

        ContactHash contactHash = ContactHash.builder()
                .user(user)
                .phoneHash(hash)
                .build();

        contactHashRepository.save(contactHash);
    }

    @Transactional
    public void removeHash(Long userId, Long hashId) {
        ContactHash contactHash = contactHashRepository.findByIdAndUserUserId(hashId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTACT_HASH_NOT_FOUND));

        contactHashRepository.delete(contactHash);
    }

    public List<ContactHash> getHashes(Long userId) {
        return contactHashRepository.findByUserUserId(userId);
    }
}
