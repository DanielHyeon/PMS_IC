import { useState, useEffect } from 'react';
import { Shield, User, Lock, Check, X, AlertCircle } from 'lucide-react';
import { UserRole } from '../App';
import { apiService } from '../../services/api';

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
      // Handle both wrapped response (backend) and direct array (mock data)
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

      // Update local state
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
      <div className="p-6 flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">권한 데이터를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-6">
        <h2 className="text-2xl font-semibold text-gray-900">역할 및 권한 관리</h2>
        <p className="text-sm text-gray-500 mt-1">RBAC 기반 접근 제어 및 직무 분리(SoD) 원칙 적용</p>
      </div>

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