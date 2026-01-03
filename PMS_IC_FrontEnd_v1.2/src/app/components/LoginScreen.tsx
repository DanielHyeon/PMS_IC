import { useState } from 'react';
import { UserInfo, UserRole } from '../App';
import { Shield, Lock, User, AlertCircle, Eye, EyeOff, CheckCircle2 } from 'lucide-react';
import { apiService } from '../../services/api';

interface LoginScreenProps {
  onLogin: (userInfo: UserInfo) => void;
}

// 데모용 사용자 계정 (실제 환경에서는 백엔드 인증)
const demoUsers: Record<string, { password: string; userInfo: UserInfo }> = {
  'sponsor@insure.com': {
    password: 'sponsor123',
    userInfo: {
      id: 'U001',
      name: '이사장',
      role: 'sponsor',
      email: 'sponsor@insure.com',
      department: '경영진',
    },
  },
  'pmo@insure.com': {
    password: 'pmo123',
    userInfo: {
      id: 'U002',
      name: 'PMO 총괄',
      role: 'pmo_head',
      email: 'pmo@insure.com',
      department: 'PMO',
    },
  },
  'pm@insure.com': {
    password: 'pm123',
    userInfo: {
      id: 'U003',
      name: '김철수',
      role: 'pm',
      email: 'pm@insure.com',
      department: 'IT혁신팀',
    },
  },
  'dev@insure.com': {
    password: 'dev123',
    userInfo: {
      id: 'U004',
      name: '박민수',
      role: 'developer',
      email: 'dev@insure.com',
      department: 'AI개발팀',
    },
  },
  'qa@insure.com': {
    password: 'qa123',
    userInfo: {
      id: 'U005',
      name: '최지훈',
      role: 'qa',
      email: 'qa@insure.com',
      department: '품질보증팀',
    },
  },
  'ba@insure.com': {
    password: 'ba123',
    userInfo: {
      id: 'U006',
      name: '이영희',
      role: 'business_analyst',
      email: 'ba@insure.com',
      department: '보험심사팀',
    },
  },
  'auditor@insure.com': {
    password: 'auditor123',
    userInfo: {
      id: 'U007',
      name: '감리인',
      role: 'auditor',
      email: 'auditor@insure.com',
      department: '외부감리법인',
    },
  },
  'admin@insure.com': {
    password: 'admin123',
    userInfo: {
      id: 'U008',
      name: '시스템관리자',
      role: 'admin',
      email: 'admin@insure.com',
      department: 'IT운영팀',
    },
  },
};

export default function LoginScreen({ onLogin }: LoginScreenProps) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showQuickLogin, setShowQuickLogin] = useState(true);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const response = await apiService.login(email, password);

      if (response.token && response.user) {
        apiService.setToken(response.token);
        onLogin(response.user);
      } else {
        // Fallback to demo users
        const user = demoUsers[email.toLowerCase()];

        if (user && user.password === password) {
          apiService.setToken('demo-token-' + email);
          onLogin(user.userInfo);
        } else {
          setError('이메일 또는 비밀번호가 올바르지 않습니다.');
          setIsLoading(false);
        }
      }
    } catch (err) {
      // API 실패 시 데모 사용자로 fallback
      const user = demoUsers[email.toLowerCase()];

      if (user && user.password === password) {
        apiService.setToken('demo-token-' + email);
        onLogin(user.userInfo);
      } else {
        setError('이메일 또는 비밀번호가 올바르지 않습니다.');
        setIsLoading(false);
      }
    }
  };

  const handleQuickLogin = async (userEmail: string) => {
    const user = demoUsers[userEmail];
    if (user) {
      setIsLoading(true);
      setEmail(userEmail);
      setPassword(user.password);

      try {
        // 실제 백엔드 로그인 시도
        const response = await apiService.login(userEmail, user.password);

        if (response.token && response.user) {
          apiService.setToken(response.token);
          onLogin(response.user);
        } else {
          // Fallback: Mock 토큰 설정
          apiService.setToken('demo-token-' + userEmail);
          onLogin(user.userInfo);
        }
      } catch (err) {
        // API 실패 시에도 Mock 토큰 설정
        apiService.setToken('demo-token-' + userEmail);
        setTimeout(() => {
          onLogin(user.userInfo);
        }, 800);
      }
    }
  };

  const roleInfo: Record<UserRole, { name: string; color: string; icon: string }> = {
    sponsor: { name: '스폰서', color: 'from-purple-500 to-purple-700', icon: '👔' },
    pmo_head: { name: 'PMO 총괄', color: 'from-indigo-500 to-indigo-700', icon: '📊' },
    pm: { name: 'PM', color: 'from-blue-500 to-blue-700', icon: '🎯' },
    developer: { name: '개발자', color: 'from-green-500 to-green-700', icon: '💻' },
    qa: { name: 'QA', color: 'from-teal-500 to-teal-700', icon: '✅' },
    business_analyst: { name: '현업분석가', color: 'from-amber-500 to-amber-700', icon: '📋' },
    auditor: { name: '감리', color: 'from-gray-500 to-gray-700', icon: '🔍' },
    admin: { name: '관리자', color: 'from-red-500 to-red-700', icon: '⚙️' },
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-900 via-blue-800 to-purple-900 flex items-center justify-center p-6">
      {/* Background Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`,
        }}></div>
      </div>

      <div className="w-full max-w-6xl flex gap-8 relative z-10">
        {/* Left Side - Login Form */}
        <div className="flex-1 bg-white rounded-2xl shadow-2xl p-8">
          <div className="mb-8">
            <div className="flex items-center gap-3 mb-2">
              <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-purple-600 rounded-xl flex items-center justify-center">
                <Shield className="text-white" size={28} />
              </div>
              <div>
                <h1 className="text-2xl font-bold text-gray-900">InsureTech AI-PMS</h1>
                <p className="text-sm text-gray-500">Project Management System</p>
              </div>
            </div>
            <div className="h-1 w-16 bg-gradient-to-r from-blue-600 to-purple-600 rounded-full"></div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
                이메일 주소
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <User className="text-gray-400" size={20} />
                </div>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                  placeholder="your-email@insure.com"
                  required
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
                비밀번호
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="text-gray-400" size={20} />
                </div>
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-10 pr-12 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                  placeholder="••••••••"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
                >
                  {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                </button>
              </div>
            </div>

            {error && (
              <div className="flex items-center gap-2 p-3 bg-red-50 border border-red-200 rounded-lg">
                <AlertCircle className="text-red-600" size={18} />
                <p className="text-sm text-red-600">{error}</p>
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-lg hover:from-blue-700 hover:to-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all font-medium shadow-lg"
            >
              {isLoading ? (
                <span className="flex items-center justify-center gap-2">
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  인증 중...
                </span>
              ) : (
                '로그인'
              )}
            </button>
          </form>

          {/* Security Notice */}
          <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <div className="flex items-start gap-2">
              <Shield className="text-blue-600 mt-0.5" size={16} />
              <div className="text-xs text-blue-900">
                <p className="font-semibold mb-1">보안 환경 안내</p>
                <ul className="space-y-0.5 text-blue-800">
                  <li>• 폐쇄망(On-Premise) 환경에서 운영</li>
                  <li>• 다중 인증(MFA) 필수</li>
                  <li>• 모든 접속 기록은 감사 로그에 저장됩니다</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        {/* Right Side - Quick Login (Demo) */}
        {showQuickLogin && (
          <div className="flex-1 bg-white/10 backdrop-blur-md rounded-2xl shadow-2xl p-8 border border-white/20">
            <div className="mb-6">
              <h2 className="text-xl font-semibold text-white mb-2">빠른 로그인 (데모)</h2>
              <p className="text-sm text-blue-200">역할별 계정을 선택하여 빠르게 시스템을 체험하세요</p>
            </div>

            <div className="space-y-3">
              {Object.entries(demoUsers).map(([userEmail, { userInfo }]) => {
                const info = roleInfo[userInfo.role];
                return (
                  <button
                    key={userEmail}
                    onClick={() => handleQuickLogin(userEmail)}
                    disabled={isLoading}
                    className="w-full p-4 bg-white/90 hover:bg-white rounded-xl transition-all text-left group disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <div className="flex items-center gap-3">
                      <div className={`w-12 h-12 bg-gradient-to-br ${info.color} rounded-lg flex items-center justify-center text-2xl group-hover:scale-110 transition-transform`}>
                        {info.icon}
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <p className="font-semibold text-gray-900">{userInfo.name}</p>
                          <span className="px-2 py-0.5 bg-blue-100 text-blue-700 rounded text-xs">
                            {info.name}
                          </span>
                        </div>
                        <p className="text-xs text-gray-500">{userInfo.department}</p>
                        <p className="text-xs text-gray-400 mt-0.5">{userEmail}</p>
                      </div>
                      <CheckCircle2 className="text-green-500 opacity-0 group-hover:opacity-100 transition-opacity" size={20} />
                    </div>
                  </button>
                );
              })}
            </div>

            <div className="mt-6 p-4 bg-white/10 backdrop-blur-sm rounded-lg border border-white/20">
              <p className="text-xs text-blue-100">
                💡 <span className="font-semibold">Tip:</span> 각 역할마다 다른 메뉴와 권한이 제공됩니다.
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="absolute bottom-6 left-0 right-0 text-center text-white/60 text-xs">
        <p>© 2025 InsureTech AI-PMS. All rights reserved. | Version 1.0 | On-Premise Secure Mode</p>
      </div>
    </div>
  );
}
