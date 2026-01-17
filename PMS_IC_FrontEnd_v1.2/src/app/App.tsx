import { useState } from 'react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import Dashboard from './components/Dashboard';
import PhaseManagement from './components/PhaseManagement';
import KanbanBoard from './components/KanbanBoard';
import BacklogManagement from './components/BacklogManagement';
import RoleManagement from './components/RoleManagement';
import AIAssistant from './components/AIAssistant';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import LoginScreen from './components/LoginScreen';
import Settings from './components/Settings';
import { LayoutDashboard, GitBranch, Kanban, ListTodo, Users, Settings as SettingsIcon, GraduationCap, FolderOpen, Briefcase, Network, FileText, ClipboardList, History } from 'lucide-react';
import EducationManagement from './components/EducationManagement';
import CommonManagement from './components/CommonManagement';
import ProjectManagement from './components/ProjectManagement';
import PartManagement from './components/PartManagement';
import RfpManagement from './components/RfpManagement';
import RequirementManagement from './components/RequirementManagement';
import { LineageManagement } from './components/lineage';
import { ProjectProvider } from '../contexts/ProjectContext';

export type View = 'dashboard' | 'projects' | 'parts' | 'rfp' | 'requirements' | 'lineage' | 'phases' | 'kanban' | 'backlog' | 'roles' | 'common' | 'education' | 'settings';

export type UserRole = 'sponsor' | 'pmo_head' | 'pm' | 'developer' | 'qa' | 'business_analyst' | 'auditor' | 'admin';

export interface UserInfo {
  id: string;
  name: string;
  role: UserRole;
  email: string;
  department: string;
}

export default function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [currentUser, setCurrentUser] = useState<UserInfo | null>(null);
  const [currentView, setCurrentView] = useState<View>('dashboard');
  const [aiPanelOpen, setAiPanelOpen] = useState(false);

  const handleLogin = (userInfo: UserInfo) => {
    setCurrentUser(userInfo);
    setIsLoggedIn(true);
    setCurrentView('dashboard');
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    setCurrentUser(null);
    setAiPanelOpen(false);
  };

  // 역할별 메뉴 접근 권한
  const getAvailableMenus = (role: UserRole): View[] => {
    const menuAccess: Record<UserRole, View[]> = {
      sponsor: ['dashboard', 'rfp', 'requirements', 'lineage', 'phases', 'roles', 'common', 'education', 'settings'],
      pmo_head: ['dashboard', 'projects', 'parts', 'rfp', 'requirements', 'lineage', 'phases', 'kanban', 'backlog', 'roles', 'common', 'education', 'settings'],
      pm: ['dashboard', 'projects', 'parts', 'rfp', 'requirements', 'lineage', 'phases', 'kanban', 'backlog', 'common', 'education', 'settings'],
      developer: ['dashboard', 'requirements', 'lineage', 'kanban', 'backlog', 'education', 'settings'],
      qa: ['dashboard', 'requirements', 'lineage', 'kanban', 'backlog', 'education', 'settings'],
      business_analyst: ['dashboard', 'rfp', 'requirements', 'lineage', 'phases', 'backlog', 'education', 'settings'],
      auditor: ['dashboard', 'requirements', 'lineage', 'phases', 'roles', 'settings'],
      admin: ['dashboard', 'projects', 'parts', 'rfp', 'requirements', 'lineage', 'phases', 'kanban', 'backlog', 'roles', 'common', 'education', 'settings'],
    };
    return menuAccess[role] || [];
  };

  const allMenuItems = [
    { id: 'dashboard' as View, label: '통합 대시보드', icon: LayoutDashboard },
    { id: 'projects' as View, label: '프로젝트 관리', icon: Briefcase },
    { id: 'parts' as View, label: '파트 관리', icon: Network },
    { id: 'rfp' as View, label: 'RFP 관리', icon: FileText },
    { id: 'requirements' as View, label: '요구사항 관리', icon: ClipboardList },
    { id: 'lineage' as View, label: 'Lineage & History', icon: History },
    { id: 'phases' as View, label: '단계별 관리', icon: GitBranch },
    { id: 'kanban' as View, label: '칸반 보드', icon: Kanban },
    { id: 'backlog' as View, label: '백로그 관리', icon: ListTodo },
    { id: 'roles' as View, label: '권한 관리', icon: Users },
    { id: 'common' as View, label: '공통 관리', icon: FolderOpen },
    { id: 'education' as View, label: '교육 관리', icon: GraduationCap },
    { id: 'settings' as View, label: '설정', icon: SettingsIcon },
  ];

  const availableMenus = currentUser ? getAvailableMenus(currentUser.role) : [];
  const menuItems = allMenuItems.filter((item) => availableMenus.includes(item.id));

  // 역할별 AI 어시스턴트 접근 권한
  const canUseAI = !!currentUser?.role && ['sponsor', 'pmo_head', 'pm', 'developer', 'qa', 'business_analyst'].includes(currentUser.role);

  const renderContent = () => {
    switch (currentView) {
      case 'dashboard':
        return <Dashboard userRole={currentUser?.role || 'pm'} />;
      case 'projects':
        return <ProjectManagement userRole={currentUser?.role || 'pm'} />;
      case 'parts':
        return <PartManagement userRole={currentUser?.role || 'pm'} />;
      case 'rfp':
        return <RfpManagement userRole={currentUser?.role || 'pm'} />;
      case 'requirements':
        return <RequirementManagement userRole={currentUser?.role || 'pm'} />;
      case 'lineage':
        return <LineageManagement userRole={currentUser?.role || 'pm'} />;
      case 'phases':
        return <PhaseManagement userRole={currentUser?.role || 'pm'} />;
      case 'kanban':
        return <KanbanBoard userRole={currentUser?.role || 'pm'} />;
      case 'backlog':
        return <BacklogManagement userRole={currentUser?.role || 'pm'} />;
      case 'roles':
        return <RoleManagement userRole={currentUser?.role || 'pm'} />;
      case 'common':
        return <CommonManagement userRole={currentUser?.role || 'pm'} />;
      case 'education':
        return <EducationManagement userRole={currentUser?.role || 'pm'} />;
      case 'settings':
        return <Settings userRole={currentUser?.role || 'pm'} />;
      default:
        return <Dashboard userRole={currentUser?.role || 'pm'} />;
    }
  };

  if (!isLoggedIn || !currentUser) {
    return <LoginScreen onLogin={handleLogin} />;
  }

  return (
    <DndProvider backend={HTML5Backend}>
      <div className="flex h-screen bg-gray-50">
        <Sidebar
          menuItems={menuItems}
          currentView={currentView}
          onViewChange={setCurrentView}
          userRole={currentUser.role}
        />
        
        <ProjectProvider>
          <div className="flex-1 flex flex-col overflow-hidden">
            <Header
              currentUser={currentUser}
              onAIToggle={() => setAiPanelOpen(!aiPanelOpen)}
              onLogout={handleLogout}
              canUseAI={canUseAI}
            />
            
            <main className="flex-1 overflow-auto">
              {renderContent()}
            </main>
          </div>
        </ProjectProvider>

        {aiPanelOpen && canUseAI && (
          <AIAssistant onClose={() => setAiPanelOpen(false)} userRole={currentUser.role} />
        )}
      </div>
    </DndProvider>
  );
}