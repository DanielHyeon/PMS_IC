const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8083/api';

export class ApiService {
  private token: string | null = null;
  private useMockData = false;

  constructor() {
    this.token = localStorage.getItem('auth_token');
    // 헬스 체크를 비동기로 실행하여 앱 시작을 막지 않음
    this.checkBackendAvailability().catch(() => {
      // 헬스 체크 실패는 조용히 처리 (실제 API 호출 시 다시 시도)
    });
  }

  private async checkBackendAvailability() {
    try {
      const healthUrl = API_BASE_URL.replace('/api', '') + '/actuator/health';
      const response = await fetch(healthUrl, {
        method: 'GET',
        signal: AbortSignal.timeout(5000), // 타임아웃을 5초로 증가
      });
      this.useMockData = !response.ok;
      if (response.ok) {
        console.log('Backend connected successfully');
      } else {
        console.warn('Backend health check failed, will retry on actual API calls');
        this.useMockData = true;
      }
    } catch (error) {
      // 헬스 체크 실패는 정상적인 상황일 수 있음 (백엔드가 아직 시작 중일 수 있음)
      // 실제 API 호출 시 다시 시도하므로 여기서는 조용히 처리
      console.debug('Backend health check not available, will retry on API calls:', error);
      this.useMockData = true;
    }
  }

  setToken(token: string) {
    this.token = token;
    localStorage.setItem('auth_token', token);
  }

  clearToken() {
    this.token = null;
    localStorage.removeItem('auth_token');
  }

  private async fetchWithFallback<T>(
    endpoint: string,
    options: RequestInit = {},
    mockData: T
  ): Promise<T> {
    // Always try real API first, even if health check failed
    try {
      const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;
      const headers: HeadersInit = {
        ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
        ...(this.token && { Authorization: `Bearer ${this.token}` }),
        ...options.headers,
      };

      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers,
        signal: AbortSignal.timeout(10000),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      // If successful, mark backend as available
      if (this.useMockData) {
        console.log('Backend is now available');
        this.useMockData = false;
      }

      return await response.json();
    } catch (error) {
      // 네트워크 에러인 경우에만 경고, 그 외는 조용히 처리
      if (error instanceof TypeError && error.message.includes('Failed to fetch')) {
        console.warn(`API call failed for ${endpoint}: Backend may not be running. Using mock data.`);
      } else if (error instanceof Error && error.name === 'TimeoutError') {
        console.warn(`API call timeout for ${endpoint}: Backend may be slow. Using mock data.`);
      } else {
        console.debug(`API call failed for ${endpoint}, using mock data:`, error);
      }
      this.useMockData = true;
      await new Promise((resolve) => setTimeout(resolve, 300));
      return mockData;
    }
  }

  async login(email: string, password: string) {
    const response = await this.fetchWithFallback(
      '/auth/login',
      {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      },
      {
        token: 'mock-jwt-token',
        user: {
          id: '1',
          name: email.split('@')[0],
          role: 'pm' as const,
          email,
          department: 'PMO',
        },
      }
    );

    // 백엔드 응답에서 data 필드 추출
    if (response && typeof response === 'object' && 'data' in response) {
      return (response as any).data;
    }

    return response as any;
  }

  async getDashboardStats() {
    return this.fetchWithFallback('/dashboard/stats', {}, {
      overallProgress: 62,
      budgetUsage: 58,
      budgetTotal: 1000,
      budgetUsed: 580,
      activeIssues: 7,
      highPriorityIssues: 3,
      completedTasks: 142,
      totalTasks: 230,
    });
  }

  async getActivities() {
    return this.fetchWithFallback('/dashboard/activities', {}, [
      { user: '박민수', action: 'OCR 모델 v2.1 성능 테스트 완료', time: '5분 전', type: 'success' as const },
      { user: '이영희', action: '데이터 비식별화 문서 승인 요청', time: '1시간 전', type: 'info' as const },
      { user: 'AI 어시스턴트', action: '일정 지연 위험 감지 알림 발송', time: '2시간 전', type: 'warning' as const },
    ]);
  }

  // ========== Project API ==========
  async getProjects() {
    const response = await this.fetchWithFallback('/projects', {}, {
      data: [
        {
          id: '1',
          name: '보험금 청구 AI 자동심사 시스템',
          code: 'PMS-IC-2025',
          status: 'IN_PROGRESS',
          progress: 62,
          startDate: '2025-01-02',
          endDate: '2025-12-31',
        },
      ],
    });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async getProject(projectId: string) {
    const response = await this.fetchWithFallback(`/projects/${projectId}`, {}, {
      data: {
        id: projectId,
        name: '보험금 청구 AI 자동심사 시스템',
        code: 'PMS-IC-2025',
        description: 'AI 기반 보험금 청구 자동심사 시스템 구축 프로젝트',
        status: 'IN_PROGRESS',
        progress: 62,
        startDate: '2025-01-02',
        endDate: '2025-12-31',
        budget: 1000000000,
        budgetUsed: 580000000,
      },
    });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async createProject(data: any) {
    const response = await this.fetchWithFallback('/projects', {
      method: 'POST',
      body: JSON.stringify(data),
    }, { data: { ...data, id: Date.now().toString() } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async updateProject(projectId: string, data: any) {
    const response = await this.fetchWithFallback(`/projects/${projectId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, { data: { ...data, id: projectId } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async deleteProject(projectId: string) {
    return this.fetchWithFallback(`/projects/${projectId}`, {
      method: 'DELETE',
    }, { success: true });
  }

  async setDefaultProject(projectId: string) {
    return this.fetchWithFallback(`/projects/${projectId}/set-default`, {
      method: 'POST',
    }, { success: true, projectId });
  }

  // ========== Part (Sub-Project) API ==========
  async getParts(projectId: string) {
    return this.fetchWithFallback(`/projects/${projectId}/parts`, {}, []);
  }

  async getPart(partId: string) {
    return this.fetchWithFallback(`/parts/${partId}`, {}, null);
  }

  async createPart(projectId: string, data: any) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/parts`, {
      method: 'POST',
      body: JSON.stringify(data),
    }, { data: { ...data, id: `part-${Date.now()}`, projectId } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async updatePart(partId: string, data: any) {
    const response = await this.fetchWithFallback(`/parts/${partId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, { data: { ...data, id: partId } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async deletePart(partId: string) {
    return this.fetchWithFallback(`/parts/${partId}`, {
      method: 'DELETE',
    }, { success: true });
  }

  async assignPartLeader(partId: string, userId: string) {
    return this.fetchWithFallback(`/parts/${partId}/leader`, {
      method: 'PUT',
      body: JSON.stringify({ userId }),
    }, { success: true });
  }

  async getPartMembers(partId: string) {
    return this.fetchWithFallback(`/parts/${partId}/members`, {}, []);
  }

  async addPartMember(partId: string, userId: string) {
    return this.fetchWithFallback(`/parts/${partId}/members`, {
      method: 'POST',
      body: JSON.stringify({ userId }),
    }, { success: true });
  }

  async removePartMember(partId: string, memberId: string) {
    return this.fetchWithFallback(`/parts/${partId}/members/${memberId}`, {
      method: 'DELETE',
    }, { success: true });
  }

  // ========== Project Members API ==========
  async getProjectMembers(projectId: string) {
    return this.fetchWithFallback(`/projects/${projectId}/members`, {}, []);
  }

  async addProjectMember(projectId: string, userId: string, role: string) {
    return this.fetchWithFallback(`/projects/${projectId}/members`, {
      method: 'POST',
      body: JSON.stringify({ userId, role }),
    }, { success: true });
  }

  async updateProjectMemberRole(projectId: string, memberId: string, role: string) {
    return this.fetchWithFallback(`/projects/${projectId}/members/${memberId}`, {
      method: 'PUT',
      body: JSON.stringify({ role }),
    }, { success: true });
  }

  async removeProjectMember(projectId: string, memberId: string) {
    return this.fetchWithFallback(`/projects/${projectId}/members/${memberId}`, {
      method: 'DELETE',
    }, { success: true });
  }

  // ========== Phase API ==========
  async getPhases() {
    return this.fetchWithFallback('/phases', {}, []);
  }

  async updatePhase(phaseId: number, data: any) {
    return this.fetchWithFallback(`/phases/${phaseId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, data);
  }

  async updateDeliverable(phaseId: number, deliverableId: number, data: any) {
    return this.fetchWithFallback(`/phases/${phaseId}/deliverables/${deliverableId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, data);
  }

  async getPhaseDeliverables(phaseId: string) {
    return this.fetchWithFallback(`/phases/${phaseId}/deliverables`, {}, []);
  }

  async uploadDeliverable(params: {
    phaseId: string;
    deliverableId?: string;
    file: File;
    name?: string;
    description?: string;
    type?: string;
  }) {
    const formData = new FormData();
    formData.append('file', params.file);
    if (params.name) formData.append('name', params.name);
    if (params.description) formData.append('description', params.description);
    if (params.type) formData.append('type', params.type);

    const endpoint = params.deliverableId
      ? `/phases/${params.phaseId}/deliverables/${params.deliverableId}/upload`
      : `/phases/${params.phaseId}/deliverables`;

    return this.fetchWithFallback(endpoint, {
      method: 'POST',
      body: formData,
    }, {});
  }

  async approveDeliverable(deliverableId: string, approved: boolean) {
    return this.fetchWithFallback(`/deliverables/${deliverableId}/approval`, {
      method: 'POST',
      body: JSON.stringify({ approved }),
    }, {});
  }

  async downloadDeliverable(deliverableId: string) {
    if (this.useMockData) {
      return null;
    }

    const headers: HeadersInit = {
      ...(this.token && { Authorization: `Bearer ${this.token}` }),
    };

    const response = await fetch(`${API_BASE_URL}/deliverables/${deliverableId}/download`, {
      method: 'GET',
      headers,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.blob();
  }

  async getPhaseKpis(phaseId: string) {
    return this.fetchWithFallback(`/phases/${phaseId}/kpis`, {}, []);
  }

  async createKpi(phaseId: string, data: any) {
    return this.fetchWithFallback(`/phases/${phaseId}/kpis`, {
      method: 'POST',
      body: JSON.stringify(data),
    }, data);
  }

  async updateKpi(phaseId: string, kpiId: string, data: any) {
    return this.fetchWithFallback(`/phases/${phaseId}/kpis/${kpiId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, data);
  }

  async deleteKpi(phaseId: string, kpiId: string) {
    return this.fetchWithFallback(`/phases/${phaseId}/kpis/${kpiId}`, {
      method: 'DELETE',
    }, { message: 'KPI deleted' });
  }

  async getTaskColumns() {
    return this.fetchWithFallback('/tasks/columns', {}, []);
  }

  async createTask(task: any) {
    return this.fetchWithFallback('/tasks', {
      method: 'POST',
      body: JSON.stringify(task),
    }, { ...task, id: Date.now() });
  }

  async updateTask(taskId: number, data: any) {
    return this.fetchWithFallback(`/tasks/${taskId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, data);
  }

  async moveTask(taskId: number, toColumn: string) {
    return this.fetchWithFallback(`/tasks/${taskId}/move`, {
      method: 'PUT',
      body: JSON.stringify({ toColumn }),
    }, { taskId, toColumn });
  }

  async deleteTask(taskId: number) {
    return this.fetchWithFallback(`/tasks/${taskId}`, {
      method: 'DELETE',
    }, { message: 'Task deleted' });
  }

  async getStories(filters?: { status?: string; epic?: string }) {
    const params = new URLSearchParams(filters as any);
    return this.fetchWithFallback(`/stories?${params}`, {}, []);
  }

  async getEpics() {
    return this.fetchWithFallback('/stories/epics', {}, ['OCR 엔진', 'AI 모델', '인프라', '데이터 관리']);
  }

  async createStory(story: any) {
    return this.fetchWithFallback('/stories', {
      method: 'POST',
      body: JSON.stringify(story),
    }, { ...story, id: Date.now() });
  }

  async updateStory(storyId: number, data: any) {
    return this.fetchWithFallback(`/stories/${storyId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, data);
  }

  async updateStoryPriority(storyId: number, direction: 'up' | 'down') {
    return this.fetchWithFallback(`/stories/${storyId}/priority`, {
      method: 'PUT',
      body: JSON.stringify({ direction }),
    }, []);
  }

  async getPermissions() {
    return this.fetchWithFallback('/permissions', {}, [
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
    ]);
  }

  async updateRolePermission(role: string, permissionId: string, granted: boolean) {
    return this.fetchWithFallback('/permissions/role', {
      method: 'PUT',
      body: JSON.stringify({ role, permissionId, granted }),
    }, { message: 'Permission updated' });
  }

  // ========== User Management API (Admin) ==========
  async getUsers() {
    return this.fetchWithFallback('/users', {}, [
      {
        id: 'user-001',
        name: '김철수',
        email: 'kim@example.com',
        department: 'IT기획팀',
        systemRole: 'user',
        legacyRole: 'pm',
        status: 'active',
        createdAt: '2024-01-15',
      },
      {
        id: 'user-002',
        name: '이영희',
        email: 'lee@example.com',
        department: '경영지원팀',
        systemRole: 'user',
        legacyRole: 'sponsor',
        status: 'active',
        createdAt: '2024-01-15',
      },
      {
        id: 'user-003',
        name: '박민수',
        email: 'park@example.com',
        department: 'PMO',
        systemRole: 'pmo',
        legacyRole: 'pmo_head',
        status: 'active',
        createdAt: '2024-02-01',
      },
      {
        id: 'user-004',
        name: '최영수',
        email: 'choi@example.com',
        department: '개발팀',
        systemRole: 'user',
        legacyRole: 'developer',
        status: 'active',
        createdAt: '2024-03-01',
      },
      {
        id: 'user-005',
        name: '정수진',
        email: 'jung@example.com',
        department: 'QA팀',
        systemRole: 'user',
        legacyRole: 'qa',
        status: 'active',
        createdAt: '2024-03-15',
      },
      {
        id: 'user-006',
        name: '한미영',
        email: 'han@example.com',
        department: '현업팀',
        systemRole: 'user',
        legacyRole: 'business_analyst',
        status: 'active',
        createdAt: '2024-04-01',
      },
      {
        id: 'user-007',
        name: '오현우',
        email: 'oh@example.com',
        department: '감사팀',
        systemRole: 'user',
        legacyRole: 'auditor',
        status: 'active',
        createdAt: '2024-04-15',
      },
      {
        id: 'user-admin',
        name: '관리자',
        email: 'admin@example.com',
        department: 'IT운영팀',
        systemRole: 'admin',
        legacyRole: 'admin',
        status: 'active',
        createdAt: '2024-01-01',
      },
    ]);
  }

  async getUser(userId: string) {
    const response = await this.fetchWithFallback(`/users/${userId}`, {}, {
      data: {
        id: userId,
        name: 'Unknown User',
        email: 'unknown@example.com',
        department: 'Unknown',
        systemRole: 'user',
        status: 'active',
      }
    });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async updateUserSystemRole(userId: string, systemRole: string) {
    const response = await this.fetchWithFallback(`/users/${userId}/system-role`, {
      method: 'PUT',
      body: JSON.stringify({ systemRole }),
    }, { data: { userId, systemRole, success: true } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async assignPM(userId: string, projectIds: string[]) {
    const response = await this.fetchWithFallback(`/users/${userId}/assign-pm`, {
      method: 'POST',
      body: JSON.stringify({ projectIds }),
    }, { data: { userId, projectIds, role: 'pm', success: true } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async revokePM(userId: string, projectId: string) {
    const response = await this.fetchWithFallback(`/users/${userId}/revoke-pm`, {
      method: 'POST',
      body: JSON.stringify({ projectId }),
    }, { data: { userId, projectId, success: true } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async getUserProjectRoles(userId: string) {
    return this.fetchWithFallback(`/users/${userId}/project-roles`, {}, []);
  }

  async createUser(data: any) {
    const response = await this.fetchWithFallback('/users', {
      method: 'POST',
      body: JSON.stringify(data),
    }, { data: { ...data, id: `user-${Date.now()}` } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async updateUser(userId: string, data: any) {
    const response = await this.fetchWithFallback(`/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, { data: { ...data, id: userId } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async deleteUser(userId: string) {
    return this.fetchWithFallback(`/users/${userId}`, {
      method: 'DELETE',
    }, { success: true });
  }

  async updateUserStatus(userId: string, status: 'active' | 'inactive') {
    const response = await this.fetchWithFallback(`/users/${userId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    }, { data: { userId, status, success: true } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  // ========== Meeting API ==========
  async getMeetings(projectId: string) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/meetings`, {}, { data: [] });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async createMeeting(projectId: string, data: any) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/meetings`, {
      method: 'POST',
      body: JSON.stringify(data),
    }, { data: { ...data, id: Date.now().toString() } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async updateMeeting(projectId: string, meetingId: string, data: any) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/meetings/${meetingId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, { data: { ...data, id: meetingId } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async deleteMeeting(projectId: string, meetingId: string) {
    return this.fetchWithFallback(`/projects/${projectId}/meetings/${meetingId}`, {
      method: 'DELETE',
    }, { message: 'Meeting deleted' });
  }

  // ========== Issue API ==========
  async getIssues(projectId: string) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/issues`, {}, { data: [] });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async createIssue(projectId: string, data: any) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/issues`, {
      method: 'POST',
      body: JSON.stringify(data),
    }, { data: { ...data, id: Date.now().toString() } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async updateIssue(projectId: string, issueId: string, data: any) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/issues/${issueId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, { data: { ...data, id: issueId } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async updateIssueStatus(projectId: string, issueId: string, status: string) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/issues/${issueId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }, { data: { id: issueId, status } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async deleteIssue(projectId: string, issueId: string) {
    return this.fetchWithFallback(`/projects/${projectId}/issues/${issueId}`, {
      method: 'DELETE',
    }, { message: 'Issue deleted' });
  }

  // ========== Education API ==========
  async getEducations() {
    const response = await this.fetchWithFallback('/educations', {}, { data: [] });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async getEducation(educationId: string) {
    const response = await this.fetchWithFallback(`/educations/${educationId}`, {}, { data: null });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async createEducation(data: any) {
    const response = await this.fetchWithFallback('/educations', {
      method: 'POST',
      body: JSON.stringify(data),
    }, { data: { ...data, id: Date.now().toString() } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async updateEducation(educationId: string, data: any) {
    const response = await this.fetchWithFallback(`/educations/${educationId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, { data: { ...data, id: educationId } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async deleteEducation(educationId: string) {
    return this.fetchWithFallback(`/educations/${educationId}`, {
      method: 'DELETE',
    }, { message: 'Education deleted' });
  }

  // ========== Education Session API ==========
  async getEducationSessions(educationId: string) {
    const response = await this.fetchWithFallback(`/educations/${educationId}/sessions`, {}, { data: [] });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async createEducationSession(educationId: string, data: any) {
    const response = await this.fetchWithFallback(`/educations/${educationId}/sessions`, {
      method: 'POST',
      body: JSON.stringify(data),
    }, { data: { ...data, id: Date.now().toString() } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async updateEducationSession(educationId: string, sessionId: string, data: any) {
    const response = await this.fetchWithFallback(`/educations/${educationId}/sessions/${sessionId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, { data: { ...data, id: sessionId } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async deleteEducationSession(educationId: string, sessionId: string) {
    return this.fetchWithFallback(`/educations/${educationId}/sessions/${sessionId}`, {
      method: 'DELETE',
    }, { message: 'Session deleted' });
  }

  // ========== Education Roadmap API ==========
  async getEducationRoadmaps() {
    const response = await this.fetchWithFallback('/educations/roadmaps', {}, { data: [] });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async getEducationRoadmapsByRole(role: string) {
    const response = await this.fetchWithFallback(`/educations/roadmaps/role/${role}`, {}, { data: [] });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async createEducationRoadmap(data: any) {
    const response = await this.fetchWithFallback('/educations/roadmaps', {
      method: 'POST',
      body: JSON.stringify(data),
    }, { data: { ...data, id: Date.now().toString() } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async updateEducationRoadmap(roadmapId: string, data: any) {
    const response = await this.fetchWithFallback(`/educations/roadmaps/${roadmapId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, { data: { ...data, id: roadmapId } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async deleteEducationRoadmap(roadmapId: string) {
    return this.fetchWithFallback(`/educations/roadmaps/${roadmapId}`, {
      method: 'DELETE',
    }, { message: 'Roadmap deleted' });
  }

  // ========== Education History API ==========
  async getEducationHistoriesBySession(sessionId: string) {
    const response = await this.fetchWithFallback(`/education-histories/session/${sessionId}`, {}, { data: [] });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async getEducationHistoriesByParticipant(participantId: string) {
    const response = await this.fetchWithFallback(`/education-histories/participant/${participantId}`, {}, { data: [] });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async registerEducationParticipant(sessionId: string, data: any) {
    const response = await this.fetchWithFallback(`/education-histories/session/${sessionId}/register`, {
      method: 'POST',
      body: JSON.stringify(data),
    }, { data: { ...data, id: Date.now().toString() } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async updateEducationHistory(historyId: string, data: any) {
    const response = await this.fetchWithFallback(`/education-histories/${historyId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, { data: { ...data, id: historyId } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async cancelEducationRegistration(historyId: string) {
    return this.fetchWithFallback(`/education-histories/${historyId}`, {
      method: 'DELETE',
    }, { message: 'Registration cancelled' });
  }

  // ========== Requirement API ==========
  async getRequirements(projectId: string) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/requirements`, {}, { data: [] });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async getRequirement(projectId: string, requirementId: string) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/requirements/${requirementId}`, {}, { data: null });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async createRequirement(projectId: string, data: any) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/requirements`, {
      method: 'POST',
      body: JSON.stringify(data),
    }, { data: { ...data, id: Date.now().toString(), code: `REQ-${Date.now()}` } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async updateRequirement(projectId: string, requirementId: string, data: any) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/requirements/${requirementId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }, { data: { ...data, id: requirementId } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async deleteRequirement(projectId: string, requirementId: string) {
    return this.fetchWithFallback(`/projects/${projectId}/requirements/${requirementId}`, {
      method: 'DELETE',
    }, { message: 'Requirement deleted' });
  }

  async linkRequirementToTask(projectId: string, requirementId: string, taskId: string) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/requirements/${requirementId}/link-task`, {
      method: 'POST',
      body: JSON.stringify({ taskId }),
    }, { data: { requirementId, taskId, linked: true } });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async unlinkRequirementFromTask(projectId: string, requirementId: string, taskId: string) {
    return this.fetchWithFallback(`/projects/${projectId}/requirements/${requirementId}/unlink-task/${taskId}`, {
      method: 'DELETE',
    }, { message: 'Task unlinked' });
  }

  // ========== RFP Auto-Classification API ==========
  async classifyRfpRequirements(projectId: string, rfpId: string) {
    const response = await this.fetchWithFallback(`/projects/${projectId}/rfp/${rfpId}/classify`, {
      method: 'POST',
    }, {
      data: {
        aiCount: 3,
        siCount: 3,
        commonCount: 2,
        nonFunctionalCount: 2,
        message: 'Requirements classified successfully'
      }
    });
    return response && typeof response === 'object' && 'data' in response ? (response as any).data : response;
  }

  async sendChatMessage(params: { sessionId?: string | null; message: string }) {
    // Chat API needs longer timeout for LLM response
    try {
      const headers: HeadersInit = {
        'Content-Type': 'application/json',
        ...(this.token && { Authorization: `Bearer ${this.token}` }),
      };

      const response = await fetch(`${API_BASE_URL}/chat/message`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          sessionId: params.sessionId ?? null,
          message: params.message,
        }),
        signal: AbortSignal.timeout(120000), // 120 seconds for LLM response
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      // If successful, mark backend as available
      if (this.useMockData) {
        console.log('Backend is now available');
        this.useMockData = false;
      }

      const data = await response.json();

      // Extract data field if present
      if (data && typeof data === 'object' && 'data' in data) {
        return data.data;
      }

      return data;
    } catch (error) {
      console.warn('Chat API call failed, using mock data:', error);
      return {
        sessionId: params.sessionId ?? 'mock-session',
        reply: '안녕하세요! PMS AI 어시스턴트입니다. 현재 Mock 모드로 동작 중입니다.',
        confidence: 0.95,
        suggestions: [
          '프로젝트 진행률 확인',
          '할당된 태스크 조회',
          '이번 스프린트 목표 확인',
        ],
      };
    }
  }
}

export const apiService = new ApiService();
