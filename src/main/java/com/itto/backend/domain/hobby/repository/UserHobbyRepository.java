package com.itto.backend.domain.hobby.repository;

import com.itto.backend.domain.hobby.entity.UserHobby;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserHobbyRepository extends JpaRepository<UserHobby, Long> {

    List<UserHobby> findByUserUserId(Long userId);

    void deleteByUserUserId(Long userId);
}
