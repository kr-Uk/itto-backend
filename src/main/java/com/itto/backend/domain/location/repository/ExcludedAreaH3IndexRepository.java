package com.itto.backend.domain.location.repository;

import com.itto.backend.domain.location.entity.ExcludedAreaH3Index;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExcludedAreaH3IndexRepository extends JpaRepository<ExcludedAreaH3Index, Long> {

    List<ExcludedAreaH3Index> findByExcludedAreaExcludedAreaId(Long excludedAreaId);

    List<ExcludedAreaH3Index> findByH3Index(String h3Index);

    void deleteByExcludedAreaExcludedAreaId(Long excludedAreaId);
}
