const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3001/api';

export class ApiService {
  private token: string | null = null;
  private useMockData = false;

  constructor() {
    this.token = localStorage.getItem('auth_token');
    this.checkBackendAvailability();
  }

  private async checkBackendAvailability() {
    try {
      const response = await fetch(API_BASE_URL.replace('/api', '/health'), {
        method: 'GET',
        signal: AbortSignal.timeout(3000),
      });
      this.useMockData = !response.ok;
    } catch (error) {
      console.warn('Backend not available, using mock data');
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
    if (this.useMockData) {
      await new Promise((resolve) => setTimeout(resolve, 300));
      return mockData;
    }

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

      return await response.json();
    } catch (error) {
      console.warn(`API call failed for ${endpoint}, using mock data:`, error);
      this.useMockData = true;
      return mockData;
    }
  }

  async login(email: string, password: string) {
    return this.fetchWithFallback(
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
}

export const apiService = new ApiService();
