// 상태 관련 유틸리티 함수

export type StatusType = 'normal' | 'warning' | 'danger';
export type PriorityType = 'high' | 'medium' | 'low';

export interface StatusColors {
  bg: string;
  text: string;
  border: string;
  dot: string;
}

/**
 * 상태별 색상 반환
 */
export const getStatusColor = (status: string): StatusColors => {
  switch (status) {
    case 'normal':
      return { bg: 'bg-green-100', text: 'text-green-700', border: 'border-green-200', dot: 'bg-green-500' };
    case 'warning':
      return { bg: 'bg-yellow-100', text: 'text-yellow-700', border: 'border-yellow-200', dot: 'bg-yellow-500' };
    case 'danger':
      return { bg: 'bg-red-100', text: 'text-red-700', border: 'border-red-200', dot: 'bg-red-500' };
    default:
      return { bg: 'bg-gray-100', text: 'text-gray-700', border: 'border-gray-200', dot: 'bg-gray-500' };
  }
};

/**
 * 상태별 라벨 반환
 */
export const getStatusLabel = (status: string): string => {
  switch (status) {
    case 'normal':
      return '정상';
    case 'warning':
      return '주의';
    case 'danger':
      return '위험';
    default:
      return '알 수 없음';
  }
};

/**
 * 우선순위별 색상 반환
 */
export const getPriorityColor = (priority: string): string => {
  switch (priority) {
    case 'high':
      return 'bg-red-100 text-red-700 border-red-300';
    case 'medium':
      return 'bg-amber-100 text-amber-700 border-amber-300';
    case 'low':
      return 'bg-green-100 text-green-700 border-green-300';
    default:
      return 'bg-gray-100 text-gray-700 border-gray-300';
  }
};

/**
 * 우선순위별 라벨 반환
 */
export const getPriorityLabel = (priority: string): string => {
  switch (priority) {
    case 'high':
      return '높음';
    case 'medium':
      return '보통';
    case 'low':
      return '낮음';
    default:
      return '없음';
  }
};

/**
 * 트랙 타입별 색상 반환
 */
export const getTrackColor = (track: string): string => {
  switch (track) {
    case 'AI':
      return 'bg-blue-100 text-blue-700';
    case 'SI':
      return 'bg-green-100 text-green-700';
    default:
      return 'bg-purple-100 text-purple-700';
  }
};

/**
 * 활동 타입별 색상 반환
 */
export const getActivityColor = (type: string): string => {
  switch (type) {
    case 'success':
      return 'bg-green-500';
    case 'warning':
      return 'bg-amber-500';
    case 'info':
      return 'bg-blue-500';
    default:
      return 'bg-gray-500';
  }
};
