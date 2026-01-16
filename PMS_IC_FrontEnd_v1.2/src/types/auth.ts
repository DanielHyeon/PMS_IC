// 권한 관련 타입 정의

// 시스템 레벨 역할 (전역)
export type SystemRole = 'admin' | 'pmo' | 'user';

// 프로젝트 레벨 역할 (프로젝트별 할당)
export type ProjectRole = 
  | 'owner'           // 프로젝트 소유자 (스폰서)
  | 'pm'              // 프로젝트 관리자
  | 'part_leader'     // 파트장
  | 'education_mgr'   // 교육 담당자
  | 'qa_lead'         // QA 리드
  | 'ba'              // 비즈니스 분석가
  | 'member';         // 구성원

// 기존 UserRole과의 호환을 위한 매핑
export type LegacyUserRole = 'sponsor' | 'pmo_head' | 'pm' | 'developer' | 'qa' | 'business_analyst' | 'auditor' | 'admin';

// 프로젝트 멤버십 (사용자-프로젝트-역할 연결)
export interface ProjectMembership {
  id: string;
  userId: string;
  projectId: string;
  partId?: string;            // 파트 소속 (optional, part_leader/member인 경우)
  role: ProjectRole;
  assignedBy: string;         // 지정한 사람 ID
  assignedAt: string;
}

// 사용자 권한 정보 (AuthContext에서 사용)
export interface UserPermissions {
  userId: string;
  systemRole: SystemRole;
  projectRoles: ProjectMembership[];
}

// 권한 체크용 Permission 타입
export type Permission = 
  // 시스템 권한
  | 'system:manage_users'
  | 'system:manage_settings'
  | 'system:assign_pm'
  // 프로젝트 권한
  | 'project:create'
  | 'project:edit'
  | 'project:delete'
  | 'project:view_all'
  | 'project:set_default'
  // 파트 권한
  | 'part:create'
  | 'part:edit'
  | 'part:delete'
  | 'part:assign_leader'
  | 'part:add_member'
  // 단계별 관리
  | 'phase:view'
  | 'phase:edit'
  // 칸반보드
  | 'kanban:view'
  | 'kanban:edit'
  | 'kanban:edit_own_tasks'
  // 백로그
  | 'backlog:view'
  | 'backlog:edit'
  // 공통 관리
  | 'common:view'
  | 'common:edit'
  // 교육 관리
  | 'education:view'
  | 'education:edit'
  // 권한 관리
  | 'roles:view'
  | 'roles:edit_system'
  | 'roles:edit_project';

// 역할별 기본 권한 매핑
export const SYSTEM_ROLE_PERMISSIONS: Record<SystemRole, Permission[]> = {
  admin: [
    'system:manage_users',
    'system:manage_settings',
    'system:assign_pm',
    'project:create',
    'project:edit',
    'project:delete',
    'project:view_all',
    'project:set_default',
    'part:create',
    'part:edit',
    'part:delete',
    'part:assign_leader',
    'part:add_member',
    'phase:view',
    'phase:edit',
    'kanban:view',
    'kanban:edit',
    'backlog:view',
    'backlog:edit',
    'common:view',
    'common:edit',
    'education:view',
    'education:edit',
    'roles:view',
    'roles:edit_system',
    'roles:edit_project',
  ],
  pmo: [
    'project:view_all',
    'phase:view',
    'kanban:view',
    'backlog:view',
    'common:view',
    'education:view',
  ],
  user: [],  // 프로젝트 역할에 따라 결정
};

export const PROJECT_ROLE_PERMISSIONS: Record<ProjectRole, Permission[]> = {
  owner: [
    'phase:view',
    'common:view',
    'education:view',
  ],
  pm: [
    'project:edit',
    'project:set_default',
    'part:create',
    'part:edit',
    'part:delete',
    'part:assign_leader',
    'part:add_member',
    'phase:view',
    'phase:edit',
    'kanban:view',
    'kanban:edit',
    'backlog:view',
    'backlog:edit',
    'common:view',
    'common:edit',
    'education:view',
    'roles:view',
    'roles:edit_project',
  ],
  part_leader: [
    'part:add_member',
    'phase:view',
    'phase:edit',
    'kanban:view',
    'kanban:edit',
    'backlog:view',
    'backlog:edit',
    'common:view',
    'common:edit',
    'roles:view',
    'roles:edit_project',
  ],
  education_mgr: [
    'education:view',
    'education:edit',
  ],
  qa_lead: [
    'kanban:view',
    'kanban:edit',
    'backlog:view',
    'backlog:edit',
  ],
  ba: [
    'phase:view',
    'backlog:view',
    'backlog:edit',
  ],
  member: [
    'phase:view',
    'kanban:view',
    'kanban:edit_own_tasks',
    'backlog:view',
    'common:view',
    'education:view',
  ],
};

// 기존 역할 → 새 역할 매핑 (마이그레이션용)
export function mapLegacyRole(legacyRole: LegacyUserRole): { systemRole: SystemRole; defaultProjectRole?: ProjectRole } {
  const mapping: Record<LegacyUserRole, { systemRole: SystemRole; defaultProjectRole?: ProjectRole }> = {
    admin: { systemRole: 'admin' },
    pmo_head: { systemRole: 'pmo' },
    sponsor: { systemRole: 'user', defaultProjectRole: 'owner' },
    pm: { systemRole: 'user', defaultProjectRole: 'pm' },
    developer: { systemRole: 'user', defaultProjectRole: 'member' },
    qa: { systemRole: 'user', defaultProjectRole: 'qa_lead' },
    business_analyst: { systemRole: 'user', defaultProjectRole: 'ba' },
    auditor: { systemRole: 'user' },  // 조회 전용
  };
  return mapping[legacyRole];
}

// 역할 표시명
export const SYSTEM_ROLE_LABELS: Record<SystemRole, string> = {
  admin: '시스템 관리자',
  pmo: 'PMO',
  user: '일반 사용자',
};

export const PROJECT_ROLE_LABELS: Record<ProjectRole, string> = {
  owner: '프로젝트 소유자',
  pm: '프로젝트 관리자',
  part_leader: '파트장',
  education_mgr: '교육 담당자',
  qa_lead: 'QA 리드',
  ba: '비즈니스 분석가',
  member: '구성원',
};
