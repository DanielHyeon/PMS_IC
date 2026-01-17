package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, String> {

    List<Part> findByProjectIdOrderByCreatedAtDesc(String projectId);

    Optional<Part> findByIdAndProjectId(String id, String projectId);

    @Query("SELECT p FROM Part p WHERE p.project.id = :projectId AND p.status = :status ORDER BY p.createdAt DESC")
    List<Part> findByProjectIdAndStatus(@Param("projectId") String projectId, @Param("status") Part.PartStatus status);

    @Query("SELECT p FROM Part p WHERE :userId MEMBER OF p.memberIds")
    List<Part> findByMemberId(@Param("userId") String userId);

    boolean existsByIdAndProjectId(String id, String projectId);
}
