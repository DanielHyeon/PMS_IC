import { useState, useEffect } from 'react';
import { Plus, Search, Star, Edit2, Trash2, Users, CheckCircle, Clock, Pause, XCircle, AlertCircle, ChevronRight } from 'lucide-react';
import { Project, ProjectStatus } from '../../types/project';
import { useProject } from '../../contexts/ProjectContext';
import { apiService } from '../../services/api';
import { UserRole } from '../App';

interface ProjectManagementProps {
  userRole: UserRole;
}

// 프로젝트 상태 정보
const PROJECT_STATUS_INFO: Record<ProjectStatus, { label: string; color: string; bgColor: string; icon: React.ElementType }> = {
  PLANNING: { label: '계획', color: 'text-gray-700', bgColor: 'bg-gray-100', icon: Clock },
  IN_PROGRESS: { label: '진행 중', color: 'text-blue-700', bgColor: 'bg-blue-100', icon: CheckCircle },
  ON_HOLD: { label: '보류', color: 'text-yellow-700', bgColor: 'bg-yellow-100', icon: Pause },
  COMPLETED: { label: '완료', color: 'text-green-700', bgColor: 'bg-green-100', icon: CheckCircle },
  CANCELLED: { label: '취소', color: 'text-red-700', bgColor: 'bg-red-100', icon: XCircle },
};

export default function ProjectManagement({ userRole }: ProjectManagementProps) {
  const { currentProject, selectProject, refreshProjects } = useProject();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<ProjectStatus | 'ALL'>('ALL');
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);

  // 관리 권한 체크
  const canManage = ['admin', 'pm', 'pmo_head'].includes(userRole);
  const canCreate = ['admin', 'pm'].includes(userRole);
  const canSetDefault = ['admin', 'pm'].includes(userRole);

  // 프로젝트 목록 로드
  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = async () => {
    setLoading(true);
    try {
      const data = await apiService.getProjects();
      // 전체 프로젝트 정보 로드
      const fullProjects = await Promise.all(
        data.map((p: { id: string }) => apiService.getProject(p.id))
      );
      setProjects(fullProjects);
    } catch (error) {
      console.error('Failed to load projects:', error);
      // Mock 데이터
      setProjects([
        {
          id: '1',
          name: 'AI 기반 손해보험 지급심사 자동화',
          code: 'INS-AI-2025-001',
          description: 'AI/ML 기술을 활용한 보험금 청구 자동 심사 시스템 구축',
          status: 'IN_PROGRESS',
          startDate: '2025-01-02',
          endDate: '2025-12-31',
          budget: 5000000000,
          progress: 62,
          managerId: 'user-001',
          managerName: '김철수',
          isDefault: true,
          createdAt: '2025-01-02T00:00:00Z',
          updatedAt: '2025-01-15T00:00:00Z',
        },
        {
          id: '2',
          name: '차세대 고객관리 시스템',
          code: 'CRM-2025-001',
          description: '통합 고객 관리 시스템 고도화',
          status: 'PLANNING',
          startDate: '2025-03-01',
          endDate: '2025-09-30',
          budget: 2000000000,
          progress: 0,
          managerId: 'user-002',
          managerName: '이영희',
          isDefault: false,
          createdAt: '2025-01-10T00:00:00Z',
          updatedAt: '2025-01-10T00:00:00Z',
        },
        {
          id: '3',
          name: '데이터 품질 고도화',
          code: 'DQ-2024-001',
          description: '전사 데이터 품질 관리 체계 구축',
          status: 'COMPLETED',
          startDate: '2024-01-01',
          endDate: '2024-12-31',
          budget: 1500000000,
          progress: 100,
          managerId: 'user-001',
          managerName: '김철수',
          isDefault: false,
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-12-31T00:00:00Z',
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  // 필터링된 프로젝트 목록
  const filteredProjects = projects.filter((project) => {
    const matchesSearch = project.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (project.code?.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesStatus = statusFilter === 'ALL' || project.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  // 프로젝트 선택
  const handleSelectProject = async (projectId: string) => {
    setSelectedProjectId(projectId);
  };

  // 선택한 프로젝트로 작업하기
  const handleWorkWithProject = async () => {
    if (selectedProjectId) {
      await selectProject(selectedProjectId);
    }
  };

  // 대표 프로젝트 설정
  const handleSetDefault = async (projectId: string) => {
    try {
      // API 호출
      await apiService.setDefaultProject(projectId);
      // 목록 새로고침
      await loadProjects();
      await refreshProjects();
    } catch (error) {
      console.error('Failed to set default project:', error);
      // Mock: 로컬 상태 업데이트
      setProjects((prev) =>
        prev.map((p) => ({
          ...p,
          isDefault: p.id === projectId,
        }))
      );
    }
  };

  // 프로젝트 삭제
  const handleDelete = async (projectId: string) => {
    if (!confirm('정말 이 프로젝트를 삭제하시겠습니까?')) return;
    
    try {
      await apiService.deleteProject(projectId);
      await loadProjects();
      await refreshProjects();
    } catch (error) {
      console.error('Failed to delete project:', error);
      // Mock
      setProjects((prev) => prev.filter((p) => p.id !== projectId));
    }
  };

  // 프로젝트 생성
  const handleCreate = async (data: Partial<Project>) => {
    try {
      await apiService.createProject(data);
      await loadProjects();
      await refreshProjects();
      setShowCreateDialog(false);
    } catch (error) {
      console.error('Failed to create project:', error);
      // Mock
      const newProject: Project = {
        id: `proj-${Date.now()}`,
        name: data.name || '새 프로젝트',
        code: data.code || `PROJ-${Date.now()}`,
        description: data.description || '',
        status: 'PLANNING',
        startDate: data.startDate || new Date().toISOString().split('T')[0],
        endDate: data.endDate || '',
        budget: data.budget || 0,
        progress: 0,
        managerId: 'current-user',
        managerName: '현재 사용자',
        isDefault: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      setProjects((prev) => [...prev, newProject]);
      setShowCreateDialog(false);
    }
  };

  // 프로젝트 수정
  const handleEdit = async (data: Partial<Project>) => {
    if (!editingProject) return;
    
    try {
      await apiService.updateProject(editingProject.id, data);
      await loadProjects();
      await refreshProjects();
      setShowEditDialog(false);
      setEditingProject(null);
    } catch (error) {
      console.error('Failed to update project:', error);
      // Mock
      setProjects((prev) =>
        prev.map((p) =>
          p.id === editingProject.id
            ? { ...p, ...data, updatedAt: new Date().toISOString() }
            : p
        )
      );
      setShowEditDialog(false);
      setEditingProject(null);
    }
  };

  // 금액 포맷팅
  const formatBudget = (budget: number) => {
    if (budget >= 100000000) {
      return `${(budget / 100000000).toFixed(1)}억원`;
    }
    return `${(budget / 10000).toLocaleString()}만원`;
  };

  if (loading) {
    return (
      <div className="p-6 flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">프로젝트 목록을 불러오는 중...</p>
        </div>
      </div>
    );
  }

  const selectedProject = projects.find((p) => p.id === selectedProjectId);

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">프로젝트 관리</h1>
          <p className="text-gray-600 mt-1">전체 프로젝트를 관리하고 작업할 프로젝트를 선택하세요</p>
        </div>
        {canCreate && (
          <button
            onClick={() => setShowCreateDialog(true)}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus size={20} />
            새 프로젝트
          </button>
        )}
      </div>

      {/* 검색 및 필터 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="프로젝트명 또는 코드로 검색..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as ProjectStatus | 'ALL')}
            className="px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="ALL">전체 상태</option>
            {Object.entries(PROJECT_STATUS_INFO).map(([status, info]) => (
              <option key={status} value={status}>{info.label}</option>
            ))}
          </select>
        </div>
      </div>

      {/* 프로젝트 목록 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="w-12 px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                선택
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                프로젝트
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                상태
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                진행률
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                PM
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                예산
              </th>
              {canManage && (
                <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  작업
                </th>
              )}
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {filteredProjects.length === 0 ? (
              <tr>
                <td colSpan={canManage ? 7 : 6} className="px-4 py-12 text-center text-gray-500">
                  <AlertCircle className="mx-auto mb-2 text-gray-400" size={32} />
                  검색 결과가 없습니다
                </td>
              </tr>
            ) : (
              filteredProjects.map((project) => {
                const statusInfo = PROJECT_STATUS_INFO[project.status];
                const StatusIcon = statusInfo.icon;
                const isSelected = selectedProjectId === project.id;
                const isCurrent = currentProject?.id === project.id;
                
                return (
                  <tr
                    key={project.id}
                    className={`hover:bg-gray-50 cursor-pointer transition-colors ${
                      isSelected ? 'bg-blue-50' : ''
                    } ${isCurrent ? 'ring-2 ring-inset ring-blue-500' : ''}`}
                    onClick={() => handleSelectProject(project.id)}
                  >
                    <td className="px-4 py-4">
                      <input
                        type="radio"
                        name="selectedProject"
                        checked={isSelected}
                        onChange={() => handleSelectProject(project.id)}
                        className="h-4 w-4 text-blue-600 focus:ring-blue-500"
                      />
                    </td>
                    <td className="px-4 py-4">
                      <div className="flex items-center gap-2">
                        {project.isDefault && (
                          <Star className="text-yellow-500 fill-yellow-500" size={16} />
                        )}
                        <div>
                          <div className="font-medium text-gray-900">{project.name}</div>
                          <div className="text-sm text-gray-500">{project.code}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-4">
                      <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${statusInfo.bgColor} ${statusInfo.color}`}>
                        <StatusIcon size={12} />
                        {statusInfo.label}
                      </span>
                    </td>
                    <td className="px-4 py-4">
                      <div className="flex items-center gap-2">
                        <div className="w-24 h-2 bg-gray-200 rounded-full overflow-hidden">
                          <div
                            className="h-full bg-blue-600 rounded-full"
                            style={{ width: `${project.progress}%` }}
                          />
                        </div>
                        <span className="text-sm text-gray-600">{project.progress}%</span>
                      </div>
                    </td>
                    <td className="px-4 py-4">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-xs font-medium text-gray-600">
                          {project.managerName?.[0] || 'P'}
                        </div>
                        <span className="text-sm text-gray-900">{project.managerName || '-'}</span>
                      </div>
                    </td>
                    <td className="px-4 py-4 text-sm text-gray-900">
                      {formatBudget(project.budget)}
                    </td>
                    {canManage && (
                      <td className="px-4 py-4 text-right">
                        <div className="flex items-center justify-end gap-2" onClick={(e) => e.stopPropagation()}>
                          <button
                            onClick={() => {
                              setEditingProject(project);
                              setShowEditDialog(true);
                            }}
                            className="p-1.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded"
                            title="편집"
                          >
                            <Edit2 size={16} />
                          </button>
                          <button
                            onClick={() => handleDelete(project.id)}
                            className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded"
                            title="삭제"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    )}
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* 선택된 프로젝트 액션 */}
      {selectedProject && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-blue-100 flex items-center justify-center">
                <Users className="text-blue-600" size={20} />
              </div>
              <div>
                <div className="font-medium text-gray-900">
                  선택된 프로젝트: {selectedProject.name}
                  {selectedProject.isDefault && (
                    <span className="ml-2 text-xs text-yellow-600 font-normal">(대표 프로젝트)</span>
                  )}
                </div>
                <div className="text-sm text-gray-500">
                  {selectedProject.startDate} ~ {selectedProject.endDate}
                </div>
              </div>
            </div>
            <div className="flex items-center gap-2">
              {canSetDefault && !selectedProject.isDefault && (
                <button
                  onClick={() => handleSetDefault(selectedProject.id)}
                  className="flex items-center gap-2 px-4 py-2 border border-yellow-300 text-yellow-700 rounded-lg hover:bg-yellow-50 transition-colors"
                >
                  <Star size={16} />
                  대표로 설정
                </button>
              )}
              <button
                onClick={handleWorkWithProject}
                className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                이 프로젝트로 작업
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 범례 */}
      <div className="text-sm text-gray-500">
        <Star className="inline text-yellow-500 fill-yellow-500" size={14} /> = 대표 프로젝트 (대시보드 기본 표시)
      </div>

      {/* 프로젝트 생성 다이얼로그 */}
      {showCreateDialog && (
        <ProjectDialog
          title="새 프로젝트 생성"
          onClose={() => setShowCreateDialog(false)}
          onSubmit={handleCreate}
        />
      )}

      {/* 프로젝트 수정 다이얼로그 */}
      {showEditDialog && editingProject && (
        <ProjectDialog
          title="프로젝트 수정"
          project={editingProject}
          onClose={() => {
            setShowEditDialog(false);
            setEditingProject(null);
          }}
          onSubmit={handleEdit}
        />
      )}
    </div>
  );
}

// 프로젝트 생성/수정 다이얼로그
interface ProjectDialogProps {
  title: string;
  project?: Project;
  onClose: () => void;
  onSubmit: (data: Partial<Project>) => void;
}

function ProjectDialog({ title, project, onClose, onSubmit }: ProjectDialogProps) {
  const [formData, setFormData] = useState({
    name: project?.name || '',
    code: project?.code || '',
    description: project?.description || '',
    status: project?.status || 'PLANNING',
    startDate: project?.startDate || '',
    endDate: project?.endDate || '',
    budget: project?.budget || 0,
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              프로젝트명 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="프로젝트 이름을 입력하세요"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              프로젝트 코드
            </label>
            <input
              type="text"
              value={formData.code}
              onChange={(e) => setFormData({ ...formData, code: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="예: INS-AI-2025-001"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              설명
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="프로젝트 설명을 입력하세요"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                시작일
              </label>
              <input
                type="date"
                value={formData.startDate}
                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                종료일
              </label>
              <input
                type="date"
                value={formData.endDate}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              예산 (원)
            </label>
            <input
              type="number"
              value={formData.budget}
              onChange={(e) => setFormData({ ...formData, budget: Number(e.target.value) })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="예: 5000000000"
            />
          </div>
          {project && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                상태
              </label>
              <select
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value as ProjectStatus })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                {Object.entries(PROJECT_STATUS_INFO).map(([status, info]) => (
                  <option key={status} value={status}>{info.label}</option>
                ))}
              </select>
            </div>
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
              type="submit"
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              {project ? '수정' : '생성'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
