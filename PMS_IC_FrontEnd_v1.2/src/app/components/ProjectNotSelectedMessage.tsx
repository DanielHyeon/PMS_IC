import { FolderKanban, ArrowRight } from 'lucide-react';

interface ProjectNotSelectedMessageProps {
  title?: string;
  description?: string;
  showAction?: boolean;
  onNavigateToProjects?: () => void;
}

export default function ProjectNotSelectedMessage({
  title = '프로젝트를 선택해주세요',
  description = '이 화면을 사용하려면 먼저 프로젝트를 선택해야 합니다.',
  showAction = true,
  onNavigateToProjects,
}: ProjectNotSelectedMessageProps) {
  return (
    <div className="flex items-center justify-center min-h-[400px]">
      <div className="text-center max-w-md">
        <div className="w-20 h-20 rounded-full bg-blue-100 flex items-center justify-center mx-auto mb-6">
          <FolderKanban className="text-blue-600" size={40} />
        </div>
        <h3 className="text-xl font-semibold text-gray-900 mb-2">{title}</h3>
        <p className="text-gray-500 mb-6">{description}</p>
        {showAction && (
          <div className="space-y-3">
            <p className="text-sm text-gray-400">
              프로젝트 관리 메뉴에서 작업할 프로젝트를 선택하거나,
              <br />
              헤더의 프로젝트 이름을 클릭하여 빠르게 전환할 수 있습니다.
            </p>
            {onNavigateToProjects && (
              <button
                onClick={onNavigateToProjects}
                className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                프로젝트 관리로 이동
                <ArrowRight size={16} />
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// 접근 권한 없음 메시지
export function AccessDeniedMessage({
  title = '접근 권한이 없습니다',
  description = '이 화면에 접근할 권한이 없습니다. 관리자에게 문의하세요.',
}: {
  title?: string;
  description?: string;
}) {
  return (
    <div className="flex items-center justify-center min-h-[400px]">
      <div className="text-center max-w-md">
        <div className="w-20 h-20 rounded-full bg-red-100 flex items-center justify-center mx-auto mb-6">
          <svg className="text-red-600 w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m0 0v2m0-2h2m-2 0H10m2-10V7a4 4 0 00-8 0v4h16V7a4 4 0 00-4-4H6a4 4 0 00-4 4v10a2 2 0 002 2h12a2 2 0 002-2v-3" />
          </svg>
        </div>
        <h3 className="text-xl font-semibold text-gray-900 mb-2">{title}</h3>
        <p className="text-gray-500">{description}</p>
      </div>
    </div>
  );
}
