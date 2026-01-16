// Dashboard Mock 데이터

export const trackProgressData = {
  ai: { progress: 58, status: 'normal', tasks: 45, completed: 26 },
  si: { progress: 72, status: 'normal', tasks: 38, completed: 27 },
  common: { progress: 45, status: 'warning', tasks: 22, completed: 10 },
};

export const subProjectData = [
  { name: 'OCR 모델 개발', track: 'AI', progress: 65, status: 'normal', leader: '박민수' },
  { name: '문서 분류 AI', track: 'AI', progress: 48, status: 'warning', leader: '이영희' },
  { name: '청구 심사 자동화', track: 'AI', progress: 72, status: 'normal', leader: '최지훈' },
  { name: '레거시 시스템 연동', track: 'SI', progress: 80, status: 'normal', leader: '김철수' },
  { name: '데이터 마이그레이션', track: 'SI', progress: 55, status: 'warning', leader: '정수민' },
  { name: '보안 인증 모듈', track: '공통', progress: 35, status: 'danger', leader: '한지영' },
  { name: '통합 테스트 환경', track: '공통', progress: 60, status: 'normal', leader: '오민석' },
];

export const partLeaderData = [
  { name: '박민수', role: 'AI 파트 리더', tasks: 15, completed: 10, inProgress: 4, blocked: 1, status: 'normal' },
  { name: '김철수', role: 'SI 파트 리더', tasks: 12, completed: 9, inProgress: 3, blocked: 0, status: 'normal' },
  { name: '한지영', role: '공통 파트 리더', tasks: 8, completed: 3, inProgress: 3, blocked: 2, status: 'danger' },
  { name: '이영희', role: 'AI 개발자', tasks: 10, completed: 5, inProgress: 4, blocked: 1, status: 'warning' },
  { name: '정수민', role: 'SI 개발자', tasks: 11, completed: 6, inProgress: 5, blocked: 0, status: 'normal' },
];

export const phaseData = [
  { phase: '1단계', planned: 100, actual: 100, status: 'completed' },
  { phase: '2단계', planned: 100, actual: 100, status: 'completed' },
  { phase: '3단계', planned: 100, actual: 85, status: 'inProgress' },
  { phase: '4단계', planned: 100, actual: 0, status: 'pending' },
  { phase: '5단계', planned: 100, actual: 0, status: 'pending' },
  { phase: '6단계', planned: 100, actual: 0, status: 'pending' },
];

export const sprintVelocity = [
  { sprint: 'Sprint 1', velocity: 32, planned: 35 },
  { sprint: 'Sprint 2', velocity: 38, planned: 35 },
  { sprint: 'Sprint 3', velocity: 35, planned: 35 },
  { sprint: 'Sprint 4', velocity: 42, planned: 40 },
  { sprint: 'Sprint 5', velocity: 40, planned: 40 },
];

export const burndownData = [
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
