import { useEffect, useState } from 'react';
import {
  FileText,
  Calendar,
  AlertTriangle,
  Plus,
  Pencil,
  Trash2,
  Clock,
  CheckCircle2,
  XCircle,
  MessageSquare,
  Users,
  MapPin,
  ChevronDown,
  ChevronUp,
  Filter,
  Search,
} from 'lucide-react';
import { UserRole } from '../App';
import { apiService } from '../../services/api';
import { useProject } from '../../contexts/ProjectContext';

// ========================================
// 타입 정의
// ========================================

interface Meeting {
  id: string;
  title: string;
  description?: string;
  meetingType: 'KICKOFF' | 'WEEKLY' | 'MONTHLY' | 'MILESTONE' | 'CLOSING' | 'TECHNICAL' | 'STAKEHOLDER' | 'OTHER';
  scheduledAt: string;
  location?: string;
  organizer?: string;
  attendees: string[];
  minutes?: string;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'POSTPONED';
  actualStartAt?: string;
  actualEndAt?: string;
}

interface Issue {
  id: string;
  title: string;
  description?: string;
  issueType: 'BUG' | 'RISK' | 'BLOCKER' | 'CHANGE_REQUEST' | 'QUESTION' | 'IMPROVEMENT' | 'OTHER';
  priority: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'VERIFIED' | 'CLOSED' | 'REOPENED' | 'DEFERRED';
  assignee?: string;
  reporter?: string;
  reviewer?: string;
  dueDate?: string;
  resolvedAt?: string;
  resolution?: string;
  comments: { author: string; content: string; commentedAt: string }[];
  createdAt: string;
}

interface Deliverable {
  id: string;
  name: string;
  description?: string;
  type: 'DOCUMENT' | 'CODE' | 'REPORT' | 'PRESENTATION' | 'OTHER';
  status: 'PENDING' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED';
  version?: string;
  submissionDate?: string;
  dueDate?: string;
  inspectionDate?: string;
  inspector?: string;
  fileName?: string;
  uploadedBy?: string;
  approver?: string;
  approvedAt?: string;
}

type TabType = 'deliverables' | 'meetings' | 'issues';

// ========================================
// 유틸리티 함수
// ========================================

const getMeetingTypeLabel = (type: Meeting['meetingType']) => {
  const labels: Record<Meeting['meetingType'], string> = {
    KICKOFF: '착수 보고',
    WEEKLY: '주간 보고',
    MONTHLY: '월간 보고',
    MILESTONE: '마일스톤',
    CLOSING: '종료 보고',
    TECHNICAL: '기술 회의',
    STAKEHOLDER: '이해관계자',
    OTHER: '기타',
  };
  return labels[type] || type;
};

const getIssueTypeLabel = (type: Issue['issueType']) => {
  const labels: Record<Issue['issueType'], string> = {
    BUG: '버그',
    RISK: '위험',
    BLOCKER: '장애',
    CHANGE_REQUEST: '변경 요청',
    QUESTION: '문의',
    IMPROVEMENT: '개선',
    OTHER: '기타',
  };
  return labels[type] || type;
};

const getPriorityLabel = (priority: Issue['priority']) => {
  const labels: Record<Issue['priority'], string> = {
    CRITICAL: '긴급',
    HIGH: '높음',
    MEDIUM: '중간',
    LOW: '낮음',
  };
  return labels[priority] || priority;
};

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    // Meeting statuses
    SCHEDULED: 'bg-blue-100 text-blue-800',
    IN_PROGRESS: 'bg-yellow-100 text-yellow-800',
    COMPLETED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-gray-100 text-gray-800',
    POSTPONED: 'bg-orange-100 text-orange-800',
    // Issue statuses
    OPEN: 'bg-red-100 text-red-800',
    RESOLVED: 'bg-green-100 text-green-800',
    VERIFIED: 'bg-emerald-100 text-emerald-800',
    CLOSED: 'bg-gray-100 text-gray-800',
    REOPENED: 'bg-purple-100 text-purple-800',
    DEFERRED: 'bg-slate-100 text-slate-800',
    // Deliverable statuses
    PENDING: 'bg-gray-100 text-gray-800',
    IN_REVIEW: 'bg-blue-100 text-blue-800',
    APPROVED: 'bg-green-100 text-green-800',
    REJECTED: 'bg-red-100 text-red-800',
  };
  return colors[status] || 'bg-gray-100 text-gray-800';
};

const getPriorityColor = (priority: Issue['priority']) => {
  const colors: Record<Issue['priority'], string> = {
    CRITICAL: 'bg-red-100 text-red-800 border-red-300',
    HIGH: 'bg-orange-100 text-orange-800 border-orange-300',
    MEDIUM: 'bg-yellow-100 text-yellow-800 border-yellow-300',
    LOW: 'bg-green-100 text-green-800 border-green-300',
  };
  return colors[priority] || 'bg-gray-100 text-gray-800';
};

const formatDateTime = (dateString?: string) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const formatDate = (dateString?: string) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleDateString('ko-KR');
};

// ========================================
// 메인 컴포넌트
// ========================================

// 초기 Mock 데이터
const initialMeetings: Meeting[] = [
  {
    id: '1',
    title: '프로젝트 킥오프 미팅',
    description: 'AI 자동심사 시스템 프로젝트 착수 회의',
    meetingType: 'KICKOFF',
    scheduledAt: '2025-01-15T10:00:00',
    location: '본사 대회의실',
    organizer: 'PMO 총괄',
    attendees: ['프로젝트 스폰서', 'PMO 총괄', 'PM', '개발팀장', 'QA팀장'],
    status: 'COMPLETED',
    minutes: '프로젝트 목표 및 일정 확정, 역할 분담 완료',
  },
  {
    id: '2',
    title: '주간 진행 점검 회의',
    description: '8월 3주차 진행 현황 점검',
    meetingType: 'WEEKLY',
    scheduledAt: '2025-08-18T14:00:00',
    location: '온라인 (Zoom)',
    organizer: 'PM',
    attendees: ['PM', '개발팀', 'QA팀'],
    status: 'SCHEDULED',
  },
  {
    id: '3',
    title: 'AI 모델 성능 리뷰',
    description: 'OCR 모델 v2.0 성능 평가 및 개선 방안 논의',
    meetingType: 'TECHNICAL',
    scheduledAt: '2025-08-20T15:00:00',
    location: '개발팀 회의실',
    organizer: 'AI팀장',
    attendees: ['AI팀장', 'ML 엔지니어', '데이터 사이언티스트'],
    status: 'SCHEDULED',
  },
];

const initialIssues: Issue[] = [
  {
    id: '1',
    title: 'OCR 인식률 목표치 미달',
    description: '현재 OCR 인식률 93.5%로 목표치 95% 대비 1.5%p 부족',
    issueType: 'RISK',
    priority: 'HIGH',
    status: 'IN_PROGRESS',
    assignee: 'AI팀장',
    reportedBy: 'PM',
    reportedAt: '2025-08-10',
    resolution: '추가 학습 데이터 확보 및 모델 튜닝 진행 중',
  },
  {
    id: '2',
    title: '레거시 시스템 연동 지연',
    description: '기존 보험금 지급 시스템과의 API 연동 일정 지연 예상',
    issueType: 'BLOCKER',
    priority: 'CRITICAL',
    status: 'OPEN',
    assignee: 'SI팀장',
    reportedBy: 'PM',
    reportedAt: '2025-08-15',
  },
  {
    id: '3',
    title: '개인정보 비식별화 검증 필요',
    description: '학습 데이터 비식별화 처리 결과에 대한 정보보호팀 검증 요청',
    issueType: 'CHANGE_REQUEST',
    priority: 'MEDIUM',
    status: 'RESOLVED',
    assignee: '정보보호팀장',
    reportedBy: 'PM',
    reportedAt: '2025-07-20',
    resolution: '비식별화 검증 완료, 승인됨',
    resolvedAt: '2025-08-01',
  },
];

const initialDeliverables: Deliverable[] = [
  {
    id: '1',
    name: 'AS-IS 프로세스 분석 보고서',
    description: '현행 보험금 청구 심사 프로세스 분석',
    type: 'DOCUMENT',
    status: 'APPROVED',
    version: '1.0',
    uploadedAt: '2025-02-10',
    approvedBy: 'PMO 총괄',
  },
  {
    id: '2',
    name: 'AI 모델 설계서',
    description: 'OCR 및 분류 모델 아키텍처 설계 문서',
    type: 'DOCUMENT',
    status: 'IN_REVIEW',
    version: '0.9',
    uploadedAt: '2025-08-12',
  },
  {
    id: '3',
    name: 'API 명세서 초안',
    description: '시스템 연동을 위한 REST API 명세',
    type: 'DOCUMENT',
    status: 'DRAFT',
    version: '0.1',
    uploadedAt: '2025-08-14',
  },
];

export default function CommonManagement({ userRole }: { userRole: UserRole }) {
  const { currentProject } = useProject();
  const [activeTab, setActiveTab] = useState<TabType>('deliverables');
  const [meetings, setMeetings] = useState<Meeting[]>(initialMeetings);
  const [issues, setIssues] = useState<Issue[]>(initialIssues);
  const [deliverables, setDeliverables] = useState<Deliverable[]>(initialDeliverables);
  const [loading, setLoading] = useState(false);

  // 필터 상태
  const [meetingFilter, setMeetingFilter] = useState<string>('');
  const [issueFilter, setIssueFilter] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState('');

  // 모달 상태
  const [showMeetingModal, setShowMeetingModal] = useState(false);
  const [showIssueModal, setShowIssueModal] = useState(false);
  const [editingMeeting, setEditingMeeting] = useState<Meeting | null>(null);
  const [editingIssue, setEditingIssue] = useState<Issue | null>(null);

  // 확장 상태
  const [expandedIssue, setExpandedIssue] = useState<string | null>(null);

  const canEdit = !['auditor', 'business_analyst'].includes(userRole);
  const canManageIssues = ['pm', 'pmo_head', 'developer', 'qa'].includes(userRole);
  const canManageMeetings = ['pm', 'pmo_head'].includes(userRole);

  // 데이터 로드 (프로젝트가 있을 때만 API 호출, 없으면 초기 데이터 유지)
  useEffect(() => {
    if (!currentProject?.id) return;

    const loadData = async () => {
      setLoading(true);
      try {
        const [meetingsData, issuesData] = await Promise.all([
          apiService.getMeetings(currentProject.id),
          apiService.getIssues(currentProject.id),
        ]);
        // API에서 데이터가 있으면 업데이트, 없으면 초기 데이터 유지
        if (Array.isArray(meetingsData) && meetingsData.length > 0) {
          setMeetings(meetingsData);
        }
        if (Array.isArray(issuesData) && issuesData.length > 0) {
          setIssues(issuesData);
        }
      } catch (error) {
        console.warn('Failed to load common management data, using initial data:', error);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [currentProject?.id]);

  // 회의 저장
  const handleSaveMeeting = async (meetingData: Partial<Meeting>) => {
    if (!currentProject?.id) return;

    try {
      if (editingMeeting) {
        const updated = await apiService.updateMeeting(currentProject.id, editingMeeting.id, meetingData);
        setMeetings(prev => prev.map(m => m.id === editingMeeting.id ? { ...m, ...updated } : m));
      } else {
        const created = await apiService.createMeeting(currentProject.id, meetingData);
        setMeetings(prev => [created, ...prev]);
      }
      setShowMeetingModal(false);
      setEditingMeeting(null);
    } catch (error) {
      console.warn('Failed to save meeting:', error);
    }
  };

  // 회의 삭제
  const handleDeleteMeeting = async (meetingId: string) => {
    if (!currentProject?.id || !confirm('이 회의를 삭제하시겠습니까?')) return;

    try {
      await apiService.deleteMeeting(currentProject.id, meetingId);
      setMeetings(prev => prev.filter(m => m.id !== meetingId));
    } catch (error) {
      console.warn('Failed to delete meeting:', error);
    }
  };

  // 이슈 저장
  const handleSaveIssue = async (issueData: Partial<Issue>) => {
    if (!currentProject?.id) return;

    try {
      if (editingIssue) {
        const updated = await apiService.updateIssue(currentProject.id, editingIssue.id, issueData);
        setIssues(prev => prev.map(i => i.id === editingIssue.id ? { ...i, ...updated } : i));
      } else {
        const created = await apiService.createIssue(currentProject.id, issueData);
        setIssues(prev => [created, ...prev]);
      }
      setShowIssueModal(false);
      setEditingIssue(null);
    } catch (error) {
      console.warn('Failed to save issue:', error);
    }
  };

  // 이슈 삭제
  const handleDeleteIssue = async (issueId: string) => {
    if (!currentProject?.id || !confirm('이 이슈를 삭제하시겠습니까?')) return;

    try {
      await apiService.deleteIssue(currentProject.id, issueId);
      setIssues(prev => prev.filter(i => i.id !== issueId));
    } catch (error) {
      console.warn('Failed to delete issue:', error);
    }
  };

  // 이슈 상태 변경
  const handleIssueStatusChange = async (issueId: string, newStatus: Issue['status']) => {
    if (!currentProject?.id) return;

    try {
      await apiService.updateIssueStatus(currentProject.id, issueId, newStatus);
      setIssues(prev => prev.map(i => i.id === issueId ? { ...i, status: newStatus } : i));
    } catch (error) {
      console.warn('Failed to update issue status:', error);
    }
  };

  // 필터링된 데이터
  const filteredMeetings = meetings.filter(m => {
    if (meetingFilter && m.status !== meetingFilter) return false;
    if (searchQuery && !m.title.toLowerCase().includes(searchQuery.toLowerCase())) return false;
    return true;
  });

  const filteredIssues = issues.filter(i => {
    if (issueFilter && i.status !== issueFilter) return false;
    if (searchQuery && !i.title.toLowerCase().includes(searchQuery.toLowerCase())) return false;
    return true;
  });

  return (
    <div className="p-6">
      <div className="mb-6">
        <h2 className="text-2xl font-semibold text-gray-900">공통 관리</h2>
        <p className="text-sm text-gray-500 mt-1">산출물, 회의, 이슈를 통합 관리합니다</p>
      </div>


      {/* 탭 네비게이션 */}
      <div className="flex border-b border-gray-200 mb-6">
        <button
          onClick={() => setActiveTab('deliverables')}
          className={`flex items-center gap-2 px-4 py-3 border-b-2 font-medium text-sm transition-colors ${
            activeTab === 'deliverables'
              ? 'border-blue-500 text-blue-600'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <FileText size={18} />
          산출물
        </button>
        <button
          onClick={() => setActiveTab('meetings')}
          className={`flex items-center gap-2 px-4 py-3 border-b-2 font-medium text-sm transition-colors ${
            activeTab === 'meetings'
              ? 'border-blue-500 text-blue-600'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <Calendar size={18} />
          회의
          {meetings.filter(m => m.status === 'SCHEDULED').length > 0 && (
            <span className="ml-1 px-2 py-0.5 text-xs bg-blue-100 text-blue-600 rounded-full">
              {meetings.filter(m => m.status === 'SCHEDULED').length}
            </span>
          )}
        </button>
        <button
          onClick={() => setActiveTab('issues')}
          className={`flex items-center gap-2 px-4 py-3 border-b-2 font-medium text-sm transition-colors ${
            activeTab === 'issues'
              ? 'border-blue-500 text-blue-600'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <AlertTriangle size={18} />
          이슈
          {issues.filter(i => !['CLOSED', 'RESOLVED', 'VERIFIED'].includes(i.status)).length > 0 && (
            <span className="ml-1 px-2 py-0.5 text-xs bg-red-100 text-red-600 rounded-full">
              {issues.filter(i => !['CLOSED', 'RESOLVED', 'VERIFIED'].includes(i.status)).length}
            </span>
          )}
        </button>
      </div>

      {/* 검색 및 필터 바 */}
      <div className="flex items-center gap-4 mb-6">
        <div className="flex-1 relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {activeTab === 'meetings' && (
          <select
            value={meetingFilter}
            onChange={(e) => setMeetingFilter(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm"
          >
            <option value="">전체 상태</option>
            <option value="SCHEDULED">예정</option>
            <option value="IN_PROGRESS">진행중</option>
            <option value="COMPLETED">완료</option>
            <option value="CANCELLED">취소</option>
          </select>
        )}

        {activeTab === 'issues' && (
          <select
            value={issueFilter}
            onChange={(e) => setIssueFilter(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm"
          >
            <option value="">전체 상태</option>
            <option value="OPEN">신규</option>
            <option value="IN_PROGRESS">진행중</option>
            <option value="RESOLVED">해결</option>
            <option value="CLOSED">종료</option>
          </select>
        )}

        {activeTab === 'meetings' && canManageMeetings && (
          <button
            onClick={() => {
              setEditingMeeting(null);
              setShowMeetingModal(true);
            }}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus size={18} />
            회의 등록
          </button>
        )}

        {activeTab === 'issues' && canManageIssues && (
          <button
            onClick={() => {
              setEditingIssue(null);
              setShowIssueModal(true);
            }}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus size={18} />
            이슈 등록
          </button>
        )}
      </div>

      {/* 탭 컨텐츠 */}
      {loading ? (
        <div className="text-center py-12 text-gray-500">데이터를 불러오는 중...</div>
      ) : (
        <>
          {/* 산출물 탭 */}
          {activeTab === 'deliverables' && (
            <div className="space-y-4">
              {deliverables.length === 0 ? (
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-12 text-center text-gray-400">
                  <FileText size={48} className="mx-auto mb-4 opacity-50" />
                  <p>등록된 산출물이 없습니다.</p>
                </div>
              ) : (
                <div className="bg-white rounded-xl shadow-sm border border-gray-200">
                  <div className="p-4 border-b border-gray-200 flex items-center justify-between">
                    <h3 className="font-medium text-gray-900">전체 산출물 목록</h3>
                    <span className="text-sm text-gray-500">{deliverables.length}개</span>
                  </div>
                  <div className="divide-y divide-gray-200">
                    {deliverables.map((deliverable) => (
                      <div
                        key={deliverable.id}
                        className="p-4 hover:bg-gray-50 transition-colors"
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex-1">
                            <div className="flex items-center gap-3 mb-1">
                              <FileText size={18} className="text-gray-400" />
                              <h4 className="font-medium text-gray-900">{deliverable.name}</h4>
                              <span
                                className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                                  deliverable.status === 'APPROVED'
                                    ? 'bg-green-100 text-green-700'
                                    : deliverable.status === 'IN_REVIEW'
                                    ? 'bg-blue-100 text-blue-700'
                                    : deliverable.status === 'DRAFT'
                                    ? 'bg-amber-100 text-amber-700'
                                    : deliverable.status === 'REJECTED'
                                    ? 'bg-red-100 text-red-700'
                                    : 'bg-gray-100 text-gray-700'
                                }`}
                              >
                                {deliverable.status === 'APPROVED'
                                  ? '승인됨'
                                  : deliverable.status === 'IN_REVIEW'
                                  ? '검토중'
                                  : deliverable.status === 'DRAFT'
                                  ? '작성중'
                                  : deliverable.status === 'REJECTED'
                                  ? '반려됨'
                                  : '대기'}
                              </span>
                              {deliverable.version && (
                                <span className="text-xs text-gray-500">v{deliverable.version}</span>
                              )}
                            </div>
                            {deliverable.description && (
                              <p className="text-sm text-gray-500 ml-7 mb-1">{deliverable.description}</p>
                            )}
                            <div className="flex items-center gap-4 text-xs text-gray-400 ml-7">
                              {deliverable.uploadedAt && (
                                <span className="flex items-center gap-1">
                                  <Clock size={12} />
                                  업로드: {deliverable.uploadedAt}
                                </span>
                              )}
                              {deliverable.approvedBy && (
                                <span>승인자: {deliverable.approvedBy}</span>
                              )}
                              {deliverable.type && (
                                <span className="px-1.5 py-0.5 bg-gray-100 rounded text-gray-600">
                                  {deliverable.type}
                                </span>
                              )}
                            </div>
                          </div>
                          <div className="flex items-center gap-2">
                            {canEdit && deliverable.status !== 'APPROVED' && (
                              <button
                                className="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
                                title="수정"
                              >
                                <Pencil size={16} />
                              </button>
                            )}
                            {canEdit && (
                              <button
                                className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                                title="삭제"
                              >
                                <Trash2 size={16} />
                              </button>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* 회의 탭 */}
          {activeTab === 'meetings' && (
            <div className="space-y-4">
              {filteredMeetings.length === 0 ? (
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-12 text-center text-gray-400">
                  <Calendar size={48} className="mx-auto mb-4 opacity-50" />
                  <p>등록된 회의가 없습니다.</p>
                </div>
              ) : (
                filteredMeetings.map((meeting) => (
                  <div
                    key={meeting.id}
                    className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <span className={`px-2 py-1 rounded text-xs font-medium ${getStatusColor(meeting.status)}`}>
                            {meeting.status === 'SCHEDULED' ? '예정' : meeting.status === 'COMPLETED' ? '완료' : meeting.status === 'IN_PROGRESS' ? '진행중' : meeting.status}
                          </span>
                          <span className="px-2 py-1 bg-gray-100 text-gray-600 rounded text-xs">
                            {getMeetingTypeLabel(meeting.meetingType)}
                          </span>
                        </div>
                        <h3 className="text-lg font-medium text-gray-900">{meeting.title}</h3>
                        {meeting.description && (
                          <p className="text-sm text-gray-500 mt-1">{meeting.description}</p>
                        )}
                        <div className="flex items-center gap-4 mt-3 text-sm text-gray-600">
                          <span className="flex items-center gap-1">
                            <Clock size={14} />
                            {formatDateTime(meeting.scheduledAt)}
                          </span>
                          {meeting.location && (
                            <span className="flex items-center gap-1">
                              <MapPin size={14} />
                              {meeting.location}
                            </span>
                          )}
                          {meeting.attendees && meeting.attendees.length > 0 && (
                            <span className="flex items-center gap-1">
                              <Users size={14} />
                              {meeting.attendees.length}명
                            </span>
                          )}
                        </div>
                      </div>
                      {canManageMeetings && (
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => {
                              setEditingMeeting(meeting);
                              setShowMeetingModal(true);
                            }}
                            className="p-2 text-gray-500 hover:bg-gray-100 rounded transition-colors"
                          >
                            <Pencil size={16} />
                          </button>
                          <button
                            onClick={() => handleDeleteMeeting(meeting.id)}
                            className="p-2 text-red-500 hover:bg-red-50 rounded transition-colors"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {/* 이슈 탭 */}
          {activeTab === 'issues' && (
            <div className="space-y-4">
              {filteredIssues.length === 0 ? (
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-12 text-center text-gray-400">
                  <AlertTriangle size={48} className="mx-auto mb-4 opacity-50" />
                  <p>등록된 이슈가 없습니다.</p>
                </div>
              ) : (
                filteredIssues.map((issue) => (
                  <div
                    key={issue.id}
                    className="bg-white rounded-xl shadow-sm border border-gray-200"
                  >
                    <div
                      className="p-4 cursor-pointer"
                      onClick={() => setExpandedIssue(expandedIssue === issue.id ? null : issue.id)}
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-3 mb-2">
                            <span className={`px-2 py-1 rounded text-xs font-medium border ${getPriorityColor(issue.priority)}`}>
                              {getPriorityLabel(issue.priority)}
                            </span>
                            <span className={`px-2 py-1 rounded text-xs font-medium ${getStatusColor(issue.status)}`}>
                              {issue.status === 'OPEN' ? '신규' : issue.status === 'IN_PROGRESS' ? '진행중' : issue.status === 'RESOLVED' ? '해결' : issue.status === 'CLOSED' ? '종료' : issue.status}
                            </span>
                            <span className="px-2 py-1 bg-gray-100 text-gray-600 rounded text-xs">
                              {getIssueTypeLabel(issue.issueType)}
                            </span>
                          </div>
                          <h3 className="text-lg font-medium text-gray-900">{issue.title}</h3>
                          <div className="flex items-center gap-4 mt-2 text-sm text-gray-500">
                            {issue.assignee && <span>담당: {issue.assignee}</span>}
                            {issue.dueDate && <span>마감: {formatDate(issue.dueDate)}</span>}
                            <span>등록: {formatDate(issue.createdAt)}</span>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          {canManageIssues && (
                            <>
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setEditingIssue(issue);
                                  setShowIssueModal(true);
                                }}
                                className="p-2 text-gray-500 hover:bg-gray-100 rounded transition-colors"
                              >
                                <Pencil size={16} />
                              </button>
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleDeleteIssue(issue.id);
                                }}
                                className="p-2 text-red-500 hover:bg-red-50 rounded transition-colors"
                              >
                                <Trash2 size={16} />
                              </button>
                            </>
                          )}
                          {expandedIssue === issue.id ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                        </div>
                      </div>
                    </div>

                    {/* 확장된 이슈 상세 */}
                    {expandedIssue === issue.id && (
                      <div className="border-t border-gray-200 p-4 bg-gray-50">
                        {issue.description && (
                          <div className="mb-4">
                            <h4 className="text-sm font-medium text-gray-700 mb-1">설명</h4>
                            <p className="text-sm text-gray-600">{issue.description}</p>
                          </div>
                        )}

                        {issue.resolution && (
                          <div className="mb-4">
                            <h4 className="text-sm font-medium text-gray-700 mb-1">해결 내용</h4>
                            <p className="text-sm text-gray-600">{issue.resolution}</p>
                          </div>
                        )}

                        {/* 상태 변경 버튼 */}
                        {canManageIssues && issue.status !== 'CLOSED' && (
                          <div className="flex items-center gap-2 mb-4">
                            <span className="text-sm text-gray-500">상태 변경:</span>
                            {issue.status === 'OPEN' && (
                              <button
                                onClick={() => handleIssueStatusChange(issue.id, 'IN_PROGRESS')}
                                className="px-3 py-1 text-xs bg-yellow-100 text-yellow-700 rounded hover:bg-yellow-200"
                              >
                                진행 시작
                              </button>
                            )}
                            {issue.status === 'IN_PROGRESS' && (
                              <button
                                onClick={() => handleIssueStatusChange(issue.id, 'RESOLVED')}
                                className="px-3 py-1 text-xs bg-green-100 text-green-700 rounded hover:bg-green-200"
                              >
                                해결 완료
                              </button>
                            )}
                            {issue.status === 'RESOLVED' && (
                              <>
                                <button
                                  onClick={() => handleIssueStatusChange(issue.id, 'VERIFIED')}
                                  className="px-3 py-1 text-xs bg-emerald-100 text-emerald-700 rounded hover:bg-emerald-200"
                                >
                                  검증 완료
                                </button>
                                <button
                                  onClick={() => handleIssueStatusChange(issue.id, 'REOPENED')}
                                  className="px-3 py-1 text-xs bg-purple-100 text-purple-700 rounded hover:bg-purple-200"
                                >
                                  재오픈
                                </button>
                              </>
                            )}
                            {['VERIFIED', 'RESOLVED'].includes(issue.status) && (
                              <button
                                onClick={() => handleIssueStatusChange(issue.id, 'CLOSED')}
                                className="px-3 py-1 text-xs bg-gray-100 text-gray-700 rounded hover:bg-gray-200"
                              >
                                종료
                              </button>
                            )}
                          </div>
                        )}

                        {/* 댓글 */}
                        {issue.comments && issue.comments.length > 0 && (
                          <div>
                            <h4 className="text-sm font-medium text-gray-700 mb-2 flex items-center gap-1">
                              <MessageSquare size={14} />
                              댓글 ({issue.comments.length})
                            </h4>
                            <div className="space-y-2">
                              {issue.comments.map((comment, idx) => (
                                <div key={idx} className="bg-white p-3 rounded border border-gray-200">
                                  <div className="flex items-center justify-between mb-1">
                                    <span className="text-sm font-medium text-gray-700">{comment.author}</span>
                                    <span className="text-xs text-gray-400">{formatDateTime(comment.commentedAt)}</span>
                                  </div>
                                  <p className="text-sm text-gray-600">{comment.content}</p>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          )}
        </>
      )}

      {/* 회의 모달 */}
      {showMeetingModal && (
        <MeetingModal
          meeting={editingMeeting}
          onSave={handleSaveMeeting}
          onClose={() => {
            setShowMeetingModal(false);
            setEditingMeeting(null);
          }}
        />
      )}

      {/* 이슈 모달 */}
      {showIssueModal && (
        <IssueModal
          issue={editingIssue}
          onSave={handleSaveIssue}
          onClose={() => {
            setShowIssueModal(false);
            setEditingIssue(null);
          }}
        />
      )}
    </div>
  );
}

// ========================================
// 회의 모달 컴포넌트
// ========================================

function MeetingModal({
  meeting,
  onSave,
  onClose,
}: {
  meeting: Meeting | null;
  onSave: (data: Partial<Meeting>) => void;
  onClose: () => void;
}) {
  const [formData, setFormData] = useState({
    title: meeting?.title || '',
    description: meeting?.description || '',
    meetingType: meeting?.meetingType || 'WEEKLY',
    scheduledAt: meeting?.scheduledAt ? meeting.scheduledAt.slice(0, 16) : '',
    location: meeting?.location || '',
    organizer: meeting?.organizer || '',
    attendees: meeting?.attendees?.join(', ') || '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave({
      ...formData,
      scheduledAt: formData.scheduledAt ? new Date(formData.scheduledAt).toISOString() : undefined,
      attendees: formData.attendees.split(',').map(s => s.trim()).filter(Boolean),
    });
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900">
            {meeting ? '회의 수정' : '회의 등록'}
          </h3>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">제목 *</label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">회의 유형 *</label>
            <select
              value={formData.meetingType}
              onChange={(e) => setFormData(prev => ({ ...prev, meetingType: e.target.value as Meeting['meetingType'] }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
            >
              <option value="KICKOFF">착수 보고</option>
              <option value="WEEKLY">주간 보고</option>
              <option value="MONTHLY">월간 보고</option>
              <option value="MILESTONE">마일스톤</option>
              <option value="CLOSING">종료 보고</option>
              <option value="TECHNICAL">기술 회의</option>
              <option value="STAKEHOLDER">이해관계자</option>
              <option value="OTHER">기타</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">일시 *</label>
            <input
              type="datetime-local"
              value={formData.scheduledAt}
              onChange={(e) => setFormData(prev => ({ ...prev, scheduledAt: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">장소</label>
            <input
              type="text"
              value={formData.location}
              onChange={(e) => setFormData(prev => ({ ...prev, location: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              placeholder="예: 회의실 A, 온라인(Zoom)"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">주최자</label>
            <input
              type="text"
              value={formData.organizer}
              onChange={(e) => setFormData(prev => ({ ...prev, organizer: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">참석자 (쉼표로 구분)</label>
            <input
              type="text"
              value={formData.attendees}
              onChange={(e) => setFormData(prev => ({ ...prev, attendees: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              placeholder="예: 홍길동, 김철수, 이영희"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">설명</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              rows={3}
            />
          </div>
          <div className="flex gap-3 pt-4">
            <button
              type="submit"
              className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              저장
            </button>
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
            >
              취소
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ========================================
// 이슈 모달 컴포넌트
// ========================================

function IssueModal({
  issue,
  onSave,
  onClose,
}: {
  issue: Issue | null;
  onSave: (data: Partial<Issue>) => void;
  onClose: () => void;
}) {
  const [formData, setFormData] = useState({
    title: issue?.title || '',
    description: issue?.description || '',
    issueType: issue?.issueType || 'BUG',
    priority: issue?.priority || 'MEDIUM',
    assignee: issue?.assignee || '',
    reporter: issue?.reporter || '',
    reviewer: issue?.reviewer || '',
    dueDate: issue?.dueDate || '',
    resolution: issue?.resolution || '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave(formData);
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900">
            {issue ? '이슈 수정' : '이슈 등록'}
          </h3>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">제목 *</label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">유형 *</label>
              <select
                value={formData.issueType}
                onChange={(e) => setFormData(prev => ({ ...prev, issueType: e.target.value as Issue['issueType'] }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              >
                <option value="BUG">버그</option>
                <option value="RISK">위험</option>
                <option value="BLOCKER">장애</option>
                <option value="CHANGE_REQUEST">변경 요청</option>
                <option value="QUESTION">문의</option>
                <option value="IMPROVEMENT">개선</option>
                <option value="OTHER">기타</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">우선순위 *</label>
              <select
                value={formData.priority}
                onChange={(e) => setFormData(prev => ({ ...prev, priority: e.target.value as Issue['priority'] }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              >
                <option value="CRITICAL">긴급</option>
                <option value="HIGH">높음</option>
                <option value="MEDIUM">중간</option>
                <option value="LOW">낮음</option>
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">담당자</label>
              <input
                type="text"
                value={formData.assignee}
                onChange={(e) => setFormData(prev => ({ ...prev, assignee: e.target.value }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">보고자</label>
              <input
                type="text"
                value={formData.reporter}
                onChange={(e) => setFormData(prev => ({ ...prev, reporter: e.target.value }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">검토자</label>
              <input
                type="text"
                value={formData.reviewer}
                onChange={(e) => setFormData(prev => ({ ...prev, reviewer: e.target.value }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">마감일</label>
              <input
                type="date"
                value={formData.dueDate}
                onChange={(e) => setFormData(prev => ({ ...prev, dueDate: e.target.value }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">설명</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
              rows={3}
            />
          </div>
          {issue && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">해결 내용</label>
              <textarea
                value={formData.resolution}
                onChange={(e) => setFormData(prev => ({ ...prev, resolution: e.target.value }))}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                rows={3}
                placeholder="이슈 해결 시 해결 내용을 입력하세요"
              />
            </div>
          )}
          <div className="flex gap-3 pt-4">
            <button
              type="submit"
              className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              저장
            </button>
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
            >
              취소
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
