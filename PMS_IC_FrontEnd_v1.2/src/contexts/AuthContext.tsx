import React, { createContext, useContext, useState, useCallback, useEffect, ReactNode } from 'react';
import {
  SystemRole,
  ProjectRole,
  UserPermissions,
  ProjectMembership,
  LegacyUserRole,
  mapLegacyRole,
} from '../types/auth';

// 사용자 기본 정보
export interface User {
  id: string;
  name: string;
  email: string;
  department: string;
}

// AuthContext 타입
interface AuthContextType {
  // 사용자 정보
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  
  // 권한 정보
  userPermissions: UserPermissions | null;
  legacyRole: LegacyUserRole | null;  // 기존 호환성
  
  // 인증 메서드
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  
  // 권한 정보 갱신
  refreshPermissions: () => Promise<void>;
  
  // 기존 역할 설정 (LoginScreen 호환용)
  setLegacyUser: (userInfo: {
    id: string;
    name: string;
    role: LegacyUserRole;
    email: string;
    department: string;
  }) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [userPermissions, setUserPermissions] = useState<UserPermissions | null>(null);
  const [legacyRole, setLegacyRole] = useState<LegacyUserRole | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const isAuthenticated = !!user;

  // 로그인
  const login = useCallback(async (email: string, password: string) => {
    setIsLoading(true);
    try {
      // TODO: 실제 API 호출로 교체
      // const response = await apiService.login(email, password);
      // setUser(response.user);
      // setUserPermissions(response.permissions);
      
      console.log('Login with:', email, password);
      // 임시 Mock
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, []);

  // 로그아웃
  const logout = useCallback(() => {
    setUser(null);
    setUserPermissions(null);
    setLegacyRole(null);
    localStorage.removeItem('currentProjectId');
  }, []);

  // 권한 정보 갱신
  const refreshPermissions = useCallback(async () => {
    if (!user) return;
    
    setIsLoading(true);
    try {
      // TODO: 실제 API 호출로 교체
      // const permissions = await apiService.getUserPermissions(user.id);
      // setUserPermissions(permissions);
      
      // 임시: 기존 역할에서 매핑
      if (legacyRole) {
        const { systemRole, defaultProjectRole } = mapLegacyRole(legacyRole);
        const mockPermissions: UserPermissions = {
          userId: user.id,
          systemRole,
          projectRoles: defaultProjectRole
            ? [
                {
                  id: 'mock-membership-1',
                  userId: user.id,
                  projectId: '1',
                  role: defaultProjectRole,
                  assignedBy: 'system',
                  assignedAt: new Date().toISOString(),
                },
              ]
            : [],
        };
        setUserPermissions(mockPermissions);
      }
    } catch (error) {
      console.error('Failed to refresh permissions:', error);
    } finally {
      setIsLoading(false);
    }
  }, [user, legacyRole]);

  // 기존 역할 설정 (LoginScreen 호환용)
  const setLegacyUser = useCallback((userInfo: {
    id: string;
    name: string;
    role: LegacyUserRole;
    email: string;
    department: string;
  }) => {
    setUser({
      id: userInfo.id,
      name: userInfo.name,
      email: userInfo.email,
      department: userInfo.department,
    });
    setLegacyRole(userInfo.role);
    
    // 기존 역할에서 권한 매핑
    const { systemRole, defaultProjectRole } = mapLegacyRole(userInfo.role);
    const mockPermissions: UserPermissions = {
      userId: userInfo.id,
      systemRole,
      projectRoles: defaultProjectRole
        ? [
            {
              id: 'mock-membership-1',
              userId: userInfo.id,
              projectId: '1',  // 기본 프로젝트
              role: defaultProjectRole,
              assignedBy: 'system',
              assignedAt: new Date().toISOString(),
            },
          ]
        : [],
    };
    setUserPermissions(mockPermissions);
  }, []);

  // 초기 로드 시 저장된 사용자 정보 복원
  useEffect(() => {
    // TODO: 토큰 기반 인증 시 여기서 토큰 검증 및 사용자 정보 로드
  }, []);

  const value: AuthContextType = {
    user,
    isAuthenticated,
    isLoading,
    userPermissions,
    legacyRole,
    login,
    logout,
    refreshPermissions,
    setLegacyUser,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

// 권한 체크 유틸리티 (컴포넌트 외부에서 사용)
export function getPermissionsForRole(legacyRole: LegacyUserRole): UserPermissions {
  const { systemRole, defaultProjectRole } = mapLegacyRole(legacyRole);
  return {
    userId: '',
    systemRole,
    projectRoles: defaultProjectRole
      ? [
          {
            id: 'mock',
            userId: '',
            projectId: '1',
            role: defaultProjectRole,
            assignedBy: 'system',
            assignedAt: new Date().toISOString(),
          },
        ]
      : [],
  };
}
