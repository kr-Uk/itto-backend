package com.itto.backend.domain.user.repository;

import com.itto.backend.domain.user.entity.Verification;
import com.itto.backend.global.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, Long> {

    List<Verification> findByStatus(VerificationStatus status);

    Optional<Verification> findTopByUserUserIdOrderByCreatedAtDesc(Long userId);
}
