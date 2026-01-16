// 프로젝트 관련 타입 정의

export type ProjectStatus = 'PLANNING' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';

export interface Project {
  id: string;
  name: string;
  code?: string;              // 프로젝트 코드 (INS-AI-2025-001)
  description: string;
  status: ProjectStatus;
  startDate: string;
  endDate: string;
  budget: number;
  progress: number;
  ownerId?: string;           // 프로젝트 소유자 (스폰서) ID
  ownerName?: string;         // 프로젝트 소유자 이름
  managerId: string;          // 프로젝트 관리자 (PM) ID
  managerName?: string;       // 프로젝트 관리자 이름
  isDefault?: boolean;        // 대표 프로젝트 여부
  createdBy?: string;         // 생성자 ID
  createdAt: string;
  updatedAt: string;
}

export interface ProjectSummary {
  id: string;
  name: string;
  code?: string;
  status: ProjectStatus;
  progress: number;
  managerName?: string;
  isDefault?: boolean;
}

// RFP 관련 타입
export type RfpStatus = 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
export type ProcessingStatus = 'PENDING' | 'EXTRACTING' | 'COMPLETED' | 'FAILED';

export interface Rfp {
  id: string;
  projectId: string;
  title: string;
  content: string;
  status: RfpStatus;
  processingStatus: ProcessingStatus;
  processingMessage?: string;
  submittedBy: string;
  submittedAt?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  reviewComments?: string;
  createdAt: string;
  updatedAt: string;
}

// 요구사항 관련 타입
export type RequirementPriority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type RequirementStatus = 'IDENTIFIED' | 'ANALYZED' | 'APPROVED' | 'IMPLEMENTED' | 'VERIFIED' | 'DEFERRED' | 'REJECTED';
export type RequirementCategory = 'AI' | 'SI' | 'COMMON' | 'NON_FUNCTIONAL' | 'FUNCTIONAL' | 'TECHNICAL' | 'BUSINESS' | 'CONSTRAINT';

export interface Requirement {
  id: string;
  rfpId: string;
  projectId: string;
  code: string;
  title: string;
  description: string;
  category: RequirementCategory;
  priority: RequirementPriority;
  status: RequirementStatus;
  acceptanceCriteria?: string;
  sourceText?: string;
  linkedTaskIds: string[];
  estimatedEffort?: number;
  actualEffort?: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

// 주간 보고서 관련 타입
export type ReportStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED';

export interface WeeklyReport {
  id: string;
  projectId: string;
  periodStart: string;
  periodEnd: string;
  content: string;
  status: ReportStatus;
  submittedBy: string;
  submittedAt?: string;
  approvedBy?: string;
  approvedAt?: string;
  createdAt: string;
  updatedAt: string;
}

// API 응답 래퍼 타입
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
