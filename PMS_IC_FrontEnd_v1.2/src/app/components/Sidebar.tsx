import { View } from '../App';
import { LucideIcon } from 'lucide-react';
import { UserRole } from '../App';

interface MenuItem {
  id: View;
  label: string;
  icon: LucideIcon;
}

interface SidebarProps {
  menuItems: MenuItem[];
  currentView: View;
  onViewChange: (view: View) => void;
  userRole: UserRole;
}

const roleColors: Record<UserRole, string> = {
  sponsor: 'from-purple-500 to-purple-700',
  pmo_head: 'from-indigo-500 to-indigo-700',
  pm: 'from-blue-500 to-blue-700',
  developer: 'from-green-500 to-green-700',
  qa: 'from-teal-500 to-teal-700',
  business_analyst: 'from-amber-500 to-amber-700',
  auditor: 'from-gray-500 to-gray-700',
  admin: 'from-red-500 to-red-700',
};

export default function Sidebar({ menuItems, currentView, onViewChange, userRole }: SidebarProps) {
  return (
    <aside className={`w-64 bg-gradient-to-b ${roleColors[userRole]} text-white flex flex-col`}>
      <div className="p-6 border-b border-white/20">
        <h1 className="font-semibold text-xl">InsureTech AI-PMS</h1>
        <p className="text-xs text-white/70 mt-1">Project Management System</p>
      </div>

      <nav className="flex-1 p-4 space-y-1">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentView === item.id;
          
          return (
            <button
              key={item.id}
              onClick={() => onViewChange(item.id)}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all ${
                isActive
                  ? 'bg-white/20 text-white shadow-lg backdrop-blur-sm'
                  : 'text-white/70 hover:bg-white/10 hover:text-white'
              }`}
            >
              <Icon size={20} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      <div className="p-4 border-t border-white/20">
        <div className="text-xs text-white/70">
          <p>Version 1.0</p>
          <p className="mt-1">On-Premise Secure Mode</p>
        </div>
      </div>
    </aside>
  );
}