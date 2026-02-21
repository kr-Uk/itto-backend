package com.itto.backend.domain.identity.repository;

import com.itto.backend.domain.identity.entity.IdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, Long> {

    Optional<IdentityVerification> findByRequestNo(String requestNo);

    Optional<IdentityVerification> findTopByUserUserIdOrderByCreatedAtDesc(Long userId);
}
