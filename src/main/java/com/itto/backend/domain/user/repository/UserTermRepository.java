package com.itto.backend.domain.user.repository;

import com.itto.backend.domain.user.entity.UserTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTermRepository extends JpaRepository<UserTerm, Long> {

    List<UserTerm> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserTerm> findTopByUserUserIdOrderByCreatedAtDesc(Long userId);
}
