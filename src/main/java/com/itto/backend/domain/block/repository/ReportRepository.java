package com.itto.backend.domain.block.repository;

import com.itto.backend.domain.block.entity.Report;
import com.itto.backend.domain.common.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Find all reports by status
     */
    List<Report> findByStatus(ReportStatus status);

    /**
     * Find all reports targeting a specific user
     */
    List<Report> findByTargetUserUserId(Long targetUserId);
}
