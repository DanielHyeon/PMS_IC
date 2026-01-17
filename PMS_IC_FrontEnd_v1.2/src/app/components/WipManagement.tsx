import React, { useState, useEffect, useCallback } from 'react';
import { RefreshCw, Download, AlertCircle, TrendingUp, ChevronDown } from 'lucide-react';
import WipDashboard from './WipDashboard';
import ProgressVisualization from './ProgressVisualization';
import { apiService } from '../../services/api';
import { exportService, ProgressItem } from '../../services/export';
import { toastService } from '../../services/toast';

interface WipManagementProps {
  projectId: string;
  onRefresh?: () => void;
}

type TabType = 'dashboard' | 'progress';

const WipManagement: React.FC<WipManagementProps> = ({ projectId, onRefresh }) => {
  const [activeTab, setActiveTab] = useState<TabType>('dashboard');
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [wipData, setWipData] = useState<any>(null);
  const [progressData, setProgressData] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const [showExportMenu, setShowExportMenu] = useState(false);

  const fetchWipData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch project-wide WIP status
      const wipResponse = await apiService.getProjectWipStatus(projectId);
      setWipData(wipResponse);

      // Fetch progress data
      const progressResponse = await apiService.getProjectProgress(projectId);
      setProgressData(progressResponse);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to fetch WIP data';
      setError(errorMessage);
      console.error('Error fetching WIP data:', err);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    fetchWipData();
  }, [fetchWipData]);

  const handleRefresh = async () => {
    setRefreshing(true);
    await fetchWipData();
    setRefreshing(false);
    onRefresh?.();
  };

  const handleExport = async (format: 'csv' | 'json' | 'print') => {
    try {
      const timestamp = new Date().toISOString().split('T')[0];

      if (activeTab === 'progress' && progressData) {
        if (format === 'csv') {
          await exportService.exportProgressToCsv(
            progressData as ProgressItem[],
            `progress-${timestamp}.csv`
          );
          toastService.success('Progress data exported as CSV');
        } else if (format === 'json') {
          await exportService.exportProgressToJson(
            progressData as ProgressItem[],
            `progress-${timestamp}.json`
          );
          toastService.success('Progress data exported as JSON');
        } else if (format === 'print') {
          exportService.printProgressReport(progressData as ProgressItem[], 'Progress Report');
        }
      } else if (activeTab === 'dashboard' && wipData) {
        if (format === 'csv') {
          await exportService.exportWipStatusToCsv(wipData, `wip-status-${timestamp}.csv`);
          toastService.success('WIP status exported as CSV');
        } else if (format === 'json') {
          await exportService.exportWipStatusToJson(wipData, `wip-status-${timestamp}.json`);
          toastService.success('WIP status exported as JSON');
        }
      }

      setShowExportMenu(false);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Export failed';
      toastService.error(`Failed to export: ${errorMessage}`);
      console.error('Export error:', error);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-2">
            <TrendingUp className="w-8 h-8 text-blue-600" />
            WIP Management
          </h1>
          <p className="text-gray-600 text-sm mt-1">
            Monitor work-in-progress limits and project progress
          </p>
        </div>
        <div className="flex gap-3">
          <button
            type="button"
            onClick={handleRefresh}
            disabled={refreshing}
            className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
            Refresh
          </button>
          <div className="relative">
            <button
              type="button"
              onClick={() => setShowExportMenu(!showExportMenu)}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <Download className="w-4 h-4" />
              Export
              <ChevronDown className="w-4 h-4" />
            </button>

            {/* Export Menu */}
            {showExportMenu && (
              <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-lg shadow-lg z-10">
                <button
                  type="button"
                  onClick={() => handleExport('csv')}
                  className="w-full text-left px-4 py-2 hover:bg-gray-50 flex items-center gap-2 text-gray-700"
                >
                  <Download className="w-4 h-4" />
                  Export as CSV
                </button>
                <button
                  type="button"
                  onClick={() => handleExport('json')}
                  className="w-full text-left px-4 py-2 hover:bg-gray-50 flex items-center gap-2 text-gray-700 border-t border-gray-100"
                >
                  <Download className="w-4 h-4" />
                  Export as JSON
                </button>
                {activeTab === 'progress' && (
                  <button
                    type="button"
                    onClick={() => handleExport('print')}
                    className="w-full text-left px-4 py-2 hover:bg-gray-50 flex items-center gap-2 text-gray-700 border-t border-gray-100"
                  >
                    <Download className="w-4 h-4" />
                    Print Report
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="font-medium text-red-900">Error Loading Data</p>
            <p className="text-sm text-red-700 mt-1">{error}</p>
          </div>
        </div>
      )}

      {/* Tab Navigation */}
      <div className="flex gap-2 border-b border-gray-200">
        <button
          type="button"
          onClick={() => setActiveTab('dashboard')}
          className={`px-4 py-3 font-medium transition-colors relative ${
            activeTab === 'dashboard'
              ? 'text-blue-600'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          WIP Dashboard
          {activeTab === 'dashboard' && (
            <div className="absolute bottom-0 left-0 right-0 h-1 bg-blue-600" />
          )}
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('progress')}
          className={`px-4 py-3 font-medium transition-colors relative ${
            activeTab === 'progress'
              ? 'text-blue-600'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          Progress Tracking
          {activeTab === 'progress' && (
            <div className="absolute bottom-0 left-0 right-0 h-1 bg-blue-600" />
          )}
        </button>
      </div>

      {/* Tab Content */}
      <div className="mt-6">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        ) : (
          <>
            {activeTab === 'dashboard' && (
              <WipDashboard
                projectId={projectId}
                onRefresh={handleRefresh}
              />
            )}
            {activeTab === 'progress' && (
              <ProgressVisualization
                items={progressData || []}
                title="Project Progress"
                showStats={true}
              />
            )}
          </>
        )}
      </div>

      {/* Quick Stats Footer */}
      {wipData && (
        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg border border-blue-200 p-4">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <p className="text-sm text-gray-600 font-medium">Total WIP</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">
                {wipData.totalWip || 0}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-600 font-medium">Columns</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">
                {wipData.columnStatuses?.length || 0}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-600 font-medium">Bottlenecks</p>
              <p className="text-2xl font-bold text-red-600 mt-1">
                {wipData.bottleneckCount || 0}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-600 font-medium">Last Updated</p>
              <p className="text-sm text-gray-900 mt-2">
                {new Date().toLocaleTimeString()}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default WipManagement;
