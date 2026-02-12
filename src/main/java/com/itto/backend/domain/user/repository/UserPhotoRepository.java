package com.itto.backend.domain.user.repository;

import com.itto.backend.domain.user.entity.UserPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPhotoRepository extends JpaRepository<UserPhoto, Long> {

    List<UserPhoto> findByUserUserIdOrderByDisplayOrderAsc(Long userId);

    Optional<UserPhoto> findByUserUserIdAndIsRepresentativeTrue(Long userId);

    long countByUserUserId(Long userId);

    void deleteByUserUserIdAndPhotoId(Long userId, Long photoId);
}
