import { useState, useEffect, useCallback } from 'react';
import {
  GitBranch,
  Clock,
  Target,
  BarChart3,
  Filter,
  RefreshCw,
  Loader2,
  FileText,
  BookOpen,
  CheckSquare,
  Zap,
  ArrowRight,
  TrendingUp,
} from 'lucide-react';
import { useProject } from '../../../contexts/ProjectContext';
import { apiService } from '../../../services/api';
import { UserRole } from '../../App';
import { Button } from '../ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs';
import { Badge } from '../ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';
import {
  LineageGraphDto,
  LineageStatisticsDto,
  LineageEventDto,
  PageResponse,
  NODE_TYPE_CONFIG,
} from '../../../types/lineage';
import LineageGraph from './LineageGraph';
import LineageTimeline from './LineageTimeline';

interface LineageManagementProps {
  userRole: UserRole;
}

export default function LineageManagement({ userRole }: LineageManagementProps) {
  const { currentProject } = useProject();
  const [activeTab, setActiveTab] = useState<'graph' | 'timeline' | 'impact'>('graph');
  const [loading, setLoading] = useState(false);
  const [graphData, setGraphData] = useState<LineageGraphDto | null>(null);
  const [statistics, setStatistics] = useState<LineageStatisticsDto | null>(null);
  const [timelineData, setTimelineData] = useState<PageResponse<LineageEventDto> | null>(null);
  const [timelineFilters, setTimelineFilters] = useState({
    aggregateType: '',
    page: 0,
    size: 20,
  });

  const loadGraphData = useCallback(async () => {
    if (!currentProject?.id) return;
    setLoading(true);
    try {
      const data = await apiService.getLineageGraph(currentProject.id);
      setGraphData(data);
      setStatistics(data.statistics);
    } catch (error) {
      console.error('Failed to load lineage graph:', error);
    } finally {
      setLoading(false);
    }
  }, [currentProject?.id]);

  const loadTimelineData = useCallback(async () => {
    if (!currentProject?.id) return;
    setLoading(true);
    try {
      const data = await apiService.getLineageTimeline(currentProject.id, timelineFilters);
      setTimelineData(data);
    } catch (error) {
      console.error('Failed to load timeline:', error);
    } finally {
      setLoading(false);
    }
  }, [currentProject?.id, timelineFilters]);

  useEffect(() => {
    if (currentProject?.id) {
      if (activeTab === 'graph') {
        loadGraphData();
      } else if (activeTab === 'timeline') {
        loadTimelineData();
      }
    }
  }, [currentProject?.id, activeTab, loadGraphData, loadTimelineData]);

  const handleRefresh = () => {
    if (activeTab === 'graph') {
      loadGraphData();
    } else if (activeTab === 'timeline') {
      loadTimelineData();
    }
  };

  const handleFilterChange = (type: string) => {
    setTimelineFilters((prev) => ({
      ...prev,
      aggregateType: type === 'all' ? '' : type,
      page: 0,
    }));
  };

  if (!currentProject) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <GitBranch className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h2 className="text-lg font-medium text-gray-900">No Project Selected</h2>
          <p className="text-gray-500 mt-1">Please select a project to view lineage data</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <GitBranch className="h-6 w-6 text-blue-600" />
            Lineage & History
          </h1>
          <p className="text-gray-500 mt-1">
            Track relationships, changes, and impact across requirements, stories, and tasks
          </p>
        </div>
        <Button variant="outline" onClick={handleRefresh} disabled={loading}>
          {loading ? (
            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
          ) : (
            <RefreshCw className="h-4 w-4 mr-2" />
          )}
          Refresh
        </Button>
      </div>

      {/* Statistics Cards */}
      {statistics && (
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4">
          <StatCard
            label="Requirements"
            value={statistics.requirements}
            icon={FileText}
            color="blue"
          />
          <StatCard
            label="User Stories"
            value={statistics.stories}
            icon={BookOpen}
            color="purple"
          />
          <StatCard
            label="Tasks"
            value={statistics.tasks}
            icon={CheckSquare}
            color="green"
          />
          <StatCard
            label="Sprints"
            value={statistics.sprints}
            icon={Zap}
            color="red"
          />
          <StatCard
            label="Linked"
            value={statistics.linkedRequirements}
            icon={ArrowRight}
            color="cyan"
          />
          <StatCard
            label="Unlinked"
            value={statistics.unlinkedRequirements}
            icon={Target}
            color="amber"
          />
          <StatCard
            label="Coverage"
            value={`${statistics.coverage}%`}
            icon={TrendingUp}
            color="emerald"
          />
        </div>
      )}

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as typeof activeTab)}>
        <div className="flex items-center justify-between">
          <TabsList>
            <TabsTrigger value="graph" className="flex items-center gap-2">
              <GitBranch className="h-4 w-4" />
              Lineage Graph
            </TabsTrigger>
            <TabsTrigger value="timeline" className="flex items-center gap-2">
              <Clock className="h-4 w-4" />
              Activity Timeline
            </TabsTrigger>
            <TabsTrigger value="impact" className="flex items-center gap-2">
              <Target className="h-4 w-4" />
              Impact Analysis
            </TabsTrigger>
          </TabsList>

          {activeTab === 'timeline' && (
            <div className="flex items-center gap-2">
              <Filter className="h-4 w-4 text-gray-500" />
              <Select
                value={timelineFilters.aggregateType || 'all'}
                onValueChange={handleFilterChange}
              >
                <SelectTrigger className="w-[180px]">
                  <SelectValue placeholder="Filter by type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Types</SelectItem>
                  <SelectItem value="REQUIREMENT">Requirements</SelectItem>
                  <SelectItem value="USER_STORY">User Stories</SelectItem>
                  <SelectItem value="TASK">Tasks</SelectItem>
                  <SelectItem value="SPRINT">Sprints</SelectItem>
                </SelectContent>
              </Select>
            </div>
          )}
        </div>

        <TabsContent value="graph" className="mt-4">
          <Card>
            <CardContent className="p-0">
              {loading ? (
                <div className="flex items-center justify-center h-[600px]">
                  <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
                </div>
              ) : graphData ? (
                <LineageGraph data={graphData} />
              ) : (
                <div className="flex items-center justify-center h-[600px] text-gray-500">
                  No lineage data available
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="timeline" className="mt-4">
          <Card>
            <CardContent className="p-4">
              {loading ? (
                <div className="flex items-center justify-center h-[400px]">
                  <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
                </div>
              ) : timelineData ? (
                <LineageTimeline
                  data={timelineData}
                  onPageChange={(page) =>
                    setTimelineFilters((prev) => ({ ...prev, page }))
                  }
                />
              ) : (
                <div className="flex items-center justify-center h-[400px] text-gray-500">
                  No activity data available
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="impact" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Target className="h-5 w-5" />
                Impact Analysis
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-center py-12 text-gray-500">
                <BarChart3 className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                <p>Select an entity from the graph or timeline to analyze its impact</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Legend */}
      <Card>
        <CardContent className="py-3">
          <div className="flex items-center justify-center gap-6 flex-wrap">
            {Object.entries(NODE_TYPE_CONFIG).map(([type, config]) => (
              <div key={type} className="flex items-center gap-2">
                <div
                  className="w-4 h-4 rounded"
                  style={{ backgroundColor: config.bgColor, borderColor: config.color, borderWidth: 2 }}
                />
                <span className="text-sm text-gray-600">{config.label}</span>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

interface StatCardProps {
  label: string;
  value: number | string;
  icon: React.ElementType;
  color: 'blue' | 'purple' | 'green' | 'red' | 'cyan' | 'amber' | 'emerald';
}

function StatCard({ label, value, icon: Icon, color }: StatCardProps) {
  const colorClasses = {
    blue: 'bg-blue-50 text-blue-600',
    purple: 'bg-purple-50 text-purple-600',
    green: 'bg-green-50 text-green-600',
    red: 'bg-red-50 text-red-600',
    cyan: 'bg-cyan-50 text-cyan-600',
    amber: 'bg-amber-50 text-amber-600',
    emerald: 'bg-emerald-50 text-emerald-600',
  };

  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex items-center gap-3">
          <div className={`p-2 rounded-lg ${colorClasses[color]}`}>
            <Icon className="h-4 w-4" />
          </div>
          <div>
            <p className="text-2xl font-bold">{value}</p>
            <p className="text-xs text-gray-500">{label}</p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
