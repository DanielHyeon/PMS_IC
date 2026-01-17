import React, { useState, useMemo } from 'react';
import {
  TrendingUp,
  CheckCircle2,
  Clock,
  AlertCircle,
  CalendarDays,
  BarChart3,
  Target,
  Zap,
} from 'lucide-react';

interface ProgressItem {
  id: string;
  title: string;
  progressPercentage: number;
  progressStage: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'DELAYED';
  dueDate?: string;
  completedTasks: number;
  totalTasks: number;
  storyPointsCompleted?: number;
  storyPointsPlanned?: number;
  estimatedHours?: number;
  actualHours?: number;
  assignee?: string;
}

interface ProgressVisualizationProps {
  items: ProgressItem[];
  title?: string;
  showStats?: boolean;
}

const ProgressVisualization: React.FC<ProgressVisualizationProps> = ({
  items,
  title = 'Progress Tracking',
  showStats = true,
}) => {
  const [sortBy, setSortBy] = useState<'progress' | 'dueDate' | 'status'>('progress');
  const [filterStage, setFilterStage] = useState<'ALL' | 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'DELAYED'>('ALL');

  const filteredAndSortedItems = useMemo(() => {
    let filtered = items;

    if (filterStage !== 'ALL') {
      filtered = items.filter((item) => item.progressStage === filterStage);
    }

    return filtered.sort((a, b) => {
      if (sortBy === 'progress') {
        return b.progressPercentage - a.progressPercentage;
      }
      if (sortBy === 'dueDate') {
        if (!a.dueDate && !b.dueDate) return 0;
        if (!a.dueDate) return 1;
        if (!b.dueDate) return -1;
        return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
      }
      return 0;
    });
  }, [items, sortBy, filterStage]);

  const stats = useMemo(() => {
    const completed = items.filter((item) => item.progressStage === 'COMPLETED').length;
    const inProgress = items.filter((item) => item.progressStage === 'IN_PROGRESS').length;
    const delayed = items.filter((item) => item.progressStage === 'DELAYED').length;
    const avgProgress = items.length > 0
      ? Math.round(items.reduce((sum, item) => sum + item.progressPercentage, 0) / items.length)
      : 0;

    return {
      total: items.length,
      completed,
      inProgress,
      delayed,
      avgProgress,
    };
  }, [items]);

  const getStageColor = (stage: string) => {
    switch (stage) {
      case 'COMPLETED':
        return 'from-emerald-500 to-emerald-600';
      case 'IN_PROGRESS':
        return 'from-blue-500 to-blue-600';
      case 'DELAYED':
        return 'from-red-500 to-red-600';
      case 'NOT_STARTED':
        return 'from-gray-400 to-gray-500';
      default:
        return 'from-gray-400 to-gray-500';
    }
  };

  const getStageBg = (stage: string) => {
    switch (stage) {
      case 'COMPLETED':
        return 'bg-emerald-50 border-emerald-200';
      case 'IN_PROGRESS':
        return 'bg-blue-50 border-blue-200';
      case 'DELAYED':
        return 'bg-red-50 border-red-200';
      case 'NOT_STARTED':
        return 'bg-gray-50 border-gray-200';
      default:
        return 'bg-gray-50 border-gray-200';
    }
  };

  const getStageIcon = (stage: string) => {
    switch (stage) {
      case 'COMPLETED':
        return <CheckCircle2 className="w-5 h-5 text-emerald-600" />;
      case 'IN_PROGRESS':
        return <Clock className="w-5 h-5 text-blue-600" />;
      case 'DELAYED':
        return <AlertCircle className="w-5 h-5 text-red-600" />;
      case 'NOT_STARTED':
        return <Target className="w-5 h-5 text-gray-500" />;
      default:
        return <Target className="w-5 h-5 text-gray-500" />;
    }
  };

  const isOverdue = (dueDate?: string) => {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date();
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold text-gray-900 flex items-center gap-2">
            <BarChart3 className="w-8 h-8 text-blue-600" />
            {title}
          </h2>
          <p className="text-gray-600 text-sm mt-1">Track progress across requirements and tasks</p>
        </div>
      </div>

      {/* Statistics Cards */}
      {showStats && (
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
          <div className="bg-white rounded-lg border border-gray-200 p-4 shadow-sm hover:shadow-md transition-shadow">
            <p className="text-sm font-medium text-gray-600 mb-1">Total Items</p>
            <p className="text-3xl font-bold text-gray-900">{stats.total}</p>
          </div>

          <div className="bg-emerald-50 rounded-lg border border-emerald-200 p-4 shadow-sm hover:shadow-md transition-shadow">
            <p className="text-sm font-medium text-emerald-700 mb-1">Completed</p>
            <p className="text-3xl font-bold text-emerald-600">{stats.completed}</p>
            <p className="text-xs text-emerald-600 mt-1">{stats.total > 0 ? Math.round((stats.completed / stats.total) * 100) : 0}%</p>
          </div>

          <div className="bg-blue-50 rounded-lg border border-blue-200 p-4 shadow-sm hover:shadow-md transition-shadow">
            <p className="text-sm font-medium text-blue-700 mb-1">In Progress</p>
            <p className="text-3xl font-bold text-blue-600">{stats.inProgress}</p>
          </div>

          <div className="bg-red-50 rounded-lg border border-red-200 p-4 shadow-sm hover:shadow-md transition-shadow">
            <p className="text-sm font-medium text-red-700 mb-1">Delayed</p>
            <p className="text-3xl font-bold text-red-600">{stats.delayed}</p>
          </div>

          <div className="bg-purple-50 rounded-lg border border-purple-200 p-4 shadow-sm hover:shadow-md transition-shadow">
            <p className="text-sm font-medium text-purple-700 mb-1">Avg Progress</p>
            <p className="text-3xl font-bold text-purple-600">{stats.avgProgress}%</p>
          </div>
        </div>
      )}

      {/* Controls */}
      <div className="flex items-center gap-4 flex-wrap">
        <div>
          <label className="text-sm font-medium text-gray-700 mr-2">Sort by:</label>
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as any)}
            className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="progress">Progress</option>
            <option value="dueDate">Due Date</option>
            <option value="status">Status</option>
          </select>
        </div>

        <div className="flex gap-2">
          <button
            onClick={() => setFilterStage('ALL')}
            className={`px-3 py-2 rounded-lg text-sm transition-colors ${
              filterStage === 'ALL'
                ? 'bg-blue-600 text-white'
                : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
            }`}
          >
            All ({items.length})
          </button>
          <button
            onClick={() => setFilterStage('COMPLETED')}
            className={`px-3 py-2 rounded-lg text-sm transition-colors ${
              filterStage === 'COMPLETED'
                ? 'bg-emerald-600 text-white'
                : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
            }`}
          >
            Completed
          </button>
          <button
            onClick={() => setFilterStage('IN_PROGRESS')}
            className={`px-3 py-2 rounded-lg text-sm transition-colors ${
              filterStage === 'IN_PROGRESS'
                ? 'bg-blue-600 text-white'
                : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
            }`}
          >
            In Progress
          </button>
          <button
            onClick={() => setFilterStage('DELAYED')}
            className={`px-3 py-2 rounded-lg text-sm transition-colors ${
              filterStage === 'DELAYED'
                ? 'bg-red-600 text-white'
                : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
            }`}
          >
            Delayed
          </button>
        </div>
      </div>

      {/* Progress Items */}
      <div className="space-y-3">
        {filteredAndSortedItems.length > 0 ? (
          filteredAndSortedItems.map((item) => (
            <div
              key={item.id}
              className={`rounded-lg border p-4 transition-all hover:shadow-md ${getStageBg(item.progressStage)}`}
            >
              <div className="flex items-start gap-4">
                {/* Stage Icon */}
                <div className="mt-1">{getStageIcon(item.progressStage)}</div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-4 mb-2">
                    <div>
                      <h3 className="font-semibold text-gray-900 truncate">{item.title}</h3>
                      <div className="flex items-center gap-4 mt-1 flex-wrap">
                        {item.assignee && (
                          <span className="text-xs bg-white px-2 py-1 rounded text-gray-700 border border-gray-200">
                            {item.assignee}
                          </span>
                        )}
                        {item.dueDate && (
                          <span
                            className={`text-xs flex items-center gap-1 ${
                              isOverdue(item.dueDate)
                                ? 'text-red-600 font-medium'
                                : 'text-gray-600'
                            }`}
                          >
                            <CalendarDays className="w-3 h-3" />
                            {new Date(item.dueDate).toLocaleDateString('en-US', {
                              month: 'short',
                              day: 'numeric',
                            })}
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Progress Percentage */}
                    <div className="text-right">
                      <p className="text-2xl font-bold text-gray-900">{item.progressPercentage}%</p>
                      <p className="text-xs text-gray-600">
                        {item.completedTasks} / {item.totalTasks} tasks
                      </p>
                    </div>
                  </div>

                  {/* Main Progress Bar */}
                  <div className="mb-3">
                    <div className="h-3 bg-gray-200 rounded-full overflow-hidden">
                      <div
                        className={`h-full bg-gradient-to-r ${getStageColor(item.progressStage)} transition-all`}
                        style={{ width: `${item.progressPercentage}%` }}
                      />
                    </div>
                  </div>

                  {/* Details Grid */}
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-2 text-sm">
                    {item.storyPointsPlanned && (
                      <div>
                        <p className="text-gray-600 font-medium">Story Points</p>
                        <p className="text-lg font-bold text-gray-900">
                          {item.storyPointsCompleted || 0} / {item.storyPointsPlanned}
                        </p>
                      </div>
                    )}

                    {item.estimatedHours && (
                      <div>
                        <p className="text-gray-600 font-medium">Hours</p>
                        <p className="text-lg font-bold text-gray-900">
                          {item.actualHours || 0} / {item.estimatedHours}h
                        </p>
                      </div>
                    )}

                    <div>
                      <p className="text-gray-600 font-medium">Status</p>
                      <p className="text-lg font-bold capitalize">
                        {item.progressStage.replace(/_/g, ' ')}
                      </p>
                    </div>

                    {item.progressStage === 'DELAYED' && (
                      <div className="col-span-2 md:col-span-1">
                        <p className="text-red-600 font-medium text-xs flex items-center gap-1">
                          <AlertCircle className="w-3 h-3" />
                          Overdue
                        </p>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))
        ) : (
          <div className="text-center py-12">
            <Zap className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-600 font-medium">No items match the current filter</p>
            <p className="text-sm text-gray-500">Try adjusting your filter settings</p>
          </div>
        )}
      </div>

      {/* Summary Bar */}
      {filteredAndSortedItems.length > 0 && (
        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg border border-blue-200 p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-blue-600" />
              <p className="font-medium text-gray-900">
                Overall Progress: <span className="text-blue-600">{stats.avgProgress}%</span>
              </p>
            </div>
            <p className="text-sm text-gray-600">
              {filteredAndSortedItems.filter((item) => item.progressStage === 'COMPLETED').length} of{' '}
              {filteredAndSortedItems.length} completed
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProgressVisualization;
