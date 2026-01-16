import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { TrendingUp, AlertTriangle, CheckCircle2, Clock, Target, DollarSign, Lock, Cpu, Cog, Layers, User } from 'lucide-react';
import { UserRole } from '../App';
import { trackProgressData, subProjectData, partLeaderData, phaseData, sprintVelocity, burndownData } from '../../mocks';
import { getStatusColor, getStatusLabel, getTrackColor, getActivityColor } from '../../utils/status';

export default function Dashboard({ userRole }: { userRole: UserRole }) {
  // 역할별 접근 권한
  const canViewBudget = ['sponsor', 'pmo_head', 'pm'].includes(userRole);
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

      {/* AI/SI/공통 투트랙 진척률 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className={`bg-white rounded-xl shadow-sm border p-6 ${getStatusColor(trackProgressData.ai.status).border}`}>
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <Cpu className="text-blue-600" size={20} />
            </div>
            <div>
              <h4 className="font-semibold text-gray-900">AI 트랙</h4>
              <span className={`text-xs px-2 py-0.5 rounded-full ${getStatusColor(trackProgressData.ai.status).bg} ${getStatusColor(trackProgressData.ai.status).text}`}>
                {getStatusLabel(trackProgressData.ai.status)}
              </span>
            </div>
          </div>
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">진척률</span>
              <span className="font-semibold">{trackProgressData.ai.progress}%</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-3">
              <div className="bg-blue-500 h-3 rounded-full transition-all" style={{ width: `${trackProgressData.ai.progress}%` }}></div>
            </div>
            <p className="text-xs text-gray-500 mt-2">완료: {trackProgressData.ai.completed}/{trackProgressData.ai.tasks} 작업</p>
          </div>
        </div>

        <div className={`bg-white rounded-xl shadow-sm border p-6 ${getStatusColor(trackProgressData.si.status).border}`}>
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
              <Cog className="text-green-600" size={20} />
            </div>
            <div>
              <h4 className="font-semibold text-gray-900">SI 트랙</h4>
              <span className={`text-xs px-2 py-0.5 rounded-full ${getStatusColor(trackProgressData.si.status).bg} ${getStatusColor(trackProgressData.si.status).text}`}>
                {getStatusLabel(trackProgressData.si.status)}
              </span>
            </div>
          </div>
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">진척률</span>
              <span className="font-semibold">{trackProgressData.si.progress}%</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-3">
              <div className="bg-green-500 h-3 rounded-full transition-all" style={{ width: `${trackProgressData.si.progress}%` }}></div>
            </div>
            <p className="text-xs text-gray-500 mt-2">완료: {trackProgressData.si.completed}/{trackProgressData.si.tasks} 작업</p>
          </div>
        </div>

        <div className={`bg-white rounded-xl shadow-sm border p-6 ${getStatusColor(trackProgressData.common.status).border}`}>
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
              <Layers className="text-purple-600" size={20} />
            </div>
            <div>
              <h4 className="font-semibold text-gray-900">공통 트랙</h4>
              <span className={`text-xs px-2 py-0.5 rounded-full ${getStatusColor(trackProgressData.common.status).bg} ${getStatusColor(trackProgressData.common.status).text}`}>
                {getStatusLabel(trackProgressData.common.status)}
              </span>
            </div>
          </div>
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-gray-600">진척률</span>
              <span className="font-semibold">{trackProgressData.common.progress}%</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-3">
              <div className="bg-purple-500 h-3 rounded-full transition-all" style={{ width: `${trackProgressData.common.progress}%` }}></div>
            </div>
            <p className="text-xs text-gray-500 mt-2">완료: {trackProgressData.common.completed}/{trackProgressData.common.tasks} 작업</p>
          </div>
        </div>
      </div>

      {/* 서브 프로젝트별 상태 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h3 className="font-semibold text-gray-900 mb-4">서브 프로젝트별 상태</h3>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200">
                <th className="text-left py-3 px-4 font-medium text-gray-600">프로젝트명</th>
                <th className="text-left py-3 px-4 font-medium text-gray-600">트랙</th>
                <th className="text-left py-3 px-4 font-medium text-gray-600">담당자</th>
                <th className="text-left py-3 px-4 font-medium text-gray-600">진척률</th>
                <th className="text-left py-3 px-4 font-medium text-gray-600">상태</th>
              </tr>
            </thead>
            <tbody>
              {subProjectData.map((project, idx) => (
                <tr key={idx} className="border-b border-gray-100 hover:bg-gray-50">
                  <td className="py-3 px-4 font-medium text-gray-900">{project.name}</td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${getTrackColor(project.track)}`}>{project.track}</span>
                  </td>
                  <td className="py-3 px-4 text-gray-600">{project.leader}</td>
                  <td className="py-3 px-4">
                    <div className="flex items-center gap-2">
                      <div className="w-20 bg-gray-200 rounded-full h-2">
                        <div className={`h-2 rounded-full ${
                          project.status === 'normal' ? 'bg-green-500' :
                          project.status === 'warning' ? 'bg-yellow-500' : 'bg-red-500'
                        }`} style={{ width: `${project.progress}%` }}></div>
                      </div>
                      <span className="text-gray-700">{project.progress}%</span>
                    </div>
                  </td>
                  <td className="py-3 px-4">
                    <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(project.status).bg} ${getStatusColor(project.status).text}`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${getStatusColor(project.status).dot}`}></span>
                      {getStatusLabel(project.status)}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* 파트 리더별 현황 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h3 className="font-semibold text-gray-900 mb-4">파트 리더별 현황</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {partLeaderData.map((leader, idx) => (
            <div key={idx} className={`border rounded-lg p-4 ${getStatusColor(leader.status).border}`}>
              <div className="flex items-center gap-3 mb-3">
                <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center">
                  <User className="text-gray-600" size={20} />
                </div>
                <div>
                  <p className="font-medium text-gray-900">{leader.name}</p>
                  <p className="text-xs text-gray-500">{leader.role}</p>
                </div>
                <span className={`ml-auto px-2 py-0.5 rounded-full text-xs font-medium ${getStatusColor(leader.status).bg} ${getStatusColor(leader.status).text}`}>
                  {getStatusLabel(leader.status)}
                </span>
              </div>
              <div className="grid grid-cols-3 gap-2 text-center">
                <div className="bg-green-50 rounded p-2">
                  <p className="text-lg font-semibold text-green-700">{leader.completed}</p>
                  <p className="text-xs text-green-600">완료</p>
                </div>
                <div className="bg-blue-50 rounded p-2">
                  <p className="text-lg font-semibold text-blue-700">{leader.inProgress}</p>
                  <p className="text-xs text-blue-600">진행중</p>
                </div>
                <div className="bg-red-50 rounded p-2">
                  <p className="text-lg font-semibold text-red-700">{leader.blocked}</p>
                  <p className="text-xs text-red-600">블로커</p>
                </div>
              </div>
            </div>
          ))}
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
              <div className={`w-2 h-2 rounded-full ${getActivityColor(activity.type)}`}></div>
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