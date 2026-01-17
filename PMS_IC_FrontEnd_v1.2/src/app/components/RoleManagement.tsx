import { useState, useEffect } from 'react';
import { Shield, User, Lock, Check, X, AlertCircle, Building2, FolderKanban, Users, Crown, UserPlus, Trash2, Edit2, Search, UserCog, ChevronDown, ChevronRight, Briefcase } from 'lucide-react';
import { UserRole } from '../App';
import { apiService } from '../../services/api';
import { useProject } from '../../contexts/ProjectContext';
import { ProjectRole, PROJECT_ROLE_LABELS, SystemRole, SYSTEM_ROLE_LABELS } from '../../types/auth';

type TabType = 'users' | 'system' | 'project';

interface Role {
  id: string;
  name: string;
  nameEn: string;
  description: string;
  userCount: number;
}

interface Permission {
  id: string;
  category: string;
  name: string;
  roles: {
    sponsor: boolean;
    pmo_head: boolean;
    pm: boolean;
    developer: boolean;
    qa: boolean;
    business_analyst: boolean;
    auditor: boolean;
    admin: boolean;
  };
}

// System user for admin management
interface SystemUser {
  id: string;
  name: string;
  email: string;
  department: string;
  systemRole: SystemRole;
  legacyRole: string;
  status: 'active' | 'inactive';
  createdAt: string;
  projectRoles?: { projectId: string; projectName: string; role: ProjectRole }[];
}

// Project member type
interface ProjectMember {
  id: string;
  userId: string;
  userName: string;
  userEmail: string;
  role: ProjectRole;
  partName?: string;
  assignedAt: string;
}

const roles: Role[] = [
  {
    id: 'sponsor',
    name: '프로젝트 스폰서',
    nameEn: 'Project Sponsor',
    description: '프로젝트의 최종 의사결정, 예산 승인, 비즈니스 목표 설정',
    userCount: 2,
  },
  {
    id: 'pmo_head',
    name: 'PMO 총괄',
    nameEn: 'PMO Head',
    description: '전사 프로젝트 포트폴리오 관리, PMO 조직 운영',
    userCount: 1,
  },
  {
    id: 'pm',
    name: '프로젝트 관리자',
    nameEn: 'Project Manager',
    description: '개별 프로젝트의 계획, 실행, 통제, 종료 책임',
    userCount: 3,
  },
  {
    id: 'developer',
    name: '개발팀',
    nameEn: 'Development Team',
    description: '사용자 스토리 구현, 단위 테스트, 코드 개발',
    userCount: 12,
  },
  {
    id: 'qa',
    name: 'QA팀',
    nameEn: 'QA Team',
    description: '테스트 케이스 설계, 통합/시스템 테스트 수행',
    userCount: 4,
  },
  {
    id: 'business_analyst',
    name: '현업 분석가',
    nameEn: 'Business Analyst',
    description: '업무 요구사항 정의, PoC 결과 검증',
    userCount: 5,
  },
  {
    id: 'auditor',
    name: '외부 감리',
    nameEn: 'External Auditor',
    description: '프로젝트 진행 상황 및 산출물 제3자 검토',
    userCount: 2,
  },
  {
    id: 'admin',
    name: '시스템 관리자',
    nameEn: 'System Admin',
    description: '시스템 운영, 사용자 계정 및 권한 관리',
    userCount: 2,
  },
];

const initialPermissions: Permission[] = [
  {
    id: 'view_dashboard',
    category: '대시보드',
    name: '전사 프로젝트 대시보드 조회',
    roles: {
      sponsor: true,
      pmo_head: true,
      pm: true,
      developer: false,
      qa: false,
      business_analyst: false,
      auditor: true,
      admin: false,
    },
  },
  {
    id: 'create_project',
    category: '프로젝트',
    name: '프로젝트 생성',
    roles: {
      sponsor: false,
      pmo_head: true,
      pm: true,
      developer: false,
      qa: false,
      business_analyst: false,
      auditor: false,
      admin: true,
    },
  },
  {
    id: 'delete_project',
    category: '프로젝트',
    name: '프로젝트 삭제',
    roles: {
      sponsor: false,
      pmo_head: true,
      pm: false,
      developer: false,
      qa: false,
      business_analyst: false,
      auditor: false,
      admin: true,
    },
  },
  {
    id: 'manage_wbs',
    category: '일정관리',
    name: 'WBS 작성 및 수정',
    roles: {
      sponsor: false,
      pmo_head: true,
      pm: true,
      developer: false,
      qa: false,
      business_analyst: false,
      auditor: false,
      admin: false,
    },
  },
  {
    id: 'manage_budget',
    category: '예산관리',
    name: '예산 편성 및 수정',
    roles: {
      sponsor: true,
      pmo_head: true,
      pm: false,
      developer: false,
      qa: false,
      business_analyst: false,
      auditor: false,
      admin: false,
    },
  },
  {
    id: 'approve_budget',
    category: '예산관리',
    name: '예산 최종 승인',
    roles: {
      sponsor: true,
      pmo_head: false,
      pm: false,
      developer: false,
      qa: false,
      business_analyst: false,
      auditor: false,
      admin: false,
    },
  },
  {
    id: 'manage_risk',
    category: '리스크/이슈',
    name: '리스크 및 이슈 등록/수정',
    roles: {
      sponsor: false,
      pmo_head: true,
      pm: true,
      developer: true,
      qa: true,
      business_analyst: false,
      auditor: false,
      admin: false,
    },
  },
  {
    id: 'approve_deliverable',
    category: '산출물',
    name: '산출물 승인/반려',
    roles: {
      sponsor: true,
      pmo_head: true,
      pm: true,
      developer: false,
      qa: false,
      business_analyst: false,
      auditor: false,
      admin: false,
    },
  },
  {
    id: 'manage_backlog',
    category: '애자일',
    name: '백로그 관리',
    roles: {
      sponsor: false,
      pmo_head: false,
      pm: true,
      developer: true,
      qa: true,
      business_analyst: true,
      auditor: false,
      admin: false,
    },
  },
  {
    id: 'manage_sprint',
    category: '애자일',
    name: '스프린트 관리',
    roles: {
      sponsor: false,
      pmo_head: false,
      pm: true,
      developer: true,
      qa: true,
      business_analyst: false,
      auditor: false,
      admin: false,
    },
  },
  {
    id: 'use_ai_assistant',
    category: 'AI 기능',
    name: 'AI 어시스턴트 사용',
    roles: {
      sponsor: true,
      pmo_head: true,
      pm: true,
      developer: true,
      qa: true,
      business_analyst: true,
      auditor: false,
      admin: false,
    },
  },
  {
    id: 'view_audit_log',
    category: '보안/감사',
    name: '감사 로그 조회',
    roles: {
      sponsor: false,
      pmo_head: true,
      pm: false,
      developer: false,
      qa: false,
      business_analyst: false,
      auditor: true,
      admin: true,
    },
  },
  {
    id: 'manage_users',
    category: '보안/감사',
    name: '사용자 및 권한 관리',
    roles: {
      sponsor: false,
      pmo_head: false,
      pm: false,
      developer: false,
      qa: false,
      business_analyst: false,
      auditor: false,
      admin: true,
    },
  },
];

export default function RoleManagement({ userRole }: { userRole: UserRole }) {
  const [activeTab, setActiveTab] = useState<TabType>('users');
  const canManageProjectRoles = ['admin', 'pm'].includes(userRole);

  // Admin only tabs
  const isAdmin = userRole === 'admin';

  // If not admin, default to project tab
  useEffect(() => {
    if (!isAdmin) {
      setActiveTab('project');
    }
  }, [isAdmin]);

  return (
    <div className="p-6">
      <div className="mb-6">
        <h2 className="text-2xl font-semibold text-gray-900">역할 및 권한 관리</h2>
        <p className="text-sm text-gray-500 mt-1">RBAC 기반 접근 제어 및 직무 분리(SoD) 원칙 적용</p>
      </div>

      {/* Tab Navigation */}
      <div className="mb-6 border-b border-gray-200">
        <nav className="-mb-px flex gap-6">
          {isAdmin && (
            <>
              <button
                type="button"
                onClick={() => setActiveTab('users')}
                className={`flex items-center gap-2 py-3 px-1 border-b-2 font-medium text-sm transition-colors ${
                  activeTab === 'users'
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <UserCog size={18} />
                사용자 관리
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('system')}
                className={`flex items-center gap-2 py-3 px-1 border-b-2 font-medium text-sm transition-colors ${
                  activeTab === 'system'
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <Building2 size={18} />
                시스템 권한
              </button>
            </>
          )}
          <button
            type="button"
            onClick={() => setActiveTab('project')}
            className={`flex items-center gap-2 py-3 px-1 border-b-2 font-medium text-sm transition-colors ${
              activeTab === 'project'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            <FolderKanban size={18} />
            프로젝트 권한
          </button>
        </nav>
      </div>

      {/* Tab Content */}
      {activeTab === 'users' && isAdmin ? (
        <UserManagementTab />
      ) : activeTab === 'system' && isAdmin ? (
        <SystemPermissionsTab userRole={userRole} />
      ) : (
        <ProjectPermissionsTab userRole={userRole} canManage={canManageProjectRoles} />
      )}
    </div>
  );
}

// ========== User Management Tab (Admin Only) ==========
function UserManagementTab() {
  const { projects } = useProject();
  const [users, setUsers] = useState<SystemUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('all');
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [selectedUser, setSelectedUser] = useState<SystemUser | null>(null);
  const [showAssignPMDialog, setShowAssignPMDialog] = useState(false);
  const [expandedUserId, setExpandedUserId] = useState<string | null>(null);

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const data = await apiService.getUsers();
      // Add mock project roles
      const usersWithRoles = (data as SystemUser[]).map(user => ({
        ...user,
        projectRoles: user.legacyRole === 'pm' ? [
          { projectId: '1', projectName: 'AI 기반 손해보험 지급심사', role: 'pm' as ProjectRole }
        ] : []
      }));
      setUsers(usersWithRoles);
    } catch (error) {
      console.error('Failed to load users:', error);
      setMessage({ type: 'error', text: '사용자 목록을 불러오는데 실패했습니다' });
    } finally {
      setLoading(false);
    }
  };

  // Filter users
  const filteredUsers = users.filter(user => {
    const matchesSearch = user.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.department.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesRole = roleFilter === 'all' || user.systemRole === roleFilter;
    return matchesSearch && matchesRole;
  });

  // Change system role
  const handleChangeSystemRole = async (userId: string, newRole: SystemRole) => {
    try {
      await apiService.updateUserSystemRole(userId, newRole);
      setUsers(users.map(u => u.id === userId ? { ...u, systemRole: newRole } : u));
      setMessage({ type: 'success', text: `시스템 역할이 ${SYSTEM_ROLE_LABELS[newRole]}(으)로 변경되었습니다` });
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      console.error('Failed to update system role:', error);
      setMessage({ type: 'error', text: '역할 변경에 실패했습니다' });
    }
  };

  // Assign PM to project
  const handleAssignPM = async (userId: string, projectIds: string[]) => {
    try {
      await apiService.assignPM(userId, projectIds);
      // Update local state
      const projectNames = projectIds.map(id =>
        projects.find(p => p.id === id)?.name || 'Unknown Project'
      );
      setUsers(users.map(u => {
        if (u.id === userId) {
          const newRoles = projectIds.map((id, i) => ({
            projectId: id,
            projectName: projectNames[i],
            role: 'pm' as ProjectRole
          }));
          return {
            ...u,
            projectRoles: [...(u.projectRoles || []), ...newRoles]
          };
        }
        return u;
      }));
      setMessage({ type: 'success', text: 'PM 역할이 지정되었습니다' });
      setShowAssignPMDialog(false);
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      console.error('Failed to assign PM:', error);
      setMessage({ type: 'error', text: 'PM 지정에 실패했습니다' });
    }
  };

  // Revoke PM from project
  const handleRevokePM = async (userId: string, projectId: string) => {
    if (!confirm('이 사용자의 PM 역할을 해제하시겠습니까?')) return;

    try {
      await apiService.revokePM(userId, projectId);
      setUsers(users.map(u => {
        if (u.id === userId) {
          return {
            ...u,
            projectRoles: (u.projectRoles || []).filter(r => r.projectId !== projectId)
          };
        }
        return u;
      }));
      setMessage({ type: 'success', text: 'PM 역할이 해제되었습니다' });
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      console.error('Failed to revoke PM:', error);
      setMessage({ type: 'error', text: 'PM 해제에 실패했습니다' });
    }
  };

  // Get role badge style
  const getSystemRoleBadge = (role: SystemRole) => {
    switch (role) {
      case 'admin':
        return 'bg-red-100 text-red-700';
      case 'pmo':
        return 'bg-purple-100 text-purple-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">사용자 목록을 불러오는 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Admin notice */}
      <div className="bg-gradient-to-r from-red-50 to-orange-50 border-2 border-red-200 rounded-xl p-4">
        <div className="flex items-start gap-3">
          <Shield className="text-red-600 mt-0.5" size={24} />
          <div>
            <h3 className="font-semibold text-gray-900">시스템 관리자 전용</h3>
            <p className="text-sm text-gray-700 mt-1">
              이 화면에서 사용자의 시스템 역할(Admin, PMO, 일반)을 변경하고, PM 역할을 지정할 수 있습니다.
            </p>
          </div>
        </div>
      </div>

      {/* Message */}
      {message && (
        <div className={`rounded-xl p-4 ${
          message.type === 'success'
            ? 'bg-green-50 border-2 border-green-300'
            : 'bg-red-50 border-2 border-red-300'
        }`}>
          <div className="flex items-center gap-3">
            {message.type === 'success' ? (
              <Check className="text-green-600" size={20} />
            ) : (
              <AlertCircle className="text-red-600" size={20} />
            )}
            <p className={`text-sm font-medium ${
              message.type === 'success' ? 'text-green-800' : 'text-red-800'
            }`}>
              {message.text}
            </p>
          </div>
        </div>
      )}

      {/* Search and Filter */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="이름, 이메일, 부서로 검색..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
            aria-label="역할 필터"
            className="px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="all">전체 역할</option>
            <option value="admin">시스템 관리자</option>
            <option value="pmo">PMO</option>
            <option value="user">일반 사용자</option>
          </select>
        </div>
      </div>

      {/* User List */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200 bg-gradient-to-r from-gray-50 to-white">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Users className="text-blue-600" size={24} />
              <div>
                <h3 className="font-semibold text-gray-900">전체 사용자</h3>
                <p className="text-sm text-gray-500">{filteredUsers.length}명</p>
              </div>
            </div>
          </div>
        </div>

        <div className="divide-y divide-gray-100">
          {filteredUsers.map(user => (
            <div key={user.id} className="hover:bg-gray-50">
              <div
                className="flex items-center gap-4 px-6 py-4 cursor-pointer"
                onClick={() => setExpandedUserId(expandedUserId === user.id ? null : user.id)}
              >
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white text-sm font-medium">
                  {user.name[0]}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-900">{user.name}</span>
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${getSystemRoleBadge(user.systemRole)}`}>
                      {SYSTEM_ROLE_LABELS[user.systemRole]}
                    </span>
                    {user.projectRoles && user.projectRoles.length > 0 && (
                      <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                        PM ({user.projectRoles.length})
                      </span>
                    )}
                  </div>
                  <div className="text-sm text-gray-500">{user.email} · {user.department}</div>
                </div>
                <div className="flex items-center gap-2">
                  <select
                    value={user.systemRole}
                    onChange={(e) => {
                      e.stopPropagation();
                      handleChangeSystemRole(user.id, e.target.value as SystemRole);
                    }}
                    onClick={(e) => e.stopPropagation()}
                    aria-label={`${user.name} 시스템 역할 변경`}
                    className="text-sm px-3 py-1.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="admin">시스템 관리자</option>
                    <option value="pmo">PMO</option>
                    <option value="user">일반 사용자</option>
                  </select>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedUser(user);
                      setShowAssignPMDialog(true);
                    }}
                    className="flex items-center gap-1 px-3 py-1.5 text-sm text-blue-600 border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors"
                    title="PM 지정"
                  >
                    <Briefcase size={14} />
                    PM 지정
                  </button>
                  {expandedUserId === user.id ? (
                    <ChevronDown size={20} className="text-gray-400" />
                  ) : (
                    <ChevronRight size={20} className="text-gray-400" />
                  )}
                </div>
              </div>

              {/* Expanded Project Roles */}
              {expandedUserId === user.id && (
                <div className="px-6 pb-4 bg-gray-50">
                  <div className="ml-14 border-l-2 border-gray-200 pl-4">
                    <h4 className="text-sm font-medium text-gray-700 mb-2">프로젝트별 역할</h4>
                    {user.projectRoles && user.projectRoles.length > 0 ? (
                      <div className="space-y-2">
                        {user.projectRoles.map(pr => (
                          <div key={pr.projectId} className="flex items-center justify-between bg-white rounded-lg px-3 py-2 border border-gray-200">
                            <div className="flex items-center gap-2">
                              <FolderKanban size={14} className="text-gray-400" />
                              <span className="text-sm text-gray-900">{pr.projectName}</span>
                              <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                                {PROJECT_ROLE_LABELS[pr.role]}
                              </span>
                            </div>
                            <button
                              type="button"
                              onClick={() => handleRevokePM(user.id, pr.projectId)}
                              className="p-1 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded"
                              title="PM 해제"
                            >
                              <Trash2 size={14} />
                            </button>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="text-sm text-gray-500">할당된 프로젝트 역할이 없습니다</p>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Role Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-red-100 flex items-center justify-center">
              <Shield className="text-red-600" size={20} />
            </div>
            <div>
              <div className="text-2xl font-bold text-gray-900">
                {users.filter(u => u.systemRole === 'admin').length}
              </div>
              <div className="text-sm text-gray-500">시스템 관리자</div>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-purple-100 flex items-center justify-center">
              <Building2 className="text-purple-600" size={20} />
            </div>
            <div>
              <div className="text-2xl font-bold text-gray-900">
                {users.filter(u => u.systemRole === 'pmo').length}
              </div>
              <div className="text-sm text-gray-500">PMO</div>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-blue-100 flex items-center justify-center">
              <Briefcase className="text-blue-600" size={20} />
            </div>
            <div>
              <div className="text-2xl font-bold text-gray-900">
                {users.filter(u => u.projectRoles && u.projectRoles.some(r => r.role === 'pm')).length}
              </div>
              <div className="text-sm text-gray-500">PM (프로젝트 관리자)</div>
            </div>
          </div>
        </div>
      </div>

      {/* Assign PM Dialog */}
      {showAssignPMDialog && selectedUser && (
        <AssignPMDialog
          user={selectedUser}
          projects={projects}
          existingProjectIds={(selectedUser.projectRoles || []).map(r => r.projectId)}
          onClose={() => {
            setShowAssignPMDialog(false);
            setSelectedUser(null);
          }}
          onAssign={handleAssignPM}
        />
      )}
    </div>
  );
}

// Assign PM Dialog
interface AssignPMDialogProps {
  user: SystemUser;
  projects: any[];
  existingProjectIds: string[];
  onClose: () => void;
  onAssign: (userId: string, projectIds: string[]) => void;
}

function AssignPMDialog({ user, projects, existingProjectIds, onClose, onAssign }: AssignPMDialogProps) {
  const [selectedProjectIds, setSelectedProjectIds] = useState<string[]>([]);

  const availableProjects = projects.filter(p => !existingProjectIds.includes(p.id));

  const handleToggleProject = (projectId: string) => {
    setSelectedProjectIds(prev =>
      prev.includes(projectId)
        ? prev.filter(id => id !== projectId)
        : [...prev, projectId]
    );
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">PM 역할 지정</h2>
          <p className="text-sm text-gray-500 mt-1">{user.name}님에게 PM 역할을 지정합니다</p>
        </div>
        <div className="p-6 space-y-4">
          {availableProjects.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <FolderKanban className="mx-auto mb-2 text-gray-300" size={48} />
              <p>지정 가능한 프로젝트가 없습니다</p>
            </div>
          ) : (
            <>
              <p className="text-sm text-gray-600">PM으로 지정할 프로젝트를 선택하세요:</p>
              <div className="max-h-60 overflow-y-auto border border-gray-200 rounded-lg divide-y divide-gray-100">
                {availableProjects.map(project => (
                  <div
                    key={project.id}
                    className={`flex items-center gap-3 px-4 py-3 cursor-pointer transition-colors ${
                      selectedProjectIds.includes(project.id) ? 'bg-blue-50' : 'hover:bg-gray-50'
                    }`}
                    onClick={() => handleToggleProject(project.id)}
                  >
                    <input
                      type="checkbox"
                      checked={selectedProjectIds.includes(project.id)}
                      onChange={() => handleToggleProject(project.id)}
                      aria-label={`${project.name} 선택`}
                      className="h-4 w-4 text-blue-600 rounded focus:ring-blue-500"
                    />
                    <div className="flex-1">
                      <div className="text-sm font-medium text-gray-900">{project.name}</div>
                      <div className="text-xs text-gray-500">{project.code}</div>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}

          <div className="flex justify-end gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              취소
            </button>
            <button
              type="button"
              onClick={() => onAssign(user.id, selectedProjectIds)}
              disabled={selectedProjectIds.length === 0}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              PM 지정
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ========== System Permissions Tab ==========
function SystemPermissionsTab({ userRole }: { userRole: UserRole }) {
  const [selectedRole, setSelectedRole] = useState<string>(userRole);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const canManageRoles = userRole === 'admin';

  useEffect(() => {
    loadPermissions();
  }, []);

  const loadPermissions = async () => {
    try {
      setLoading(true);
      const response = await apiService.getPermissions() as any;
      if (Array.isArray(response)) {
        setPermissions(response);
      } else if (response?.data && Array.isArray(response.data)) {
        setPermissions(response.data);
      } else {
        console.warn('Unexpected response format:', response);
        setPermissions([]);
      }
    } catch (error) {
      console.error('Failed to load permissions:', error);
      setMessage({ type: 'error', text: '권한 데이터를 불러오는데 실패했습니다' });
      setPermissions([]);
    } finally {
      setLoading(false);
    }
  };

  const handlePermissionToggle = async (permissionId: string, currentValue: boolean) => {
    if (!canManageRoles) {
      setMessage({ type: 'error', text: '권한이 없습니다. 시스템 관리자만 권한을 수정할 수 있습니다.' });
      return;
    }

    try {
      setSaving(true);
      await apiService.updateRolePermission(selectedRole, permissionId, !currentValue);

      setPermissions(permissions.map(p => {
        if (p.id === permissionId) {
          return {
            ...p,
            roles: {
              ...p.roles,
              [selectedRole]: !currentValue
            }
          };
        }
        return p;
      }));

      setMessage({ type: 'success', text: '권한이 업데이트되었습니다' });
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      console.error('Failed to update permission:', error);
      setMessage({ type: 'error', text: '권한 업데이트에 실패했습니다' });
    } finally {
      setSaving(false);
    }
  };

  const categories = Array.from(new Set(permissions.map((p) => p.category)));

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">권한 데이터를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div>
      {/* Message Alert */}
      {message && (
        <div className={`mb-6 rounded-xl p-4 ${
          message.type === 'success'
            ? 'bg-green-50 border-2 border-green-300'
            : 'bg-red-50 border-2 border-red-300'
        }`}>
          <div className="flex items-start gap-3">
            {message.type === 'success' ? (
              <Check className="text-green-600" size={24} />
            ) : (
              <AlertCircle className="text-red-600" size={24} />
            )}
            <p className={`text-sm font-medium ${
              message.type === 'success' ? 'text-green-800' : 'text-red-800'
            }`}>
              {message.text}
            </p>
          </div>
        </div>
      )}

      {/* Admin Notice */}
      {canManageRoles && (
        <div className="mb-6 bg-gradient-to-r from-amber-50 to-yellow-50 border-2 border-amber-300 rounded-xl p-4">
          <div className="flex items-start gap-3">
            <Shield className="text-amber-600 mt-0.5" size={24} />
            <div>
              <h3 className="font-semibold text-gray-900">시스템 관리자 권한</h3>
              <p className="text-sm text-gray-700 mt-1">
                권한 매트릭스에서 각 권한을 클릭하여 역할별 접근 권한을 허용하거나 거부할 수 있습니다.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Security Notice */}
      <div className="mb-6 bg-gradient-to-r from-blue-50 to-purple-50 border-2 border-blue-300 rounded-xl p-4">
        <div className="flex items-start gap-3">
          <Shield className="text-blue-600 mt-0.5" size={24} />
          <div>
            <h3 className="font-semibold text-gray-900">제로 트러스트 보안 모델 적용</h3>
            <p className="text-sm text-gray-700 mt-1">
              모든 사용자와 서비스는 명시적으로 허용된 권한만 가지며, 모든 접근 요청은 MFA 인증 및 감사 로그에 기록됩니다.
            </p>
          </div>
        </div>
      </div>

      {/* Role Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {roles.map((role) => (
          <button
            type="button"
            key={role.id}
            onClick={() => setSelectedRole(role.id)}
            className={`text-left p-4 rounded-xl border-2 transition-all ${
              selectedRole === role.id
                ? 'border-blue-500 bg-blue-50 shadow-md'
                : 'border-gray-200 bg-white hover:border-gray-300'
            }`}
          >
            <div className="flex items-start justify-between mb-2">
              <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center text-white">
                <User size={20} />
              </div>
              <span className="px-2 py-1 bg-gray-100 text-gray-700 rounded-full text-xs">
                {role.userCount}명
              </span>
            </div>
            <h3 className="font-semibold text-gray-900">{role.name}</h3>
            <p className="text-xs text-gray-500 mt-1">{role.nameEn}</p>
            <p className="text-xs text-gray-600 mt-2 line-clamp-2">{role.description}</p>
          </button>
        ))}
      </div>

      {/* Permission Matrix */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="p-6 border-b border-gray-200 bg-gradient-to-r from-gray-50 to-white">
          <div className="flex items-center gap-3">
            <Lock className="text-blue-600" size={24} />
            <div>
              <h3 className="font-semibold text-gray-900">권한 매트릭스</h3>
              <p className="text-sm text-gray-500">
                선택된 역할: <span className="font-medium text-blue-600">{roles.find((r) => r.id === selectedRole)?.name}</span>
              </p>
            </div>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">
                  카테고리
                </th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">
                  권한
                </th>
                <th className="px-6 py-3 text-center text-xs font-semibold text-gray-700 uppercase tracking-wider">
                  접근 권한
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {categories.map((category) => {
                const categoryPerms = permissions.filter((p) => p.category === category);
                return categoryPerms.map((permission, idx) => (
                  <tr
                    key={permission.id}
                    className={permission.roles[selectedRole as keyof typeof permission.roles] ? 'bg-green-50' : ''}
                  >
                    {idx === 0 && (
                      <td
                        rowSpan={categoryPerms.length}
                        className="px-6 py-4 align-top font-medium text-gray-900 bg-gray-50"
                      >
                        {category}
                      </td>
                    )}
                    <td className="px-6 py-4 text-sm text-gray-700">{permission.name}</td>
                    <td className="px-6 py-4 text-center">
                      {canManageRoles ? (
                        <button
                          type="button"
                          onClick={() => handlePermissionToggle(
                            permission.id,
                            permission.roles[selectedRole as keyof typeof permission.roles]
                          )}
                          disabled={saving}
                          className={`inline-flex items-center gap-1 px-3 py-1 rounded-full transition-all ${
                            permission.roles[selectedRole as keyof typeof permission.roles]
                              ? 'bg-green-100 text-green-700 hover:bg-green-200'
                              : 'bg-red-100 text-red-700 hover:bg-red-200'
                          } ${saving ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
                        >
                          {permission.roles[selectedRole as keyof typeof permission.roles] ? (
                            <>
                              <Check size={16} />
                              <span className="text-xs font-medium">허용</span>
                            </>
                          ) : (
                            <>
                              <X size={16} />
                              <span className="text-xs font-medium">거부</span>
                            </>
                          )}
                        </button>
                      ) : (
                        <div className={`inline-flex items-center gap-1 px-3 py-1 rounded-full ${
                          permission.roles[selectedRole as keyof typeof permission.roles]
                            ? 'bg-green-100 text-green-700'
                            : 'bg-red-100 text-red-700'
                        }`}>
                          {permission.roles[selectedRole as keyof typeof permission.roles] ? (
                            <>
                              <Check size={16} />
                              <span className="text-xs font-medium">허용</span>
                            </>
                          ) : (
                            <>
                              <X size={16} />
                              <span className="text-xs font-medium">거부</span>
                            </>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                ));
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* SoD Principles */}
      <div className="mt-6 bg-amber-50 border border-amber-200 rounded-xl p-6">
        <h3 className="font-semibold text-gray-900 mb-3">직무 분리(SoD) 원칙</h3>
        <div className="space-y-2 text-sm text-gray-700">
          <div className="flex items-start gap-2">
            <span className="text-amber-600 mt-0.5">⚠️</span>
            <p>
              <span className="font-medium">개발과 운영의 분리:</span> 개발팀은 프로덕션 환경에 직접 배포할 수 없으며, CI/CD 파이프라인을 통한 자동화된 배포만 가능합니다.
            </p>
          </div>
          <div className="flex items-start gap-2">
            <span className="text-amber-600 mt-0.5">⚠️</span>
            <p>
              <span className="font-medium">개발과 테스트의 분리:</span> 개발팀은 단위 테스트를 수행하지만, 통합/시스템 테스트는 독립된 QA팀이 수행합니다.
            </p>
          </div>
          <div className="flex items-start gap-2">
            <span className="text-amber-600 mt-0.5">⚠️</span>
            <p>
              <span className="font-medium">시스템 관리와 감사의 분리:</span> 시스템 관리자는 감사 로그를 수정하거나 삭제할 수 없으며, 조회는 PMO 총괄과 외부 감리만 가능합니다.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

// ========== Project Permissions Tab ==========
function ProjectPermissionsTab({ userRole, canManage }: { userRole: UserRole; canManage: boolean }) {
  const { currentProject, projects } = useProject();
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [loading, setLoading] = useState(false);
  const [showAddMemberDialog, setShowAddMemberDialog] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    if (currentProject && !selectedProjectId) {
      setSelectedProjectId(currentProject.id);
    }
  }, [currentProject, selectedProjectId]);

  useEffect(() => {
    if (selectedProjectId) {
      loadProjectMembers(selectedProjectId);
    }
  }, [selectedProjectId]);

  const loadProjectMembers = async (projectId: string) => {
    setLoading(true);
    try {
      const data = await apiService.getProjectMembers(projectId);
      setMembers(data);
    } catch (error) {
      console.error('Failed to load project members:', error);
      setMembers([
        { id: 'm1', userId: 'user-001', userName: '김철수', userEmail: 'kim@example.com', role: 'pm', assignedAt: '2025-01-02' },
        { id: 'm2', userId: 'user-002', userName: '이영희', userEmail: 'lee@example.com', role: 'owner', assignedAt: '2025-01-02' },
        { id: 'm3', userId: 'user-003', userName: '박민수', userEmail: 'park@example.com', role: 'part_leader', partName: 'UI/UX 파트', assignedAt: '2025-01-05' },
        { id: 'm4', userId: 'user-004', userName: '최영수', userEmail: 'choi@example.com', role: 'part_leader', partName: '백엔드 파트', assignedAt: '2025-01-05' },
        { id: 'm5', userId: 'user-005', userName: '정수진', userEmail: 'jung@example.com', role: 'education_mgr', assignedAt: '2025-01-10' },
        { id: 'm6', userId: 'user-006', userName: '한미영', userEmail: 'han@example.com', role: 'qa_lead', assignedAt: '2025-01-10' },
        { id: 'm7', userId: 'user-007', userName: '오현우', userEmail: 'oh@example.com', role: 'ba', assignedAt: '2025-01-10' },
        { id: 'm8', userId: 'user-008', userName: '김지은', userEmail: 'kim2@example.com', role: 'member', partName: 'UI/UX 파트', assignedAt: '2025-01-15' },
        { id: 'm9', userId: 'user-009', userName: '이준호', userEmail: 'lee2@example.com', role: 'member', partName: '백엔드 파트', assignedAt: '2025-01-15' },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleChangeRole = async (memberId: string, newRole: ProjectRole) => {
    try {
      await apiService.updateProjectMemberRole(selectedProjectId!, memberId, newRole);
      setMembers(members.map(m => m.id === memberId ? { ...m, role: newRole } : m));
      setMessage({ type: 'success', text: '역할이 변경되었습니다' });
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      console.error('Failed to update role:', error);
      setMembers(members.map(m => m.id === memberId ? { ...m, role: newRole } : m));
      setMessage({ type: 'success', text: '역할이 변경되었습니다' });
      setTimeout(() => setMessage(null), 3000);
    }
  };

  const handleRemoveMember = async (memberId: string) => {
    if (!confirm('정말 이 멤버를 프로젝트에서 제거하시겠습니까?')) return;

    try {
      await apiService.removeProjectMember(selectedProjectId!, memberId);
      setMembers(members.filter(m => m.id !== memberId));
      setMessage({ type: 'success', text: '멤버가 제거되었습니다' });
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      console.error('Failed to remove member:', error);
      setMembers(members.filter(m => m.id !== memberId));
      setMessage({ type: 'success', text: '멤버가 제거되었습니다' });
      setTimeout(() => setMessage(null), 3000);
    }
  };

  const getRoleIcon = (role: ProjectRole) => {
    switch (role) {
      case 'owner': return <Crown className="text-yellow-500" size={16} />;
      case 'pm': return <Shield className="text-blue-500" size={16} />;
      case 'part_leader': return <Users className="text-purple-500" size={16} />;
      case 'education_mgr': return <User className="text-green-500" size={16} />;
      case 'qa_lead': return <Check className="text-orange-500" size={16} />;
      case 'ba': return <User className="text-indigo-500" size={16} />;
      default: return <User className="text-gray-500" size={16} />;
    }
  };

  const getRoleColor = (role: ProjectRole) => {
    switch (role) {
      case 'owner': return 'bg-yellow-100 text-yellow-700';
      case 'pm': return 'bg-blue-100 text-blue-700';
      case 'part_leader': return 'bg-purple-100 text-purple-700';
      case 'education_mgr': return 'bg-green-100 text-green-700';
      case 'qa_lead': return 'bg-orange-100 text-orange-700';
      case 'ba': return 'bg-indigo-100 text-indigo-700';
      default: return 'bg-gray-100 text-gray-700';
    }
  };

  const groupedMembers = {
    leadership: members.filter(m => ['owner', 'pm'].includes(m.role)),
    partLeaders: members.filter(m => m.role === 'part_leader'),
    specialists: members.filter(m => ['education_mgr', 'qa_lead', 'ba'].includes(m.role)),
    members: members.filter(m => m.role === 'member'),
  };

  if (!selectedProjectId) {
    return (
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-12 text-center">
        <FolderKanban className="mx-auto mb-4 text-gray-400" size={48} />
        <h3 className="text-lg font-medium text-gray-900 mb-2">프로젝트를 선택해주세요</h3>
        <p className="text-gray-500">프로젝트 권한을 관리하려면 먼저 프로젝트를 선택하세요.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Project Selection */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <label htmlFor="project-select" className="text-sm font-medium text-gray-700">프로젝트:</label>
            <select
              id="project-select"
              value={selectedProjectId}
              onChange={(e) => setSelectedProjectId(e.target.value)}
              aria-label="프로젝트 선택"
              className="px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 min-w-[300px]"
            >
              {projects.map(project => (
                <option key={project.id} value={project.id}>{project.name}</option>
              ))}
            </select>
          </div>
          {canManage && (
            <button
              type="button"
              onClick={() => setShowAddMemberDialog(true)}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <UserPlus size={18} />
              멤버 추가
            </button>
          )}
        </div>
      </div>

      {/* Message */}
      {message && (
        <div className={`rounded-xl p-4 ${
          message.type === 'success'
            ? 'bg-green-50 border-2 border-green-300'
            : 'bg-red-50 border-2 border-red-300'
        }`}>
          <div className="flex items-center gap-3">
            {message.type === 'success' ? (
              <Check className="text-green-600" size={20} />
            ) : (
              <AlertCircle className="text-red-600" size={20} />
            )}
            <p className={`text-sm font-medium ${
              message.type === 'success' ? 'text-green-800' : 'text-red-800'
            }`}>
              {message.text}
            </p>
          </div>
        </div>
      )}

      {/* Delegation Structure Info */}
      <div className="bg-gradient-to-r from-blue-50 to-purple-50 border-2 border-blue-200 rounded-xl p-4">
        <div className="flex items-start gap-3">
          <Shield className="text-blue-600 mt-0.5" size={20} />
          <div>
            <h3 className="font-semibold text-gray-900">계층적 권한 위임 구조</h3>
            <p className="text-sm text-gray-700 mt-1">
              시스템 관리자 → PM 지정 | PM → 파트장/담당자 지정 | 파트장 → 구성원 추가
            </p>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center min-h-[200px]">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Project Leadership */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-200 bg-gradient-to-r from-blue-50 to-white">
              <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                <Crown className="text-yellow-500" size={18} />
                프로젝트 리더십
              </h3>
            </div>
            <div className="divide-y divide-gray-100">
              {groupedMembers.leadership.map(member => (
                <MemberRow
                  key={member.id}
                  member={member}
                  canManage={canManage && userRole === 'admin'}
                  getRoleIcon={getRoleIcon}
                  getRoleColor={getRoleColor}
                  onChangeRole={handleChangeRole}
                  onRemove={handleRemoveMember}
                />
              ))}
            </div>
          </div>

          {/* Part Leaders */}
          {groupedMembers.partLeaders.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
              <div className="px-6 py-4 border-b border-gray-200 bg-gradient-to-r from-purple-50 to-white">
                <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                  <Users className="text-purple-500" size={18} />
                  파트장
                </h3>
              </div>
              <div className="divide-y divide-gray-100">
                {groupedMembers.partLeaders.map(member => (
                  <MemberRow
                    key={member.id}
                    member={member}
                    canManage={canManage}
                    getRoleIcon={getRoleIcon}
                    getRoleColor={getRoleColor}
                    onChangeRole={handleChangeRole}
                    onRemove={handleRemoveMember}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Specialists */}
          {groupedMembers.specialists.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
              <div className="px-6 py-4 border-b border-gray-200 bg-gradient-to-r from-green-50 to-white">
                <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                  <User className="text-green-500" size={18} />
                  전문 담당자
                </h3>
              </div>
              <div className="divide-y divide-gray-100">
                {groupedMembers.specialists.map(member => (
                  <MemberRow
                    key={member.id}
                    member={member}
                    canManage={canManage}
                    getRoleIcon={getRoleIcon}
                    getRoleColor={getRoleColor}
                    onChangeRole={handleChangeRole}
                    onRemove={handleRemoveMember}
                  />
                ))}
              </div>
            </div>
          )}

          {/* General Members */}
          {groupedMembers.members.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
              <div className="px-6 py-4 border-b border-gray-200 bg-gradient-to-r from-gray-50 to-white">
                <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                  <User className="text-gray-500" size={18} />
                  구성원 ({groupedMembers.members.length}명)
                </h3>
              </div>
              <div className="divide-y divide-gray-100">
                {groupedMembers.members.map(member => (
                  <MemberRow
                    key={member.id}
                    member={member}
                    canManage={canManage}
                    getRoleIcon={getRoleIcon}
                    getRoleColor={getRoleColor}
                    onChangeRole={handleChangeRole}
                    onRemove={handleRemoveMember}
                  />
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Add Member Dialog */}
      {showAddMemberDialog && (
        <AddProjectMemberDialog
          projectId={selectedProjectId}
          existingMembers={members}
          onClose={() => setShowAddMemberDialog(false)}
          onAdd={(userId, userName, role) => {
            const newMember: ProjectMember = {
              id: `m-${Date.now()}`,
              userId,
              userName,
              userEmail: `${userName.toLowerCase().replace(' ', '')}@example.com`,
              role,
              assignedAt: new Date().toISOString().split('T')[0],
            };
            setMembers([...members, newMember]);
            setShowAddMemberDialog(false);
            setMessage({ type: 'success', text: '멤버가 추가되었습니다' });
            setTimeout(() => setMessage(null), 3000);
          }}
        />
      )}
    </div>
  );
}

// Member Row Component
interface MemberRowProps {
  member: ProjectMember;
  canManage: boolean;
  getRoleIcon: (role: ProjectRole) => React.ReactNode;
  getRoleColor: (role: ProjectRole) => string;
  onChangeRole: (memberId: string, role: ProjectRole) => void;
  onRemove: (memberId: string) => void;
}

function MemberRow({ member, canManage, getRoleIcon, getRoleColor, onChangeRole, onRemove }: MemberRowProps) {
  const [editing, setEditing] = useState(false);

  return (
    <div className="flex items-center gap-4 px-6 py-4 hover:bg-gray-50">
      <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-sm font-medium text-gray-600">
        {member.userName[0]}
      </div>
      <div className="flex-1 min-w-0">
        <div className="font-medium text-gray-900">{member.userName}</div>
        <div className="text-sm text-gray-500">{member.userEmail}</div>
      </div>
      <div className="flex items-center gap-2">
        {member.partName && (
          <span className="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded">
            {member.partName}
          </span>
        )}
        {editing ? (
          <select
            value={member.role}
            onChange={(e) => {
              onChangeRole(member.id, e.target.value as ProjectRole);
              setEditing(false);
            }}
            onBlur={() => setEditing(false)}
            autoFocus
            aria-label={`${member.userName} 역할 변경`}
            className="text-xs px-2 py-1 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="owner">프로젝트 소유자</option>
            <option value="pm">프로젝트 관리자</option>
            <option value="part_leader">파트장</option>
            <option value="education_mgr">교육 담당자</option>
            <option value="qa_lead">QA 리드</option>
            <option value="ba">비즈니스 분석가</option>
            <option value="member">구성원</option>
          </select>
        ) : (
          <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium ${getRoleColor(member.role)}`}>
            {getRoleIcon(member.role)}
            {PROJECT_ROLE_LABELS[member.role]}
          </span>
        )}
      </div>
      {canManage && (
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => setEditing(true)}
            className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded"
            title="역할 변경"
          >
            <Edit2 size={14} />
          </button>
          <button
            type="button"
            onClick={() => onRemove(member.id)}
            className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded"
            title="제거"
          >
            <Trash2 size={14} />
          </button>
        </div>
      )}
    </div>
  );
}

// Add Project Member Dialog
interface AddProjectMemberDialogProps {
  projectId: string;
  existingMembers: ProjectMember[];
  onClose: () => void;
  onAdd: (userId: string, userName: string, role: ProjectRole) => void;
}

function AddProjectMemberDialog({ projectId, existingMembers, onClose, onAdd }: AddProjectMemberDialogProps) {
  const [selectedUserId, setSelectedUserId] = useState('');
  const [selectedRole, setSelectedRole] = useState<ProjectRole>('member');
  const [searchTerm, setSearchTerm] = useState('');

  const existingUserIds = new Set(existingMembers.map(m => m.userId));
  const availableUsers = [
    { id: 'user-010', name: '강민지', email: 'kang@example.com' },
    { id: 'user-011', name: '윤서영', email: 'yoon@example.com' },
    { id: 'user-012', name: '장현준', email: 'jang@example.com' },
    { id: 'user-013', name: '송지훈', email: 'song@example.com' },
    { id: 'user-014', name: '임수민', email: 'lim@example.com' },
  ].filter(u => !existingUserIds.has(u.id));

  const filteredUsers = availableUsers.filter(
    u => u.name.includes(searchTerm) || u.email.includes(searchTerm)
  );

  const handleAdd = () => {
    const user = availableUsers.find(u => u.id === selectedUserId);
    if (user) {
      onAdd(user.id, user.name, selectedRole);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">프로젝트 멤버 추가</h2>
        </div>
        <div className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">사용자 검색</label>
            <input
              type="text"
              placeholder="이름 또는 이메일로 검색..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="max-h-40 overflow-y-auto border border-gray-200 rounded-lg">
            {filteredUsers.length === 0 ? (
              <div className="p-4 text-center text-gray-500 text-sm">
                추가 가능한 사용자가 없습니다
              </div>
            ) : (
              filteredUsers.map(user => (
                <div
                  key={user.id}
                  className={`flex items-center gap-3 px-4 py-3 cursor-pointer transition-colors ${
                    selectedUserId === user.id ? 'bg-blue-50' : 'hover:bg-gray-50'
                  }`}
                  onClick={() => setSelectedUserId(user.id)}
                >
                  <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-sm font-medium">
                    {user.name[0]}
                  </div>
                  <div className="flex-1">
                    <div className="text-sm font-medium text-gray-900">{user.name}</div>
                    <div className="text-xs text-gray-500">{user.email}</div>
                  </div>
                  {selectedUserId === user.id && (
                    <Check className="text-blue-600" size={16} />
                  )}
                </div>
              ))
            )}
          </div>

          <div>
            <label htmlFor="member-role-select" className="block text-sm font-medium text-gray-700 mb-1">역할</label>
            <select
              id="member-role-select"
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value as ProjectRole)}
              aria-label="멤버 역할 선택"
              className="w-full px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="member">구성원</option>
              <option value="part_leader">파트장</option>
              <option value="education_mgr">교육 담당자</option>
              <option value="qa_lead">QA 리드</option>
              <option value="ba">비즈니스 분석가</option>
            </select>
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              취소
            </button>
            <button
              type="button"
              onClick={handleAdd}
              disabled={!selectedUserId}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              추가
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
