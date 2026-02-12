package com.itto.backend.domain.user.repository;

import com.itto.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByFirebaseUid(String firebaseUid);

    boolean existsByFirebaseUid(String firebaseUid);

    boolean existsByNickname(String nickname);

    Optional<User> findByFirebaseUidAndDeletedAtIsNull(String firebaseUid);
}
