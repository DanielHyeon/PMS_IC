import React, { useState, useEffect } from 'react';
import {
  AlertCircle,
  TrendingUp,
  Zap,
  Users,
  Target,
  Activity,
  ChevronDown,
  RefreshCw,
  Filter,
  Wifi,
  WifiOff,
} from 'lucide-react';
import { webSocketService, WipUpdateEvent } from '../../services/websocket';
import { toastService } from '../../services/toast';
import { apiService } from '../../services/api';

interface ColumnWipStatus {
  columnId: string;
  columnName: string;
  currentWip: number;
  wipLimitSoft?: number;
  wipLimitHard?: number;
  isBottleneck: boolean;
  health: 'RED' | 'YELLOW' | 'GREEN';
  softLimitPercentage?: number;
  hardLimitPercentage?: number;
}

interface SprintWipStatus {
  sprintId: string;
  sprintName: string;
  currentWip: number;
  conwipLimit?: number;
  wipValidationEnabled: boolean;
  conwipPercentage?: number;
  health: string;
}

interface PersonalWipStatus {
  assigneeId: string;
  assigneeName: string;
  currentWip: number;
  maxPersonalWip: number;
  percentage: number;
  recentTasks: string[];
}

interface WipDashboardProps {
  projectId: string;
  onRefresh?: () => void;
}

const WipDashboard: React.FC<WipDashboardProps> = ({ projectId, onRefresh }) => {
  const [columns, setColumns] = useState<ColumnWipStatus[]>([]);
  const [sprints, setSprints] = useState<SprintWipStatus[]>([]);
  const [personalWips, setPersonalWips] = useState<PersonalWipStatus[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedColumn, setExpandedColumn] = useState<string | null>(null);
  const [filterHealth, setFilterHealth] = useState<'ALL' | 'RED' | 'YELLOW' | 'GREEN'>('ALL');
  const [filterBottleneck, setFilterBottleneck] = useState(false);
  const [filterViolated, setFilterViolated] = useState(false);
  const [showFilters, setShowFilters] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'name' | 'wip' | 'health'>('name');
  const [wsConnected, setWsConnected] = useState(false);

  useEffect(() => {
    loadWipData();

    // Connect WebSocket for real-time updates
    webSocketService.connect(projectId);

    // Subscribe to WebSocket status
    const unsubscribeStatus = webSocketService.subscribeStatus((status) => {
      setWsConnected(status === 'connected');
    });

    // Subscribe to WIP updates
    const unsubscribeMessage = webSocketService.subscribe((event: WipUpdateEvent) => {
      handleWipUpdate(event);
    });

    return () => {
      unsubscribeStatus();
      unsubscribeMessage();
      webSocketService.disconnect();
    };
  }, [projectId]);

  const handleWipUpdate = (event: WipUpdateEvent) => {
    switch (event.type) {
      case 'WIP_VIOLATION':
        if (event.data.violationType === 'SOFT_LIMIT') {
          toastService.wipViolation(event.data.columnName || 'Unknown', 'soft');
        } else if (event.data.violationType === 'HARD_LIMIT') {
          toastService.wipViolation(event.data.columnName || 'Unknown', 'hard');
        }
        loadWipData(); // Refresh data
        break;
      case 'BOTTLENECK_DETECTED':
        toastService.bottleneckDetected(event.data.columnName || 'Unknown');
        loadWipData();
        break;
      case 'COLUMN_UPDATE':
      case 'SPRINT_UPDATE':
        loadWipData();
        break;
    }
  };

  const loadWipData = async () => {
    setLoading(true);
    try {
      // Mock data - replace with actual API calls
      const mockColumns: ColumnWipStatus[] = [
        {
          columnId: '1',
          columnName: 'To Do',
          currentWip: 8,
          wipLimitSoft: 10,
          wipLimitHard: 15,
          isBottleneck: false,
          health: 'GREEN',
          softLimitPercentage: 80,
          hardLimitPercentage: 53,
        },
        {
          columnId: '2',
          columnName: 'In Progress',
          currentWip: 12,
          wipLimitSoft: 8,
          wipLimitHard: 12,
          isBottleneck: true,
          health: 'YELLOW',
          softLimitPercentage: 150,
          hardLimitPercentage: 100,
        },
        {
          columnId: '3',
          columnName: 'Review',
          currentWip: 13,
          wipLimitSoft: 5,
          wipLimitHard: 10,
          isBottleneck: false,
          health: 'RED',
          softLimitPercentage: 260,
          hardLimitPercentage: 130,
        },
        {
          columnId: '4',
          columnName: 'Done',
          currentWip: 45,
          wipLimitSoft: undefined,
          wipLimitHard: undefined,
          isBottleneck: false,
          health: 'GREEN',
        },
      ];

      const mockSprints: SprintWipStatus[] = [
        {
          sprintId: '1',
          sprintName: 'Sprint 25',
          currentWip: 22,
          conwipLimit: 25,
          wipValidationEnabled: true,
          conwipPercentage: 88,
          health: 'YELLOW',
        },
        {
          sprintId: '2',
          sprintName: 'Sprint 26',
          currentWip: 15,
          conwipLimit: 30,
          wipValidationEnabled: true,
          conwipPercentage: 50,
          health: 'GREEN',
        },
      ];

      const mockPersonalWips: PersonalWipStatus[] = [
        {
          assigneeId: '1',
          assigneeName: 'Alice Chen',
          currentWip: 4,
          maxPersonalWip: 5,
          percentage: 80,
          recentTasks: ['TASK-101', 'TASK-102', 'TASK-103', 'TASK-104'],
        },
        {
          assigneeId: '2',
          assigneeName: 'Bob Smith',
          currentWip: 6,
          maxPersonalWip: 5,
          percentage: 120,
          recentTasks: ['TASK-201', 'TASK-202', 'TASK-203', 'TASK-204', 'TASK-205', 'TASK-206'],
        },
        {
          assigneeId: '3',
          assigneeName: 'Carol Williams',
          currentWip: 3,
          maxPersonalWip: 5,
          percentage: 60,
          recentTasks: ['TASK-301', 'TASK-302', 'TASK-303'],
        },
      ];

      setColumns(mockColumns);
      setSprints(mockSprints);
      setPersonalWips(mockPersonalWips);
    } catch (error) {
      console.error('Failed to load WIP data:', error);
    } finally {
      setLoading(false);
    }
  };

  const getHealthColor = (health: string) => {
    switch (health) {
      case 'RED':
        return 'from-red-500 to-red-600';
      case 'YELLOW':
        return 'from-amber-500 to-amber-600';
      case 'GREEN':
        return 'from-emerald-500 to-emerald-600';
      default:
        return 'from-gray-500 to-gray-600';
    }
  };

  const getHealthBg = (health: string) => {
    switch (health) {
      case 'RED':
        return 'bg-red-50 border-red-200';
      case 'YELLOW':
        return 'bg-amber-50 border-amber-200';
      case 'GREEN':
        return 'bg-emerald-50 border-emerald-200';
      default:
        return 'bg-gray-50 border-gray-200';
    }
  };

  const getHealthTextColor = (health: string) => {
    switch (health) {
      case 'RED':
        return 'text-red-700';
      case 'YELLOW':
        return 'text-amber-700';
      case 'GREEN':
        return 'text-emerald-700';
      default:
        return 'text-gray-700';
    }
  };

  const isColumnViolated = (col: ColumnWipStatus): boolean => {
    if (col.wipLimitHard && col.currentWip > col.wipLimitHard) return true;
    if (col.wipLimitSoft && col.currentWip > col.wipLimitSoft) return true;
    return false;
  };

  let filteredColumns = columns.filter(col => {
    // Filter by health status
    if (filterHealth !== 'ALL' && col.health !== filterHealth) {
      return false;
    }
    // Filter by bottleneck
    if (filterBottleneck && !col.isBottleneck) {
      return false;
    }
    // Filter by violation
    if (filterViolated && !isColumnViolated(col)) {
      return false;
    }
    // Filter by search term
    if (searchTerm && !col.columnName.toLowerCase().includes(searchTerm.toLowerCase())) {
      return false;
    }
    return true;
  });

  // Sort columns
  filteredColumns = filteredColumns.sort((a, b) => {
    switch (sortBy) {
      case 'name':
        return a.columnName.localeCompare(b.columnName);
      case 'wip':
        return b.currentWip - a.currentWip;
      case 'health':
        const healthOrder: Record<string, number> = { RED: 0, YELLOW: 1, GREEN: 2 };
        return healthOrder[a.health] - healthOrder[b.health];
      default:
        return 0;
    }
  });

  const redColumns = columns.filter(col => col.health === 'RED').length;
  const yellowColumns = columns.filter(col => col.health === 'YELLOW').length;
  const bottleneckColumns = columns.filter(col => col.isBottleneck).length;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <RefreshCw className="w-8 h-8 animate-spin text-blue-500 mx-auto mb-2" />
          <p className="text-gray-600">Loading WIP data...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 p-6">
      {/* WebSocket Connection Status */}
      <div className="flex items-center gap-2 px-4 py-2 bg-gray-50 rounded-lg border border-gray-200">
        {wsConnected ? (
          <>
            <Wifi className="w-4 h-4 text-emerald-600" />
            <span className="text-sm text-emerald-700 font-medium">Real-time updates enabled</span>
          </>
        ) : (
          <>
            <WifiOff className="w-4 h-4 text-gray-400" />
            <span className="text-sm text-gray-600">Disconnected - using polling</span>
          </>
        )}
      </div>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold text-gray-900">WIP Dashboard</h2>
          <p className="text-gray-600 text-sm mt-1">Monitor work-in-progress limits and bottlenecks</p>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setShowFilters(!showFilters)}
            className="flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors border border-gray-300"
            title="Open advanced filters"
          >
            <Filter className="w-4 h-4" />
            Filters
          </button>
          <button
            type="button"
            onClick={() => {
              loadWipData();
              onRefresh?.();
            }}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <RefreshCw className="w-4 h-4" />
            Refresh
          </button>
        </div>
      </div>

      {/* Advanced Filters Panel */}
      {showFilters && (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-4 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            {/* Search */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Search by Column</label>
              <input
                type="text"
                placeholder="Type column name..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            {/* Health Status */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2" htmlFor="filterHealth">Status</label>
              <select
                id="filterHealth"
                aria-label="Filter by health status"
                value={filterHealth}
                onChange={(e) => setFilterHealth(e.target.value as any)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="ALL">All Statuses</option>
                <option value="GREEN">Green (Healthy)</option>
                <option value="YELLOW">Yellow (Warning)</option>
                <option value="RED">Red (Critical)</option>
              </select>
            </div>

            {/* Sort */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2" htmlFor="sortBy">Sort By</label>
              <select
                id="sortBy"
                aria-label="Sort columns by"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="name">Column Name</option>
                <option value="wip">WIP Count (High to Low)</option>
                <option value="health">Health Status</option>
              </select>
            </div>

            {/* Toggle Filters */}
            <div className="flex flex-col gap-2">
              <label className="block text-sm font-medium text-gray-700">Quick Filters</label>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setFilterBottleneck(!filterBottleneck)}
                  className={`px-3 py-2 text-sm rounded-lg transition-colors ${
                    filterBottleneck
                      ? 'bg-orange-500 text-white'
                      : 'bg-gray-100 text-gray-700 border border-gray-300 hover:bg-gray-200'
                  }`}
                  title="Show only bottleneck columns"
                >
                  🔴 Bottlenecks
                </button>
                <button
                  type="button"
                  onClick={() => setFilterViolated(!filterViolated)}
                  className={`px-3 py-2 text-sm rounded-lg transition-colors ${
                    filterViolated
                      ? 'bg-red-500 text-white'
                      : 'bg-gray-100 text-gray-700 border border-gray-300 hover:bg-gray-200'
                  }`}
                  title="Show only WIP limit violations"
                >
                  ⚠️ Violated
                </button>
              </div>
            </div>
          </div>

          {/* Active Filters Display */}
          {(filterBottleneck || filterViolated || filterHealth !== 'ALL' || searchTerm) && (
            <div className="flex flex-wrap gap-2 pt-2 border-t border-gray-200">
              {searchTerm && (
                <span className="inline-flex items-center gap-1 bg-blue-100 text-blue-700 px-2 py-1 rounded text-xs">
                  🔍 {searchTerm}
                  <button
                    type="button"
                    onClick={() => setSearchTerm('')}
                    className="hover:text-blue-900 font-bold"
                  >
                    ×
                  </button>
                </span>
              )}
              {filterHealth !== 'ALL' && (
                <span className="inline-flex items-center gap-1 bg-blue-100 text-blue-700 px-2 py-1 rounded text-xs">
                  Status: {filterHealth}
                  <button
                    type="button"
                    onClick={() => setFilterHealth('ALL')}
                    className="hover:text-blue-900 font-bold"
                  >
                    ×
                  </button>
                </span>
              )}
              {filterBottleneck && (
                <span className="inline-flex items-center gap-1 bg-orange-100 text-orange-700 px-2 py-1 rounded text-xs">
                  Bottlenecks Only
                  <button
                    type="button"
                    onClick={() => setFilterBottleneck(false)}
                    className="hover:text-orange-900 font-bold"
                  >
                    ×
                  </button>
                </span>
              )}
              {filterViolated && (
                <span className="inline-flex items-center gap-1 bg-red-100 text-red-700 px-2 py-1 rounded text-xs">
                  Violations Only
                  <button
                    type="button"
                    onClick={() => setFilterViolated(false)}
                    className="hover:text-red-900 font-bold"
                  >
                    ×
                  </button>
                </span>
              )}
            </div>
          )}
        </div>
      )}

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg border border-gray-200 p-4 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between mb-2">
            <p className="text-sm font-medium text-gray-600">Critical Alerts</p>
            {redColumns > 0 && <AlertCircle className="w-4 h-4 text-red-500" />}
          </div>
          <p className="text-3xl font-bold text-red-600">{redColumns}</p>
          <p className="text-xs text-gray-500 mt-1">Columns at hard limit</p>
        </div>

        <div className="bg-white rounded-lg border border-gray-200 p-4 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between mb-2">
            <p className="text-sm font-medium text-gray-600">Warnings</p>
            {yellowColumns > 0 && <Zap className="w-4 h-4 text-amber-500" />}
          </div>
          <p className="text-3xl font-bold text-amber-600">{yellowColumns}</p>
          <p className="text-xs text-gray-500 mt-1">Columns at soft limit</p>
        </div>

        <div className="bg-white rounded-lg border border-gray-200 p-4 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between mb-2">
            <p className="text-sm font-medium text-gray-600">Bottlenecks</p>
            <Target className="w-4 h-4 text-orange-500" />
          </div>
          <p className="text-3xl font-bold text-orange-600">{bottleneckColumns}</p>
          <p className="text-xs text-gray-500 mt-1">Bottleneck columns</p>
        </div>

        <div className="bg-white rounded-lg border border-gray-200 p-4 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between mb-2">
            <p className="text-sm font-medium text-gray-600">Total WIP</p>
            <Activity className="w-4 h-4 text-blue-500" />
          </div>
          <p className="text-3xl font-bold text-blue-600">{columns.reduce((sum, col) => sum + col.currentWip, 0)}</p>
          <p className="text-xs text-gray-500 mt-1">Across all columns</p>
        </div>
      </div>

      {/* Column WIP Status */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
        <div className="bg-gradient-to-r from-gray-50 to-gray-100 px-6 py-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-blue-600" />
              Column WIP Status
            </h3>
            <div className="flex gap-2">
              <button
                onClick={() => setFilterHealth('ALL')}
                className={`px-3 py-1 rounded text-sm transition-colors ${
                  filterHealth === 'ALL'
                    ? 'bg-blue-600 text-white'
                    : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
                }`}
              >
                All
              </button>
              <button
                onClick={() => setFilterHealth('GREEN')}
                className={`px-3 py-1 rounded text-sm transition-colors ${
                  filterHealth === 'GREEN'
                    ? 'bg-emerald-600 text-white'
                    : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
                }`}
              >
                Good
              </button>
              <button
                onClick={() => setFilterHealth('YELLOW')}
                className={`px-3 py-1 rounded text-sm transition-colors ${
                  filterHealth === 'YELLOW'
                    ? 'bg-amber-600 text-white'
                    : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
                }`}
              >
                Warning
              </button>
              <button
                onClick={() => setFilterHealth('RED')}
                className={`px-3 py-1 rounded text-sm transition-colors ${
                  filterHealth === 'RED'
                    ? 'bg-red-600 text-white'
                    : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
                }`}
              >
                Critical
              </button>
            </div>
          </div>
        </div>

        <div className="divide-y divide-gray-200">
          {filteredColumns.map((column) => (
            <div key={column.columnId} className={`border-l-4 ${getHealthBg(column.health)}`}>
              <button
                onClick={() => setExpandedColumn(expandedColumn === column.columnId ? null : column.columnId)}
                className="w-full p-4 hover:bg-white/50 transition-colors text-left"
              >
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <div className={`w-3 h-3 rounded-full bg-gradient-to-r ${getHealthColor(column.health)}`} />
                    <div>
                      <h4 className="font-semibold text-gray-900">{column.columnName}</h4>
                      <p className={`text-sm ${getHealthTextColor(column.health)}`}>
                        {column.health === 'RED' && 'Hard limit exceeded'}
                        {column.health === 'YELLOW' && 'Soft limit exceeded'}
                        {column.health === 'GREEN' && 'Normal'}
                        {column.isBottleneck && ' • Bottleneck detected'}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="text-right">
                      <p className="text-2xl font-bold text-gray-900">{column.currentWip}</p>
                      {column.wipLimitHard && (
                        <p className="text-xs text-gray-500">/ {column.wipLimitHard} hard</p>
                      )}
                    </div>
                    <ChevronDown
                      className={`w-5 h-5 text-gray-400 transition-transform ${
                        expandedColumn === column.columnId ? 'rotate-180' : ''
                      }`}
                    />
                  </div>
                </div>

                {/* Hard Limit Progress Bar */}
                {column.wipLimitHard && (
                  <div className="mb-2">
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-medium text-gray-600">Hard Limit</span>
                      <span className="text-xs text-gray-500">{column.hardLimitPercentage}%</span>
                    </div>
                    <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                      <div
                        className={`h-full bg-gradient-to-r ${getHealthColor(column.health)} transition-all`}
                        style={{ width: `${Math.min(column.hardLimitPercentage || 0, 100)}%` }}
                      />
                    </div>
                  </div>
                )}

                {/* Soft Limit Progress Bar */}
                {column.wipLimitSoft && (
                  <div>
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-medium text-gray-600">Soft Limit</span>
                      <span className="text-xs text-gray-500">{column.softLimitPercentage}%</span>
                    </div>
                    <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                      <div
                        className={`h-full ${
                          column.softLimitPercentage! > 100
                            ? 'bg-amber-500'
                            : 'bg-emerald-500'
                        } transition-all`}
                        style={{ width: `${Math.min(column.softLimitPercentage || 0, 100)}%` }}
                      />
                    </div>
                  </div>
                )}
              </button>

              {/* Expanded Details */}
              {expandedColumn === column.columnId && (
                <div className="px-4 pb-4 bg-white/50">
                  <div className="grid grid-cols-3 gap-4 text-sm">
                    <div>
                      <p className="text-gray-600 font-medium mb-1">Current WIP</p>
                      <p className="text-2xl font-bold text-gray-900">{column.currentWip}</p>
                    </div>
                    {column.wipLimitSoft && (
                      <div>
                        <p className="text-gray-600 font-medium mb-1">Soft Limit</p>
                        <p className="text-2xl font-bold text-amber-600">{column.wipLimitSoft}</p>
                      </div>
                    )}
                    {column.wipLimitHard && (
                      <div>
                        <p className="text-gray-600 font-medium mb-1">Hard Limit</p>
                        <p className="text-2xl font-bold text-red-600">{column.wipLimitHard}</p>
                      </div>
                    )}
                  </div>
                  {column.isBottleneck && (
                    <div className="mt-4 p-3 bg-orange-50 border border-orange-200 rounded">
                      <p className="text-sm text-orange-700 font-medium flex items-center gap-2">
                        <AlertCircle className="w-4 h-4" />
                        This is a bottleneck column. Prioritize completing tasks here.
                      </p>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Sprint CONWIP Status */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <div className="bg-gradient-to-r from-gray-50 to-gray-100 px-6 py-4 border-b border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
              <Target className="w-5 h-5 text-blue-600" />
              Sprint CONWIP
            </h3>
          </div>

          <div className="divide-y divide-gray-200">
            {sprints.map((sprint) => (
              <div key={sprint.sprintId} className={`p-4 ${getHealthBg(sprint.health)}`}>
                <div className="flex items-center justify-between mb-3">
                  <div>
                    <h4 className="font-semibold text-gray-900">{sprint.sprintName}</h4>
                    <p className={`text-sm ${getHealthTextColor(sprint.health)}`}>
                      {sprint.health === 'RED' && 'At limit'}
                      {sprint.health === 'YELLOW' && 'Approaching limit'}
                      {sprint.health === 'GREEN' && 'Good capacity'}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-2xl font-bold text-gray-900">{sprint.currentWip}</p>
                    {sprint.conwipLimit && (
                      <p className="text-xs text-gray-500">/ {sprint.conwipLimit}</p>
                    )}
                  </div>
                </div>

                {sprint.conwipLimit && (
                  <>
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-medium text-gray-600">CONWIP Utilization</span>
                      <span className="text-xs font-semibold text-gray-900">{sprint.conwipPercentage}%</span>
                    </div>
                    <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                      <div
                        className={`h-full bg-gradient-to-r ${getHealthColor(sprint.health)} transition-all`}
                        style={{ width: `${Math.min(sprint.conwipPercentage || 0, 100)}%` }}
                      />
                    </div>
                  </>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Personal WIP Status */}
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
          <div className="bg-gradient-to-r from-gray-50 to-gray-100 px-6 py-4 border-b border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
              <Users className="w-5 h-5 text-blue-600" />
              Personal WIP
            </h3>
          </div>

          <div className="divide-y divide-gray-200">
            {personalWips.map((assignee) => {
              const health = assignee.percentage > 100 ? 'RED' : assignee.percentage > 80 ? 'YELLOW' : 'GREEN';
              return (
                <div key={assignee.assigneeId} className={`p-4 ${getHealthBg(health)}`}>
                  <div className="flex items-center justify-between mb-3">
                    <div>
                      <h4 className="font-semibold text-gray-900">{assignee.assigneeName}</h4>
                      <p className={`text-sm ${getHealthTextColor(health)}`}>
                        {health === 'RED' && 'Overloaded'}
                        {health === 'YELLOW' && 'High load'}
                        {health === 'GREEN' && 'Balanced'}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-2xl font-bold text-gray-900">{assignee.currentWip}</p>
                      <p className="text-xs text-gray-500">/ {assignee.maxPersonalWip}</p>
                    </div>
                  </div>

                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs font-medium text-gray-600">Workload</span>
                    <span className="text-xs font-semibold text-gray-900">{assignee.percentage}%</span>
                  </div>
                  <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                    <div
                      className={`h-full bg-gradient-to-r ${getHealthColor(health)} transition-all`}
                      style={{ width: `${Math.min(assignee.percentage, 100)}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};

export default WipDashboard;
