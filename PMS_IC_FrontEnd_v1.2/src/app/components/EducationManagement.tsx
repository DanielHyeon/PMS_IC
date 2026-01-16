import { useEffect, useState } from 'react';
import {
  BookOpen,
  Calendar,
  Users,
  Plus,
  Pencil,
  Trash2,
  Clock,
  CheckCircle2,
  XCircle,
  MapPin,
  User,
  Award,
  ChevronDown,
  ChevronUp,
  Filter,
  Search,
  GraduationCap,
  Target,
  Layers,
} from 'lucide-react';
import { UserRole } from '../App';
import { apiService } from '../../services/api';

// ========================================
// 타입 정의
// ========================================

interface Education {
  id: string;
  title: string;
  description?: string;
  educationType: 'IT_BASIC' | 'IT_INTERMEDIATE' | 'IT_ADVANCED' | 'BUSINESS_AI_AWARENESS' | 'BUSINESS_CASE_STUDY' | 'POST_DEPLOYMENT';
  category: 'AGENT_AI' | 'MACHINE_LEARNING' | 'DEEP_LEARNING' | 'PYTHON' | 'BUSINESS_PLANNING' | 'BUSINESS_OPERATION' | 'AGENT_ROLE_EXPLANATION';
  targetRole: 'ALL' | 'PM' | 'DEVELOPER' | 'QA' | 'BUSINESS_ANALYST' | 'DATA_SCIENTIST';
  durationHours?: number;
  prerequisites?: string;
  learningObjectives?: string;
  instructor?: string;
  materials?: string;
  isActive?: boolean;
  createdAt?: string;
}

interface EducationSession {
  id: string;
  educationId: string;
  educationTitle?: string;
  sessionName?: string;
  scheduledAt: string;
  endAt?: string;
  location?: string;
  instructor?: string;
  maxParticipants?: number;
  currentParticipants?: number;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  notes?: string;
}

interface EducationRoadmap {
  id: string;
  educationId: string;
  educationTitle?: string;
  targetRole: string;
  level: 'BASIC' | 'INTERMEDIATE' | 'ADVANCED';
  orderNum: number;
  isRequired?: boolean;
  description?: string;
}

type TabType = 'courses' | 'sessions' | 'roadmap';

// ========================================
// 유틸리티 함수
// ========================================

const getEducationTypeLabel = (type: Education['educationType']) => {
  const labels: Record<Education['educationType'], string> = {
    IT_BASIC: 'IT 기초',
    IT_INTERMEDIATE: 'IT 중급',
    IT_ADVANCED: 'IT 고급',
    BUSINESS_AI_AWARENESS: '현업 AI 인식',
    BUSINESS_CASE_STUDY: '현업 사례 연구',
    POST_DEPLOYMENT: '사후 교육',
  };
  return labels[type] || type;
};

const getCategoryLabel = (category: Education['category']) => {
  const labels: Record<Education['category'], string> = {
    AGENT_AI: '에이전트 AI',
    MACHINE_LEARNING: '머신러닝',
    DEEP_LEARNING: '딥러닝',
    PYTHON: '파이썬',
    BUSINESS_PLANNING: '기획',
    BUSINESS_OPERATION: '운영',
    AGENT_ROLE_EXPLANATION: '에이전트 역할 설명',
  };
  return labels[category] || category;
};

const getTargetRoleLabel = (role: Education['targetRole']) => {
  const labels: Record<Education['targetRole'], string> = {
    ALL: '전체',
    PM: 'PM',
    DEVELOPER: '개발자',
    QA: 'QA',
    BUSINESS_ANALYST: '비즈니스 분석가',
    DATA_SCIENTIST: '데이터 과학자',
  };
  return labels[role] || role;
};

const getLevelLabel = (level: EducationRoadmap['level']) => {
  const labels: Record<EducationRoadmap['level'], string> = {
    BASIC: '기초',
    INTERMEDIATE: '중급',
    ADVANCED: '고급',
  };
  return labels[level] || level;
};

const getSessionStatusConfig = (status: EducationSession['status']) => {
  const config: Record<EducationSession['status'], { label: string; color: string; icon: any }> = {
    SCHEDULED: { label: '예정', color: 'bg-blue-100 text-blue-800', icon: Clock },
    IN_PROGRESS: { label: '진행중', color: 'bg-yellow-100 text-yellow-800', icon: Users },
    COMPLETED: { label: '완료', color: 'bg-green-100 text-green-800', icon: CheckCircle2 },
    CANCELLED: { label: '취소', color: 'bg-red-100 text-red-800', icon: XCircle },
  };
  return config[status] || { label: status, color: 'bg-gray-100 text-gray-800', icon: Clock };
};

// ========================================
// 메인 컴포넌트
// ========================================

export default function EducationManagement({ userRole }: { userRole: UserRole }) {
  const [activeTab, setActiveTab] = useState<TabType>('courses');
  const [educations, setEducations] = useState<Education[]>([]);
  const [sessions, setSessions] = useState<EducationSession[]>([]);
  const [roadmaps, setRoadmaps] = useState<EducationRoadmap[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<string>('');
  const [filterRole, setFilterRole] = useState<string>('');

  // Modal states
  const [showEducationModal, setShowEducationModal] = useState(false);
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [editingEducation, setEditingEducation] = useState<Education | null>(null);
  const [editingSession, setEditingSession] = useState<EducationSession | null>(null);
  const [selectedEducationId, setSelectedEducationId] = useState<string | null>(null);

  const canManage = ['pmo_head', 'pm', 'admin'].includes(userRole);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [educationsData, roadmapsData] = await Promise.all([
        apiService.getEducations(),
        apiService.getEducationRoadmaps(),
      ]);
      setEducations(educationsData || []);
      setRoadmaps(roadmapsData || []);
    } catch (error) {
      console.error('Failed to load education data:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadSessions = async (educationId: string) => {
    try {
      const sessionsData = await apiService.getEducationSessions(educationId);
      setSessions(sessionsData || []);
    } catch (error) {
      console.error('Failed to load sessions:', error);
    }
  };

  // Education CRUD
  const handleSaveEducation = async (data: Partial<Education>) => {
    try {
      if (editingEducation) {
        const updated = await apiService.updateEducation(editingEducation.id, data);
        setEducations(prev => prev.map(e => e.id === editingEducation.id ? { ...e, ...updated } : e));
      } else {
        const created = await apiService.createEducation(data);
        setEducations(prev => [created, ...prev]);
      }
      setShowEducationModal(false);
      setEditingEducation(null);
    } catch (error) {
      console.error('Failed to save education:', error);
    }
  };

  const handleDeleteEducation = async (educationId: string) => {
    if (!window.confirm('이 교육 과정을 삭제하시겠습니까?')) return;
    try {
      await apiService.deleteEducation(educationId);
      setEducations(prev => prev.filter(e => e.id !== educationId));
    } catch (error) {
      console.error('Failed to delete education:', error);
    }
  };

  // Session CRUD
  const handleSaveSession = async (data: Partial<EducationSession>) => {
    if (!selectedEducationId) return;
    try {
      if (editingSession) {
        const updated = await apiService.updateEducationSession(selectedEducationId, editingSession.id, data);
        setSessions(prev => prev.map(s => s.id === editingSession.id ? { ...s, ...updated } : s));
      } else {
        const created = await apiService.createEducationSession(selectedEducationId, data);
        setSessions(prev => [created, ...prev]);
      }
      setShowSessionModal(false);
      setEditingSession(null);
    } catch (error) {
      console.error('Failed to save session:', error);
    }
  };

  const handleDeleteSession = async (sessionId: string) => {
    if (!selectedEducationId || !window.confirm('이 세션을 삭제하시겠습니까?')) return;
    try {
      await apiService.deleteEducationSession(selectedEducationId, sessionId);
      setSessions(prev => prev.filter(s => s.id !== sessionId));
    } catch (error) {
      console.error('Failed to delete session:', error);
    }
  };

  // Filtered data
  const filteredEducations = educations.filter(e => {
    if (filterType && e.educationType !== filterType) return false;
    if (filterRole && e.targetRole !== filterRole && e.targetRole !== 'ALL') return false;
    if (searchQuery && !e.title.toLowerCase().includes(searchQuery.toLowerCase())) return false;
    return true;
  });

  const filteredSessions = sessions.filter(s => {
    if (searchQuery && !s.sessionName?.toLowerCase().includes(searchQuery.toLowerCase())) return false;
    return true;
  });

  // Group roadmaps by role and level
  const roadmapMatrix = roadmaps.reduce((acc, r) => {
    const key = `${r.targetRole}-${r.level}`;
    if (!acc[key]) acc[key] = [];
    acc[key].push(r);
    return acc;
  }, {} as Record<string, EducationRoadmap[]>);

  const roles = ['ALL', 'PM', 'DEVELOPER', 'QA', 'BUSINESS_ANALYST', 'DATA_SCIENTIST'];
  const levels: EducationRoadmap['level'][] = ['BASIC', 'INTERMEDIATE', 'ADVANCED'];

  if (loading) {
    return (
      <div className="p-6 flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">교육 데이터를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">교육 관리</h2>
          <p className="text-sm text-gray-500 mt-1">IT/현업 대상 교육 과정 및 로드맵 관리</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          {[
            { id: 'courses', label: '교육 과정', icon: BookOpen },
            { id: 'sessions', label: '교육 세션', icon: Calendar },
            { id: 'roadmap', label: '로드맵 매트릭스', icon: Layers },
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as TabType)}
              className={`flex items-center gap-2 py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === tab.id
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <tab.icon className="w-5 h-5" />
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Search and Filter Bar */}
      <div className="flex flex-wrap gap-4 items-center">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>
        {activeTab === 'courses' && (
          <>
            <select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            >
              <option value="">모든 유형</option>
              <option value="IT_BASIC">IT 기초</option>
              <option value="IT_INTERMEDIATE">IT 중급</option>
              <option value="IT_ADVANCED">IT 고급</option>
              <option value="BUSINESS_AI_AWARENESS">현업 AI 인식</option>
              <option value="BUSINESS_CASE_STUDY">현업 사례 연구</option>
            </select>
            <select
              value={filterRole}
              onChange={(e) => setFilterRole(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            >
              <option value="">모든 대상</option>
              <option value="PM">PM</option>
              <option value="DEVELOPER">개발자</option>
              <option value="QA">QA</option>
              <option value="BUSINESS_ANALYST">비즈니스 분석가</option>
            </select>
          </>
        )}
        {canManage && activeTab === 'courses' && (
          <button
            onClick={() => { setEditingEducation(null); setShowEducationModal(true); }}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <Plus className="w-4 h-4" />
            교육 과정 추가
          </button>
        )}
      </div>

      {/* Content */}
      {activeTab === 'courses' && (
        <div className="grid gap-4">
          {filteredEducations.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              등록된 교육 과정이 없습니다.
            </div>
          ) : (
            filteredEducations.map(education => (
              <div key={education.id} className="bg-white rounded-lg shadow p-6 hover:shadow-md transition-shadow">
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="text-lg font-semibold text-gray-900">{education.title}</h3>
                      <span className="px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-800">
                        {getEducationTypeLabel(education.educationType)}
                      </span>
                      <span className="px-2 py-1 text-xs rounded-full bg-purple-100 text-purple-800">
                        {getCategoryLabel(education.category)}
                      </span>
                    </div>
                    <p className="text-gray-600 text-sm mb-3">{education.description}</p>
                    <div className="flex flex-wrap gap-4 text-sm text-gray-500">
                      <span className="flex items-center gap-1">
                        <Target className="w-4 h-4" />
                        대상: {getTargetRoleLabel(education.targetRole)}
                      </span>
                      {education.durationHours && (
                        <span className="flex items-center gap-1">
                          <Clock className="w-4 h-4" />
                          {education.durationHours}시간
                        </span>
                      )}
                      {education.instructor && (
                        <span className="flex items-center gap-1">
                          <User className="w-4 h-4" />
                          {education.instructor}
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => { setSelectedEducationId(education.id); loadSessions(education.id); setActiveTab('sessions'); }}
                      className="p-2 text-blue-600 hover:bg-blue-50 rounded"
                      title="세션 보기"
                    >
                      <Calendar className="w-4 h-4" />
                    </button>
                    {canManage && (
                      <>
                        <button
                          onClick={() => { setEditingEducation(education); setShowEducationModal(true); }}
                          className="p-2 text-gray-600 hover:bg-gray-100 rounded"
                          title="수정"
                        >
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDeleteEducation(education.id)}
                          className="p-2 text-red-600 hover:bg-red-50 rounded"
                          title="삭제"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {activeTab === 'sessions' && (
        <div className="space-y-4">
          {selectedEducationId && canManage && (
            <button
              onClick={() => { setEditingSession(null); setShowSessionModal(true); }}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              <Plus className="w-4 h-4" />
              세션 추가
            </button>
          )}
          {!selectedEducationId ? (
            <div className="text-center py-12 text-gray-500">
              교육 과정을 선택해주세요.
            </div>
          ) : filteredSessions.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              등록된 세션이 없습니다.
            </div>
          ) : (
            <div className="grid gap-4">
              {filteredSessions.map(session => {
                const statusConfig = getSessionStatusConfig(session.status);
                const StatusIcon = statusConfig.icon;
                return (
                  <div key={session.id} className="bg-white rounded-lg shadow p-6">
                    <div className="flex justify-between items-start">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <h3 className="text-lg font-semibold text-gray-900">
                            {session.sessionName || session.educationTitle}
                          </h3>
                          <span className={`px-2 py-1 text-xs rounded-full flex items-center gap-1 ${statusConfig.color}`}>
                            <StatusIcon className="w-3 h-3" />
                            {statusConfig.label}
                          </span>
                        </div>
                        <div className="flex flex-wrap gap-4 text-sm text-gray-500">
                          <span className="flex items-center gap-1">
                            <Calendar className="w-4 h-4" />
                            {new Date(session.scheduledAt).toLocaleString('ko-KR')}
                          </span>
                          {session.location && (
                            <span className="flex items-center gap-1">
                              <MapPin className="w-4 h-4" />
                              {session.location}
                            </span>
                          )}
                          {session.instructor && (
                            <span className="flex items-center gap-1">
                              <User className="w-4 h-4" />
                              {session.instructor}
                            </span>
                          )}
                          <span className="flex items-center gap-1">
                            <Users className="w-4 h-4" />
                            {session.currentParticipants || 0} / {session.maxParticipants || '무제한'}명
                          </span>
                        </div>
                      </div>
                      {canManage && (
                        <div className="flex gap-2">
                          <button
                            onClick={() => { setEditingSession(session); setShowSessionModal(true); }}
                            className="p-2 text-gray-600 hover:bg-gray-100 rounded"
                          >
                            <Pencil className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDeleteSession(session.id)}
                            className="p-2 text-red-600 hover:bg-red-50 rounded"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {activeTab === 'roadmap' && (
        <div className="bg-white rounded-lg shadow overflow-x-auto">
          <table className="min-w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  역할 / 레벨
                </th>
                {levels.map(level => (
                  <th key={level} className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {getLevelLabel(level)}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {roles.map(role => (
                <tr key={role} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap font-medium text-gray-900">
                    {getTargetRoleLabel(role as Education['targetRole'])}
                  </td>
                  {levels.map(level => {
                    const items = roadmapMatrix[`${role}-${level}`] || [];
                    return (
                      <td key={level} className="px-6 py-4">
                        <div className="space-y-2">
                          {items.map(item => (
                            <div
                              key={item.id}
                              className={`p-2 rounded text-sm ${
                                item.isRequired
                                  ? 'bg-blue-100 text-blue-800 border border-blue-300'
                                  : 'bg-gray-100 text-gray-700'
                              }`}
                            >
                              <div className="font-medium">{item.educationTitle}</div>
                              {item.isRequired && (
                                <span className="text-xs">(필수)</span>
                              )}
                            </div>
                          ))}
                          {items.length === 0 && (
                            <span className="text-gray-400 text-sm">-</span>
                          )}
                        </div>
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Education Modal */}
      {showEducationModal && (
        <EducationModal
          education={editingEducation}
          onSave={handleSaveEducation}
          onClose={() => { setShowEducationModal(false); setEditingEducation(null); }}
        />
      )}

      {/* Session Modal */}
      {showSessionModal && selectedEducationId && (
        <SessionModal
          session={editingSession}
          onSave={handleSaveSession}
          onClose={() => { setShowSessionModal(false); setEditingSession(null); }}
        />
      )}
    </div>
  );
}

// ========================================
// Education Modal
// ========================================

function EducationModal({
  education,
  onSave,
  onClose,
}: {
  education: Education | null;
  onSave: (data: Partial<Education>) => void;
  onClose: () => void;
}) {
  const [formData, setFormData] = useState<Partial<Education>>(
    education || {
      title: '',
      description: '',
      educationType: 'IT_BASIC',
      category: 'AGENT_AI',
      targetRole: 'ALL',
      durationHours: undefined,
      instructor: '',
      prerequisites: '',
      learningObjectives: '',
    }
  );

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b">
          <h3 className="text-lg font-semibold">
            {education ? '교육 과정 수정' : '교육 과정 추가'}
          </h3>
        </div>
        <div className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">제목 *</label>
            <input
              type="text"
              value={formData.title || ''}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">설명</label>
            <textarea
              value={formData.description || ''}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              rows={3}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">교육 유형</label>
              <select
                value={formData.educationType || 'IT_BASIC'}
                onChange={(e) => setFormData({ ...formData, educationType: e.target.value as Education['educationType'] })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              >
                <option value="IT_BASIC">IT 기초</option>
                <option value="IT_INTERMEDIATE">IT 중급</option>
                <option value="IT_ADVANCED">IT 고급</option>
                <option value="BUSINESS_AI_AWARENESS">현업 AI 인식</option>
                <option value="BUSINESS_CASE_STUDY">현업 사례 연구</option>
                <option value="POST_DEPLOYMENT">사후 교육</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">카테고리</label>
              <select
                value={formData.category || 'AGENT_AI'}
                onChange={(e) => setFormData({ ...formData, category: e.target.value as Education['category'] })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              >
                <option value="AGENT_AI">에이전트 AI</option>
                <option value="MACHINE_LEARNING">머신러닝</option>
                <option value="DEEP_LEARNING">딥러닝</option>
                <option value="PYTHON">파이썬</option>
                <option value="BUSINESS_PLANNING">기획</option>
                <option value="BUSINESS_OPERATION">운영</option>
                <option value="AGENT_ROLE_EXPLANATION">에이전트 역할 설명</option>
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">대상 역할</label>
              <select
                value={formData.targetRole || 'ALL'}
                onChange={(e) => setFormData({ ...formData, targetRole: e.target.value as Education['targetRole'] })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              >
                <option value="ALL">전체</option>
                <option value="PM">PM</option>
                <option value="DEVELOPER">개발자</option>
                <option value="QA">QA</option>
                <option value="BUSINESS_ANALYST">비즈니스 분석가</option>
                <option value="DATA_SCIENTIST">데이터 과학자</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">교육 시간 (시간)</label>
              <input
                type="number"
                value={formData.durationHours || ''}
                onChange={(e) => setFormData({ ...formData, durationHours: parseInt(e.target.value) || undefined })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">강사</label>
            <input
              type="text"
              value={formData.instructor || ''}
              onChange={(e) => setFormData({ ...formData, instructor: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">선수 과목</label>
            <input
              type="text"
              value={formData.prerequisites || ''}
              onChange={(e) => setFormData({ ...formData, prerequisites: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">학습 목표</label>
            <textarea
              value={formData.learningObjectives || ''}
              onChange={(e) => setFormData({ ...formData, learningObjectives: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              rows={2}
            />
          </div>
        </div>
        <div className="p-6 border-t flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
          >
            취소
          </button>
          <button
            onClick={() => onSave(formData)}
            className="px-4 py-2 text-white bg-blue-600 rounded-lg hover:bg-blue-700"
          >
            저장
          </button>
        </div>
      </div>
    </div>
  );
}

// ========================================
// Session Modal
// ========================================

function SessionModal({
  session,
  onSave,
  onClose,
}: {
  session: EducationSession | null;
  onSave: (data: Partial<EducationSession>) => void;
  onClose: () => void;
}) {
  const [formData, setFormData] = useState<Partial<EducationSession>>(
    session || {
      sessionName: '',
      scheduledAt: '',
      location: '',
      instructor: '',
      maxParticipants: undefined,
      status: 'SCHEDULED',
      notes: '',
    }
  );

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg">
        <div className="p-6 border-b">
          <h3 className="text-lg font-semibold">
            {session ? '세션 수정' : '세션 추가'}
          </h3>
        </div>
        <div className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">세션명</label>
            <input
              type="text"
              value={formData.sessionName || ''}
              onChange={(e) => setFormData({ ...formData, sessionName: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">일시 *</label>
            <input
              type="datetime-local"
              value={formData.scheduledAt?.slice(0, 16) || ''}
              onChange={(e) => setFormData({ ...formData, scheduledAt: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">장소</label>
            <input
              type="text"
              value={formData.location || ''}
              onChange={(e) => setFormData({ ...formData, location: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">강사</label>
              <input
                type="text"
                value={formData.instructor || ''}
                onChange={(e) => setFormData({ ...formData, instructor: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">최대 인원</label>
              <input
                type="number"
                value={formData.maxParticipants || ''}
                onChange={(e) => setFormData({ ...formData, maxParticipants: parseInt(e.target.value) || undefined })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
          {session && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">상태</label>
              <select
                value={formData.status || 'SCHEDULED'}
                onChange={(e) => setFormData({ ...formData, status: e.target.value as EducationSession['status'] })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              >
                <option value="SCHEDULED">예정</option>
                <option value="IN_PROGRESS">진행중</option>
                <option value="COMPLETED">완료</option>
                <option value="CANCELLED">취소</option>
              </select>
            </div>
          )}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">메모</label>
            <textarea
              value={formData.notes || ''}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              rows={2}
            />
          </div>
        </div>
        <div className="p-6 border-t flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
          >
            취소
          </button>
          <button
            onClick={() => onSave(formData)}
            className="px-4 py-2 text-white bg-blue-600 rounded-lg hover:bg-blue-700"
          >
            저장
          </button>
        </div>
      </div>
    </div>
  );
}
