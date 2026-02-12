package com.itto.backend.domain.location.repository;

import com.itto.backend.domain.location.entity.ExcludedArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExcludedAreaRepository extends JpaRepository<ExcludedArea, Long> {

    List<ExcludedArea> findByUserUserId(Long userId);

    long countByUserUserId(Long userId);
}
