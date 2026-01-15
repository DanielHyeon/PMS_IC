import { useState } from 'react';
import { ChevronDown, Plus, FolderKanban, Check, Loader2 } from 'lucide-react';
import { useProject } from '../../contexts/ProjectContext';
import { ProjectStatus } from '../../types/project';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from './ui/dropdown-menu';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';

const statusColors: Record<ProjectStatus, string> = {
  PLANNING: 'bg-blue-100 text-blue-700',
  IN_PROGRESS: 'bg-green-100 text-green-700',
  ON_HOLD: 'bg-yellow-100 text-yellow-700',
  COMPLETED: 'bg-gray-100 text-gray-700',
  CANCELLED: 'bg-red-100 text-red-700',
};

const statusLabels: Record<ProjectStatus, string> = {
  PLANNING: '계획',
  IN_PROGRESS: '진행중',
  ON_HOLD: '보류',
  COMPLETED: '완료',
  CANCELLED: '취소',
};

export default function ProjectSelector() {
  const { currentProject, projects, isLoading, selectProject, createProject } = useProject();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [newProject, setNewProject] = useState({
    name: '',
    description: '',
    startDate: '',
    endDate: '',
    budget: '',
  });
  const [isCreating, setIsCreating] = useState(false);

  const handleSelectProject = async (projectId: string) => {
    if (projectId !== currentProject?.id) {
      await selectProject(projectId);
    }
  };

  const handleCreateProject = async () => {
    if (!newProject.name.trim()) return;

    setIsCreating(true);
    try {
      await createProject({
        name: newProject.name,
        description: newProject.description,
        startDate: newProject.startDate,
        endDate: newProject.endDate,
        budget: newProject.budget ? parseInt(newProject.budget) : 0,
        status: 'PLANNING',
        progress: 0,
      });
      setIsCreateDialogOpen(false);
      setNewProject({ name: '', description: '', startDate: '', endDate: '', budget: '' });
    } catch (error) {
      console.error('Failed to create project:', error);
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="outline"
            className="w-full justify-between bg-white border-gray-200 hover:bg-gray-50"
            disabled={isLoading}
          >
            <div className="flex items-center gap-2 truncate">
              <FolderKanban className="h-4 w-4 text-blue-600 flex-shrink-0" />
              {isLoading ? (
                <span className="text-gray-400">로딩 중...</span>
              ) : currentProject ? (
                <span className="truncate font-medium">{currentProject.name}</span>
              ) : (
                <span className="text-gray-400">프로젝트 선택</span>
              )}
            </div>
            {isLoading ? (
              <Loader2 className="h-4 w-4 animate-spin text-gray-400" />
            ) : (
              <ChevronDown className="h-4 w-4 text-gray-400" />
            )}
          </Button>
        </DropdownMenuTrigger>

        <DropdownMenuContent align="start" className="w-72">
          {projects.length === 0 ? (
            <div className="px-2 py-4 text-center text-sm text-gray-500">
              프로젝트가 없습니다
            </div>
          ) : (
            projects.map((project) => (
              <DropdownMenuItem
                key={project.id}
                onClick={() => handleSelectProject(project.id)}
                className="flex items-center justify-between cursor-pointer"
              >
                <div className="flex items-center gap-2 flex-1 min-w-0">
                  <FolderKanban className="h-4 w-4 text-gray-400 flex-shrink-0" />
                  <span className="truncate">{project.name}</span>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  <span className={`px-2 py-0.5 rounded-full text-xs ${statusColors[project.status]}`}>
                    {statusLabels[project.status]}
                  </span>
                  {currentProject?.id === project.id && (
                    <Check className="h-4 w-4 text-blue-600" />
                  )}
                </div>
              </DropdownMenuItem>
            ))
          )}

          <DropdownMenuSeparator />

          <DropdownMenuItem
            onClick={() => setIsCreateDialogOpen(true)}
            className="text-blue-600 cursor-pointer"
          >
            <Plus className="h-4 w-4 mr-2" />
            새 프로젝트 생성
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      {/* 프로젝트 생성 다이얼로그 */}
      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>새 프로젝트 생성</DialogTitle>
            <DialogDescription>
              새 프로젝트의 기본 정보를 입력하세요.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="name">프로젝트명 *</Label>
              <Input
                id="name"
                value={newProject.name}
                onChange={(e) => setNewProject({ ...newProject, name: e.target.value })}
                placeholder="프로젝트 이름을 입력하세요"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="description">설명</Label>
              <Textarea
                id="description"
                value={newProject.description}
                onChange={(e) => setNewProject({ ...newProject, description: e.target.value })}
                placeholder="프로젝트 설명을 입력하세요"
                rows={3}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="startDate">시작일</Label>
                <Input
                  id="startDate"
                  type="date"
                  value={newProject.startDate}
                  onChange={(e) => setNewProject({ ...newProject, startDate: e.target.value })}
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="endDate">종료일</Label>
                <Input
                  id="endDate"
                  type="date"
                  value={newProject.endDate}
                  onChange={(e) => setNewProject({ ...newProject, endDate: e.target.value })}
                />
              </div>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="budget">예산 (원)</Label>
              <Input
                id="budget"
                type="number"
                value={newProject.budget}
                onChange={(e) => setNewProject({ ...newProject, budget: e.target.value })}
                placeholder="0"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsCreateDialogOpen(false)}
              disabled={isCreating}
            >
              취소
            </Button>
            <Button
              onClick={handleCreateProject}
              disabled={!newProject.name.trim() || isCreating}
            >
              {isCreating ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  생성 중...
                </>
              ) : (
                '프로젝트 생성'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
