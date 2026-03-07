package com.itto.backend.domain.user.repository;

import com.itto.backend.domain.user.entity.User;
import com.itto.backend.domain.common.enums.Provider;
import com.itto.backend.domain.common.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUid(Provider provider, String providerUid);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndUserIdNot(String nickname, Long userId);

    Page<User> findByVerificationStatus(VerificationStatus status, Pageable pageable);

    boolean existsByCiAndDeletedAtIsNull(String ci);

    boolean existsByDiAndDeletedAtIsNull(String di);
}
