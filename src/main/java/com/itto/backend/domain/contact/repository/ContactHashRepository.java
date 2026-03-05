package com.itto.backend.domain.contact.repository;

import com.itto.backend.domain.contact.entity.ContactHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContactHashRepository extends JpaRepository<ContactHash, Long> {

    List<ContactHash> findByUserUserId(Long userId);

    @Modifying
    @Query("DELETE FROM ContactHash ch WHERE ch.user.userId = :userId")
    void deleteByUserUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT ch2.user.userId FROM ContactHash ch1 " +
           "JOIN ContactHash ch2 ON ch1.phoneHash = ch2.phoneHash " +
           "WHERE ch1.user.userId = :userId AND ch2.user.userId != :userId " +
           "AND ch2.user.userId IN (" +
           "  SELECT ch3.user.userId FROM ContactHash ch3 " +
           "  JOIN ContactHash ch4 ON ch3.phoneHash = ch4.phoneHash " +
           "  WHERE ch4.user.userId = :userId AND ch3.user.userId != :userId" +
           ")")
    List<Long> findMutualBlockUserIds(@Param("userId") Long userId);

    Optional<ContactHash> findByIdAndUserUserId(Long id, Long userId);
}
