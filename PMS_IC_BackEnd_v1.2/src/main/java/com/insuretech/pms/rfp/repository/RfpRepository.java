package com.insuretech.pms.rfp.repository;

import com.insuretech.pms.rfp.entity.Rfp;
import com.insuretech.pms.rfp.entity.RfpStatus;
import com.insuretech.pms.rfp.entity.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RfpRepository extends JpaRepository<Rfp, String> {

    List<Rfp> findByProjectIdOrderByCreatedAtDesc(String projectId);

    List<Rfp> findByProjectIdAndStatusOrderByCreatedAtDesc(String projectId, RfpStatus status);

    List<Rfp> findByProjectIdAndProcessingStatusOrderByCreatedAtDesc(String projectId, ProcessingStatus processingStatus);

    Optional<Rfp> findByIdAndProjectId(String id, String projectId);

    @Query("SELECT r FROM Rfp r WHERE r.projectId = :projectId AND " +
           "(LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Rfp> searchByKeyword(@Param("projectId") String projectId, @Param("keyword") String keyword);

    long countByProjectId(String projectId);

    long countByProjectIdAndStatus(String projectId, RfpStatus status);
}
