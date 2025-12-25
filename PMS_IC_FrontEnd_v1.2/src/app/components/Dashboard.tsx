import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { TrendingUp, AlertTriangle, CheckCircle2, Clock, Target, DollarSign, Lock } from 'lucide-react';
import { UserRole } from '../App';

const phaseData = [
  { phase: '1단계', planned: 100, actual: 100, status: 'completed' },
  { phase: '2단계', planned: 100, actual: 100, status: 'completed' },
  { phase: '3단계', planned: 100, actual: 85, status: 'inProgress' },
  { phase: '4단계', planned: 100, actual: 0, status: 'pending' },
  { phase: '5단계', planned: 100, actual: 0, status: 'pending' },
  { phase: '6단계', planned: 100, actual: 0, status: 'pending' },
];

const sprintVelocity = [
  { sprint: 'Sprint 1', velocity: 32, planned: 35 },
  { sprint: 'Sprint 2', velocity: 38, planned: 35 },
  { sprint: 'Sprint 3', velocity: 35, planned: 35 },
  { sprint: 'Sprint 4', velocity: 42, planned: 40 },
  { sprint: 'Sprint 5', velocity: 40, planned: 40 },
];

const burndownData = [
  { day: 'Day 1', remaining: 120, ideal: 120 },
  { day: 'Day 2', remaining: 110, ideal: 105 },
  { day: 'Day 3', remaining: 95, ideal: 90 },
  { day: 'Day 4', remaining: 85, ideal: 75 },
  { day: 'Day 5', remaining: 70, ideal: 60 },
  { day: 'Day 6', remaining: 55, ideal: 45 },
  { day: 'Day 7', remaining: 42, ideal: 30 },
  { day: 'Day 8', remaining: 28, ideal: 15 },
  { day: 'Day 9', remaining: 15, ideal: 0 },
  { day: 'Day 10', remaining: 8, ideal: 0 },
];

export default function Dashboard({ userRole }: { userRole: UserRole }) {
  // 역할별 접근 권한
  const canViewBudget = ['sponsor', 'pmo_head', 'pm'].includes(userRole);
  const canViewDetailedMetrics = !['auditor'].includes(userRole);
  const isReadOnly = ['auditor', 'business_analyst'].includes(userRole);

  return (
    <div className="p-6 space-y-6">
      {/* Role Banner */}
      {isReadOnly && (
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 flex items-center gap-3">
          <Lock className="text-amber-600" size={20} />
          <div>
            <p className="text-sm font-medium text-amber-900">읽기 전용 모드</p>
            <p className="text-xs text-amber-700">현재 역할은 조회 권한만 가지고 있습니다.</p>
          </div>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">전체 진행률</p>
              <p className="text-3xl font-semibold text-gray-900 mt-2">62%</p>
              <p className="text-xs text-green-600 mt-1 flex items-center gap-1">
                <TrendingUp size={14} />
                <span>On Track</span>
              </p>
            </div>
            <div className="w-14 h-14 bg-blue-100 rounded-full flex items-center justify-center">
              <Target className="text-blue-600" size={28} />
            </div>
          </div>
        </div>

        {canViewBudget ? (
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">예산 집행률</p>
                <p className="text-3xl font-semibold text-gray-900 mt-2">58%</p>
                <p className="text-xs text-gray-600 mt-1">₩580M / ₩1,000M</p>
              </div>
              <div className="w-14 h-14 bg-green-100 rounded-full flex items-center justify-center">
                <DollarSign className="text-green-600" size={28} />
              </div>
            </div>
          </div>
        ) : (
          <div className="bg-gray-100 rounded-xl shadow-sm border border-gray-200 p-6 relative">
            <div className="absolute inset-0 flex items-center justify-center bg-gray-100/90 rounded-xl backdrop-blur-sm">
              <div className="text-center">
                <Lock className="text-gray-400 mx-auto mb-2" size={24} />
                <p className="text-xs text-gray-500">접근 권한 없음</p>
              </div>
            </div>
          </div>
        )}

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">활성 이슈</p>
              <p className="text-3xl font-semibold text-gray-900 mt-2">7</p>
              <p className="text-xs text-amber-600 mt-1 flex items-center gap-1">
                <AlertTriangle size={14} />
                <span>3 High Priority</span>
              </p>
            </div>
            <div className="w-14 h-14 bg-amber-100 rounded-full flex items-center justify-center">
              <AlertTriangle className="text-amber-600" size={28} />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">완료 작업</p>
              <p className="text-3xl font-semibold text-gray-900 mt-2">142</p>
              <p className="text-xs text-gray-600 mt-1">총 230개 중</p>
            </div>
            <div className="w-14 h-14 bg-purple-100 rounded-full flex items-center justify-center">
              <CheckCircle2 className="text-purple-600" size={28} />
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Phase Progress - Waterfall View */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="font-semibold text-gray-900 mb-4">단계별 진행 현황 (Waterfall View)</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={phaseData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="phase" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="planned" fill="#93c5fd" name="계획" />
              <Bar dataKey="actual" fill="#3b82f6" name="실적" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Sprint Velocity */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h3 className="font-semibold text-gray-900 mb-4">스프린트 속도 (Sprint Velocity)</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={sprintVelocity}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="sprint" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="planned" fill="#d1d5db" name="계획 Story Points" />
              <Bar dataKey="velocity" fill="#8b5cf6" name="실제 Velocity" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Burndown Chart */}
        <div className="lg:col-span-2 bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-semibold text-gray-900">소멸 차트 (Current Sprint Burndown)</h3>
            <span className="text-sm text-gray-500">Sprint 5 (Day 10 of 14)</span>
          </div>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={burndownData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="day" />
              <YAxis label={{ value: 'Story Points', angle: -90, position: 'insideLeft' }} />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="ideal" stroke="#d1d5db" strokeWidth={2} name="이상적 소멸" strokeDasharray="5 5" />
              <Line type="monotone" dataKey="remaining" stroke="#3b82f6" strokeWidth={2} name="실제 남은 작업" />
            </LineChart>
          </ResponsiveContainer>
          <div className="mt-4 p-3 bg-blue-50 rounded-lg border border-blue-200">
            <p className="text-sm text-blue-900">
              <Clock className="inline-block mr-2" size={16} />
              현재 진행률 93% - 스프린트 목표 달성 예상 확률: <span className="font-semibold">85%</span>
            </p>
          </div>
        </div>

        {/* AI Insights */}
        <div className="bg-gradient-to-br from-purple-50 to-blue-50 rounded-xl shadow-sm border border-purple-200 p-6">
          <h3 className="font-semibold text-gray-900 mb-4">AI 인사이트</h3>
          <div className="space-y-4">
            <div className="bg-white rounded-lg p-4 border border-purple-200">
              <div className="flex items-start gap-2">
                <AlertTriangle className="text-amber-500 mt-0.5" size={18} />
                <div>
                  <p className="text-sm font-medium text-gray-900">위험 감지</p>
                  <p className="text-xs text-gray-600 mt-1">
                    OCR 모델 정확도 목표 미달 가능성 75%. 
                    특정 병원 진단서 양식 인식률 저하 문제.
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-lg p-4 border border-green-200">
              <div className="flex items-start gap-2">
                <CheckCircle2 className="text-green-500 mt-0.5" size={18} />
                <div>
                  <p className="text-sm font-medium text-gray-900">주간 성과</p>
                  <p className="text-xs text-gray-600 mt-1">
                    데이터 전처리 완료율 95% 달성.
                    모델 학습 인프라 최적화 완료.
                  </p>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-lg p-4 border border-blue-200">
              <div className="flex items-start gap-2">
                <TrendingUp className="text-blue-500 mt-0.5" size={18} />
                <div>
                  <p className="text-sm font-medium text-gray-900">권장 사항</p>
                  <p className="text-xs text-gray-600 mt-1">
                    다음 스프린트: 데이터 증강(Data Augmentation) 우선 진행 권장.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Activities */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h3 className="font-semibold text-gray-900 mb-4">최근 활동</h3>
        <div className="space-y-3">
          {[
            { user: '박민수', action: 'OCR 모델 v2.1 성능 테스트 완료', time: '5분 전', type: 'success' },
            { user: '이영희', action: '데이터 비식별화 문서 승인 요청', time: '1시간 전', type: 'info' },
            { user: 'AI 어시스턴트', action: '일정 지연 위험 감지 알림 발송', time: '2시간 전', type: 'warning' },
            { user: '김철수', action: 'Sprint 5 회고 회의록 등록', time: '3시간 전', type: 'info' },
            { user: '최지훈', action: '긴급 이슈 #47 해결 완료', time: '5시간 전', type: 'success' },
          ].map((activity, idx) => (
            <div key={idx} className="flex items-center gap-4 py-3 border-b border-gray-100 last:border-0">
              <div className={`w-2 h-2 rounded-full ${
                activity.type === 'success' ? 'bg-green-500' :
                activity.type === 'warning' ? 'bg-amber-500' : 'bg-blue-500'
              }`}></div>
              <div className="flex-1">
                <p className="text-sm text-gray-900">
                  <span className="font-medium">{activity.user}</span> {activity.action}
                </p>
              </div>
              <span className="text-xs text-gray-500">{activity.time}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}