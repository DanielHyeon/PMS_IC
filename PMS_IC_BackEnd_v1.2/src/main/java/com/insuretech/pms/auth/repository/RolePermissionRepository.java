package com.insuretech.pms.auth.repository;

import com.insuretech.pms.auth.entity.RolePermission;
import com.insuretech.pms.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, String> {

    List<RolePermission> findByRole(User.UserRole role);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.role = ?1 AND rp.permission.id = ?2")
    Optional<RolePermission> findByRoleAndPermissionId(User.UserRole role, String permissionId);

    List<RolePermission> findByRoleAndGrantedTrue(User.UserRole role);

    void deleteByRoleAndPermissionId(User.UserRole role, String permissionId);
}
