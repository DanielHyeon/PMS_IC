package com.insuretech.pms.auth.repository;

import com.insuretech.pms.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    List<Permission> findByCategory(String category);

    List<Permission> findAllByOrderByCategoryAscNameAsc();
}
