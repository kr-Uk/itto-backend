package com.itto.backend.domain.location.repository;

import com.itto.backend.domain.location.entity.ExcludedAreaH3Index;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ExcludedAreaH3IndexRepository extends JpaRepository<ExcludedAreaH3Index, Long> {

    List<ExcludedAreaH3Index> findByExcludedAreaId(Long excludedAreaId);

    List<ExcludedAreaH3Index> findByH3Index(String h3Index);

    void deleteByExcludedAreaId(Long excludedAreaId);

    /**
     * 특정 H3 셀을 제외 지역으로 등록한 사용자 ID 목록 조회
     */
    @Query("SELECT DISTINCT e.excludedArea.user.userId FROM ExcludedAreaH3Index e WHERE e.h3Index = :h3Index")
    Set<Long> findUserIdsByH3Index(@Param("h3Index") String h3Index);

    /**
     * 여러 ExcludedArea ID에 해당하는 H3 인덱스를 한번에 조회 (N+1 방지)
     */
    @Query("SELECT e FROM ExcludedAreaH3Index e WHERE e.excludedArea.id IN :excludedAreaIds")
    List<ExcludedAreaH3Index> findByExcludedAreaIdIn(@Param("excludedAreaIds") List<Long> excludedAreaIds);
}
