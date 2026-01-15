import React, { createContext, useContext, useState, useCallback, useEffect, ReactNode } from 'react';
import { Project, ProjectSummary } from '../types/project';
import { apiService } from '../services/api';

interface ProjectContextType {
  // 현재 선택된 프로젝트
  currentProject: Project | null;

  // 프로젝트 목록 (선택기용 요약 정보)
  projects: ProjectSummary[];

  // 로딩 상태
  isLoading: boolean;

  // 프로젝트 선택
  selectProject: (projectId: string) => Promise<void>;

  // 프로젝트 목록 새로고침
  refreshProjects: () => Promise<void>;

  // 프로젝트 생성
  createProject: (data: Partial<Project>) => Promise<Project>;

  // 프로젝트 업데이트
  updateProject: (projectId: string, data: Partial<Project>) => Promise<Project>;
}

const ProjectContext = createContext<ProjectContextType | undefined>(undefined);

interface ProjectProviderProps {
  children: ReactNode;
}

export function ProjectProvider({ children }: ProjectProviderProps) {
  const [currentProject, setCurrentProject] = useState<Project | null>(null);
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // 프로젝트 목록 로드
  const refreshProjects = useCallback(async () => {
    setIsLoading(true);
    try {
      const projectList = await apiService.getProjects();
      setProjects(projectList);

      // 저장된 프로젝트 ID가 있으면 자동 선택
      const savedProjectId = localStorage.getItem('currentProjectId');
      if (savedProjectId && projectList.length > 0) {
        const savedProject = projectList.find((p: ProjectSummary) => p.id === savedProjectId);
        if (savedProject) {
          const fullProject = await apiService.getProject(savedProjectId);
          setCurrentProject(fullProject);
        } else if (projectList.length > 0) {
          // 저장된 프로젝트가 없으면 첫 번째 프로젝트 선택
          const fullProject = await apiService.getProject(projectList[0].id);
          setCurrentProject(fullProject);
          localStorage.setItem('currentProjectId', projectList[0].id);
        }
      } else if (projectList.length > 0 && !currentProject) {
        // 프로젝트가 있는데 선택된 게 없으면 첫 번째 선택
        const fullProject = await apiService.getProject(projectList[0].id);
        setCurrentProject(fullProject);
        localStorage.setItem('currentProjectId', projectList[0].id);
      }
    } catch (error) {
      console.error('Failed to load projects:', error);
    } finally {
      setIsLoading(false);
    }
  }, [currentProject]);

  // 프로젝트 선택
  const selectProject = useCallback(async (projectId: string) => {
    setIsLoading(true);
    try {
      const project = await apiService.getProject(projectId);
      setCurrentProject(project);
      localStorage.setItem('currentProjectId', projectId);
    } catch (error) {
      console.error('Failed to select project:', error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, []);

  // 프로젝트 생성
  const createProject = useCallback(async (data: Partial<Project>): Promise<Project> => {
    setIsLoading(true);
    try {
      const newProject = await apiService.createProject(data);
      await refreshProjects();
      return newProject;
    } catch (error) {
      console.error('Failed to create project:', error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [refreshProjects]);

  // 프로젝트 업데이트
  const updateProject = useCallback(async (projectId: string, data: Partial<Project>): Promise<Project> => {
    setIsLoading(true);
    try {
      const updatedProject = await apiService.updateProject(projectId, data);
      if (currentProject?.id === projectId) {
        setCurrentProject(updatedProject);
      }
      await refreshProjects();
      return updatedProject;
    } catch (error) {
      console.error('Failed to update project:', error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [currentProject, refreshProjects]);

  // 초기 로드
  useEffect(() => {
    refreshProjects();
  }, []);

  const value: ProjectContextType = {
    currentProject,
    projects,
    isLoading,
    selectProject,
    refreshProjects,
    createProject,
    updateProject,
  };

  return (
    <ProjectContext.Provider value={value}>
      {children}
    </ProjectContext.Provider>
  );
}

export function useProject() {
  const context = useContext(ProjectContext);
  if (context === undefined) {
    throw new Error('useProject must be used within a ProjectProvider');
  }
  return context;
}
