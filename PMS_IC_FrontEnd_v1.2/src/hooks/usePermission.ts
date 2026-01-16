import { useCallback, useMemo } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useProject } from '../contexts/ProjectContext';
import {
  Permission,
  SystemRole,
  ProjectRole,
  SYSTEM_ROLE_PERMISSIONS,
  PROJECT_ROLE_PERMISSIONS,
  LegacyUserRole,
  mapLegacyRole,
} from '../types/auth';

interface UsePermissionReturn {
  // 시스템 권한
  isAdmin: boolean;
  isPmo: boolean;
  systemRole: SystemRole;
  
  // 현재 프로젝트 권한
  currentProjectRole: ProjectRole | null;
  currentPartId: string | null;
  
  // 권한 체크 메서드
  hasPermission: (permission: Permission) => boolean;
  canAccessProject: (projectId: string) => boolean;
  canManageProject: (projectId: string) => boolean;
  canManagePart: (partId: string) => boolean;
  
  // 메뉴 접근 권한
  canViewDashboard: boolean;
  canViewProjects: boolean;
  canViewParts: boolean;
  canViewPhases: boolean;
  canEditPhases: boolean;
  canViewKanban: boolean;
  canEditKanban: boolean;
  canViewBacklog: boolean;
  canEditBacklog: boolean;
  canViewCommon: boolean;
  canEditCommon: boolean;
  canViewEducation: boolean;
  canEditEducation: boolean;
  canViewRoles: boolean;
  canEditSystemRoles: boolean;
  canEditProjectRoles: boolean;
}

/**
 * 권한 체크 훅
 * AuthContext와 ProjectContext를 기반으로 권한 정보 제공
 */
export function usePermission(): UsePermissionReturn {
  const { user, userPermissions, legacyRole } = useAuth();
  const { currentProject } = useProject();
  
  // 시스템 역할 (기존 역할에서 매핑 또는 새 권한 체계 사용)
  const systemRole = useMemo<SystemRole>(() => {
    if (userPermissions?.systemRole) {
      return userPermissions.systemRole;
    }
    // 기존 역할에서 매핑
    if (legacyRole) {
      return mapLegacyRole(legacyRole).systemRole;
    }
    return 'user';
  }, [userPermissions, legacyRole]);
  
  // 현재 프로젝트에서의 역할
  const currentProjectRole = useMemo<ProjectRole | null>(() => {
    if (!currentProject || !userPermissions?.projectRoles) {
      // 기존 역할에서 매핑
      if (legacyRole) {
        const mapped = mapLegacyRole(legacyRole);
        return mapped.defaultProjectRole || null;
      }
      return null;
    }
    const membership = userPermissions.projectRoles.find(
      (m) => m.projectId === currentProject.id
    );
    return membership?.role || null;
  }, [currentProject, userPermissions, legacyRole]);
  
  // 현재 파트 ID (part_leader, member인 경우)
  const currentPartId = useMemo<string | null>(() => {
    if (!currentProject || !userPermissions?.projectRoles) {
      return null;
    }
    const membership = userPermissions.projectRoles.find(
      (m) => m.projectId === currentProject.id
    );
    return membership?.partId || null;
  }, [currentProject, userPermissions]);
  
  // 권한 체크
  const hasPermission = useCallback((permission: Permission): boolean => {
    // 시스템 역할 권한 체크
    const systemPermissions = SYSTEM_ROLE_PERMISSIONS[systemRole] || [];
    if (systemPermissions.includes(permission)) {
      return true;
    }
    
    // 프로젝트 역할 권한 체크
    if (currentProjectRole) {
      const projectPermissions = PROJECT_ROLE_PERMISSIONS[currentProjectRole] || [];
      if (projectPermissions.includes(permission)) {
        return true;
      }
    }
    
    return false;
  }, [systemRole, currentProjectRole]);
  
  // 프로젝트 접근 가능 여부
  const canAccessProject = useCallback((projectId: string): boolean => {
    // Admin, PMO는 모든 프로젝트 접근 가능
    if (systemRole === 'admin' || systemRole === 'pmo') {
      return true;
    }
    // 해당 프로젝트에 역할이 있는지 체크
    if (!userPermissions?.projectRoles) {
      return true; // 기존 호환성
    }
    return userPermissions.projectRoles.some((m) => m.projectId === projectId);
  }, [systemRole, userPermissions]);
  
  // 프로젝트 관리 가능 여부 (PM 이상)
  const canManageProject = useCallback((projectId: string): boolean => {
    if (systemRole === 'admin') {
      return true;
    }
    if (!currentProject || currentProject.id !== projectId) {
      return false;
    }
    // PM이고 본인이 생성한 프로젝트인지 (또는 managerId가 본인인지)
    if (currentProjectRole === 'pm' && currentProject.managerId === user?.id) {
      return true;
    }
    return false;
  }, [systemRole, currentProject, currentProjectRole, user]);
  
  // 파트 관리 가능 여부 (PM 또는 해당 파트장)
  const canManagePart = useCallback((partId: string): boolean => {
    if (systemRole === 'admin') {
      return true;
    }
    if (currentProjectRole === 'pm') {
      return true;
    }
    if (currentProjectRole === 'part_leader' && currentPartId === partId) {
      return true;
    }
    return false;
  }, [systemRole, currentProjectRole, currentPartId]);
  
  // 메뉴 권한 계산
  const isAdmin = systemRole === 'admin';
  const isPmo = systemRole === 'pmo';
  
  // 대시보드 - 모든 역할 가능
  const canViewDashboard = true;
  
  // 프로젝트 관리 - Admin, PM
  const canViewProjects = isAdmin || currentProjectRole === 'pm';
  
  // 파트 관리 - Admin, PM, Part Leader
  const canViewParts = isAdmin || isPmo || currentProjectRole === 'pm' || currentProjectRole === 'part_leader';
  
  // 단계별 관리
  const canViewPhases = hasPermission('phase:view');
  const canEditPhases = hasPermission('phase:edit');
  
  // 칸반보드
  const canViewKanban = hasPermission('kanban:view');
  const canEditKanban = hasPermission('kanban:edit') || hasPermission('kanban:edit_own_tasks');
  
  // 백로그
  const canViewBacklog = hasPermission('backlog:view');
  const canEditBacklog = hasPermission('backlog:edit');
  
  // 공통 관리
  const canViewCommon = hasPermission('common:view');
  const canEditCommon = hasPermission('common:edit');
  
  // 교육 관리
  const canViewEducation = hasPermission('education:view');
  const canEditEducation = hasPermission('education:edit');
  
  // 권한 관리
  const canViewRoles = hasPermission('roles:view') || isAdmin;
  const canEditSystemRoles = hasPermission('roles:edit_system');
  const canEditProjectRoles = hasPermission('roles:edit_project');
  
  return {
    isAdmin,
    isPmo,
    systemRole,
    currentProjectRole,
    currentPartId,
    hasPermission,
    canAccessProject,
    canManageProject,
    canManagePart,
    canViewDashboard,
    canViewProjects,
    canViewParts,
    canViewPhases,
    canEditPhases,
    canViewKanban,
    canEditKanban,
    canViewBacklog,
    canEditBacklog,
    canViewCommon,
    canEditCommon,
    canViewEducation,
    canEditEducation,
    canViewRoles,
    canEditSystemRoles,
    canEditProjectRoles,
  };
}

/**
 * 기존 역할 기반 권한 체크 (호환성용)
 */
export function hasLegacyPermission(role: LegacyUserRole, permission: Permission): boolean {
  const { systemRole, defaultProjectRole } = mapLegacyRole(role);
  
  // 시스템 역할 권한
  const systemPermissions = SYSTEM_ROLE_PERMISSIONS[systemRole] || [];
  if (systemPermissions.includes(permission)) {
    return true;
  }
  
  // 프로젝트 역할 권한
  if (defaultProjectRole) {
    const projectPermissions = PROJECT_ROLE_PERMISSIONS[defaultProjectRole] || [];
    if (projectPermissions.includes(permission)) {
      return true;
    }
  }
  
  return false;
}
