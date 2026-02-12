package com.itto.backend.domain.user.repository;

import com.itto.backend.domain.user.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findByUserUserId(Long userId);

    Optional<FcmToken> findByUserUserIdAndDeviceId(Long userId, String deviceId);

    void deleteByUserUserId(Long userId);
}
