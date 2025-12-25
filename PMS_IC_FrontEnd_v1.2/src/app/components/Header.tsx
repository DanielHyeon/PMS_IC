import { UserInfo } from '../App';
import { Bell, Bot, User, LogOut, Shield } from 'lucide-react';
import { useState } from 'react';

interface HeaderProps {
  currentUser: UserInfo;
  onAIToggle: () => void;
  onLogout: () => void;
  canUseAI: boolean;
}

const roleNames: Record<string, string> = {
  sponsor: '프로젝트 스폰서',
  pmo_head: 'PMO 총괄',
  pm: '프로젝트 관리자',
  developer: '개발팀',
  qa: 'QA팀',
  business_analyst: '현업 분석가',
  auditor: '외부 감리',
  admin: '시스템 관리자',
};

export default function Header({ currentUser, onAIToggle, onLogout, canUseAI }: HeaderProps) {
  const [showUserMenu, setShowUserMenu] = useState(false);

  return (
    <header className="bg-white border-b border-gray-200 px-6 py-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">AI 기반 손해보험 지급심사 자동화 프로젝트</h2>
          <p className="text-sm text-gray-500 mt-1">Project Code: INS-AI-2025-001</p>
        </div>

        <div className="flex items-center gap-4">
          {canUseAI && (
            <button
              onClick={onAIToggle}
              className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-lg hover:from-purple-700 hover:to-blue-700 transition-all shadow-md"
            >
              <Bot size={20} />
              <span>AI 어시스턴트</span>
            </button>
          )}

          <button className="relative p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors">
            <Bell size={20} />
            <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
          </button>

          <div className="relative">
            <button
              onClick={() => setShowUserMenu(!showUserMenu)}
              className="flex items-center gap-3 pl-4 border-l border-gray-200 hover:bg-gray-50 rounded-lg p-2 transition-colors"
            >
              <div className="text-right">
                <p className="text-sm font-medium text-gray-900">{currentUser.name}</p>
                <p className="text-xs text-gray-500">{roleNames[currentUser.role]}</p>
              </div>
              <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center text-white">
                <User size={20} />
              </div>
            </button>

            {showUserMenu && (
              <div className="absolute right-0 mt-2 w-64 bg-white rounded-lg shadow-xl border border-gray-200 z-50">
                <div className="p-4 border-b border-gray-200">
                  <p className="font-medium text-gray-900">{currentUser.name}</p>
                  <p className="text-sm text-gray-500">{currentUser.email}</p>
                  <p className="text-xs text-gray-400 mt-1">{currentUser.department}</p>
                  <div className="mt-2 inline-flex items-center gap-1 px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs">
                    <Shield size={12} />
                    <span>{roleNames[currentUser.role]}</span>
                  </div>
                </div>
                <div className="p-2">
                  <button
                    onClick={onLogout}
                    className="w-full flex items-center gap-2 px-3 py-2 text-red-600 hover:bg-red-50 rounded transition-colors"
                  >
                    <LogOut size={16} />
                    <span>로그아웃</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}