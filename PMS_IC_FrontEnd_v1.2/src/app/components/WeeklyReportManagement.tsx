import { useState, useEffect, useCallback } from 'react';
import {
  FileBarChart,
  Search,
  Filter,
  MoreVertical,
  Eye,
  Edit,
  Trash2,
  RefreshCw,
  CheckCircle,
  Clock,
  Send,
  Loader2,
  Plus,
  Sparkles,
  Calendar,
  Download,
  Copy,
} from 'lucide-react';
import { useProject } from '../../contexts/ProjectContext';
import { WeeklyReport, ReportStatus } from '../../types/project';
import { apiService } from '../../services/api';
import { UserRole } from '../App';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from './ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from './ui/dropdown-menu';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import { Badge } from './ui/badge';

interface WeeklyReportManagementProps {
  userRole: UserRole;
}

const statusConfig: Record<ReportStatus, { label: string; color: string; icon: typeof CheckCircle }> = {
  DRAFT: { label: '초안', color: 'bg-gray-100 text-gray-700', icon: Edit },
  SUBMITTED: { label: '제출됨', color: 'bg-blue-100 text-blue-700', icon: Send },
  APPROVED: { label: '승인됨', color: 'bg-green-100 text-green-700', icon: CheckCircle },
};

// 이번 주 시작일/종료일 계산
function getThisWeekDates() {
  const now = new Date();
  const dayOfWeek = now.getDay();
  const monday = new Date(now);
  monday.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));
  const friday = new Date(monday);
  friday.setDate(monday.getDate() + 4);

  return {
    start: monday.toISOString().split('T')[0],
    end: friday.toISOString().split('T')[0],
  };
}

export default function WeeklyReportManagement({ userRole }: WeeklyReportManagementProps) {
  const { currentProject } = useProject();
  const [reports, setReports] = useState<WeeklyReport[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<ReportStatus | 'ALL'>('ALL');

  // 다이얼로그 상태
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isViewDialogOpen, setIsViewDialogOpen] = useState(false);
  const [isAiDialogOpen, setIsAiDialogOpen] = useState(false);
  const [selectedReport, setSelectedReport] = useState<WeeklyReport | null>(null);

  // 새 보고서 폼 상태
  const thisWeek = getThisWeekDates();
  const [newReport, setNewReport] = useState({
    periodStart: thisWeek.start,
    periodEnd: thisWeek.end,
    content: '',
  });
  const [isCreating, setIsCreating] = useState(false);

  // AI 생성 상태
  const [aiPeriod, setAiPeriod] = useState({
    start: thisWeek.start,
    end: thisWeek.end,
  });
  const [aiContext, setAiContext] = useState('');
  const [generatedContent, setGeneratedContent] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);

  // 보고서 목록 로드
  const loadReports = useCallback(async () => {
    if (!currentProject) return;

    setIsLoading(true);
    try {
      const data = await apiService.getWeeklyReports(currentProject.id);
      setReports(data || []);
    } catch (error) {
      console.error('Failed to load reports:', error);
    } finally {
      setIsLoading(false);
    }
  }, [currentProject]);

  useEffect(() => {
    loadReports();
  }, [loadReports]);

  // 필터링된 보고서 목록
  const filteredReports = reports.filter((report) => {
    const matchesSearch =
      report.content?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      report.periodStart.includes(searchTerm) ||
      report.periodEnd.includes(searchTerm);
    const matchesStatus = statusFilter === 'ALL' || report.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  // 보고서 생성
  const handleCreateReport = async () => {
    if (!currentProject || !newReport.content.trim()) return;

    setIsCreating(true);
    try {
      await apiService.createWeeklyReport(currentProject.id, {
        ...newReport,
        status: 'DRAFT',
      });
      setIsCreateDialogOpen(false);
      setNewReport({
        periodStart: thisWeek.start,
        periodEnd: thisWeek.end,
        content: '',
      });
      await loadReports();
    } catch (error) {
      console.error('Failed to create report:', error);
    } finally {
      setIsCreating(false);
    }
  };

  // AI 보고서 생성
  const handleGenerateAiReport = async () => {
    if (!currentProject) return;

    setIsGenerating(true);
    setGeneratedContent('');
    try {
      const result = await apiService.generateAiReport(
        currentProject.id,
        aiPeriod.start,
        aiPeriod.end,
        aiContext || undefined
      );
      setGeneratedContent(result.content || '');
    } catch (error) {
      console.error('Failed to generate AI report:', error);
      setGeneratedContent('AI 보고서 생성에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsGenerating(false);
    }
  };

  // AI 생성 결과를 새 보고서로 사용
  const handleUseGeneratedContent = () => {
    setNewReport({
      periodStart: aiPeriod.start,
      periodEnd: aiPeriod.end,
      content: generatedContent,
    });
    setIsAiDialogOpen(false);
    setIsCreateDialogOpen(true);
  };

  // 보고서 상세 보기
  const handleViewReport = (report: WeeklyReport) => {
    setSelectedReport(report);
    setIsViewDialogOpen(true);
  };

  // 내용 복사
  const handleCopyContent = async (content: string) => {
    try {
      await navigator.clipboard.writeText(content);
    } catch (error) {
      console.error('Failed to copy:', error);
    }
  };

  // 권한 체크
  const canCreate = ['sponsor', 'pmo_head', 'pm', 'business_analyst', 'admin'].includes(userRole);
  const canUseAi = ['pmo_head', 'pm', 'admin'].includes(userRole);

  if (!currentProject) {
    return (
      <div className="p-6 text-center text-gray-500">
        <FileBarChart className="h-12 w-12 mx-auto mb-4 text-gray-300" />
        <p>프로젝트를 먼저 선택해주세요.</p>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">주간 보고서</h1>
          <p className="text-gray-500 mt-1">
            프로젝트 주간 보고서를 작성하고 관리합니다.
          </p>
        </div>
        <div className="flex gap-2">
          {canUseAi && (
            <Button variant="outline" onClick={() => setIsAiDialogOpen(true)}>
              <Sparkles className="h-4 w-4 mr-2" />
              AI 생성
            </Button>
          )}
          {canCreate && (
            <Button onClick={() => setIsCreateDialogOpen(true)}>
              <Plus className="h-4 w-4 mr-2" />
              보고서 작성
            </Button>
          )}
        </div>
      </div>

      {/* 필터 영역 */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="보고서 검색..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <Select value={statusFilter} onValueChange={(v) => setStatusFilter(v as ReportStatus | 'ALL')}>
              <SelectTrigger className="w-40">
                <Filter className="h-4 w-4 mr-2" />
                <SelectValue placeholder="상태 필터" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체</SelectItem>
                {Object.entries(statusConfig).map(([key, { label }]) => (
                  <SelectItem key={key} value={key}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button variant="outline" onClick={loadReports} disabled={isLoading}>
              <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 보고서 목록 */}
      {isLoading ? (
        <div className="text-center py-12">
          <Loader2 className="h-8 w-8 animate-spin mx-auto text-blue-500" />
          <p className="text-gray-500 mt-2">로딩 중...</p>
        </div>
      ) : filteredReports.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <FileBarChart className="h-12 w-12 mx-auto text-gray-300 mb-4" />
            <p className="text-gray-500">
              {searchTerm || statusFilter !== 'ALL'
                ? '검색 결과가 없습니다.'
                : '작성된 보고서가 없습니다.'}
            </p>
            {canCreate && !searchTerm && statusFilter === 'ALL' && (
              <div className="mt-4 flex justify-center gap-2">
                {canUseAi && (
                  <Button variant="outline" onClick={() => setIsAiDialogOpen(true)}>
                    <Sparkles className="h-4 w-4 mr-2" />
                    AI로 생성하기
                  </Button>
                )}
                <Button onClick={() => setIsCreateDialogOpen(true)}>
                  직접 작성하기
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4">
          {filteredReports.map((report) => {
            const status = statusConfig[report.status];
            const StatusIcon = status.icon;

            return (
              <Card key={report.id} className="hover:shadow-md transition-shadow">
                <CardContent className="p-6">
                  <div className="flex items-start justify-between">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-3 mb-2">
                        <div className="flex items-center gap-2 text-gray-700">
                          <Calendar className="h-4 w-4" />
                          <span className="font-semibold">
                            {new Date(report.periodStart).toLocaleDateString('ko-KR')} ~{' '}
                            {new Date(report.periodEnd).toLocaleDateString('ko-KR')}
                          </span>
                        </div>
                        <Badge className={status.color}>
                          <StatusIcon className="h-3 w-3 mr-1" />
                          {status.label}
                        </Badge>
                      </div>
                      <p className="text-gray-600 text-sm line-clamp-3 whitespace-pre-wrap">
                        {report.content?.substring(0, 300) || '내용 없음'}
                        {report.content && report.content.length > 300 && '...'}
                      </p>
                      <div className="flex items-center gap-4 mt-3 text-sm text-gray-400">
                        <span>작성: {new Date(report.createdAt).toLocaleDateString('ko-KR')}</span>
                        {report.submittedAt && (
                          <span>제출: {new Date(report.submittedAt).toLocaleDateString('ko-KR')}</span>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 ml-4">
                      <Button variant="outline" size="sm" onClick={() => handleViewReport(report)}>
                        <Eye className="h-4 w-4 mr-1" />
                        보기
                      </Button>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="sm">
                            <MoreVertical className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => handleViewReport(report)}>
                            <Eye className="h-4 w-4 mr-2" />
                            상세 보기
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => handleCopyContent(report.content || '')}>
                            <Copy className="h-4 w-4 mr-2" />
                            내용 복사
                          </DropdownMenuItem>
                          <DropdownMenuItem>
                            <Download className="h-4 w-4 mr-2" />
                            다운로드
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          <DropdownMenuItem className="text-red-600">
                            <Trash2 className="h-4 w-4 mr-2" />
                            삭제
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* 보고서 생성 다이얼로그 */}
      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-[700px]">
          <DialogHeader>
            <DialogTitle>주간 보고서 작성</DialogTitle>
            <DialogDescription>
              보고서 기간과 내용을 입력하세요.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="periodStart">시작일</Label>
                <Input
                  id="periodStart"
                  type="date"
                  value={newReport.periodStart}
                  onChange={(e) => setNewReport({ ...newReport, periodStart: e.target.value })}
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="periodEnd">종료일</Label>
                <Input
                  id="periodEnd"
                  type="date"
                  value={newReport.periodEnd}
                  onChange={(e) => setNewReport({ ...newReport, periodEnd: e.target.value })}
                />
              </div>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="content">보고서 내용 *</Label>
              <Textarea
                id="content"
                value={newReport.content}
                onChange={(e) => setNewReport({ ...newReport, content: e.target.value })}
                placeholder="주간 업무 내용을 작성하세요..."
                rows={12}
                className="font-mono text-sm"
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
              onClick={handleCreateReport}
              disabled={!newReport.content.trim() || isCreating}
            >
              {isCreating ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  저장 중...
                </>
              ) : (
                '보고서 저장'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 보고서 상세 다이얼로그 */}
      <Dialog open={isViewDialogOpen} onOpenChange={setIsViewDialogOpen}>
        <DialogContent className="sm:max-w-[800px] max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Calendar className="h-5 w-5" />
              주간 보고서
            </DialogTitle>
            <DialogDescription>
              {selectedReport && (
                <>
                  {new Date(selectedReport.periodStart).toLocaleDateString('ko-KR')} ~{' '}
                  {new Date(selectedReport.periodEnd).toLocaleDateString('ko-KR')}
                </>
              )}
            </DialogDescription>
          </DialogHeader>

          {selectedReport && (
            <div className="space-y-4">
              <div className="flex gap-2">
                <Badge className={statusConfig[selectedReport.status].color}>
                  {statusConfig[selectedReport.status].label}
                </Badge>
              </div>

              <div>
                <Label className="text-gray-500">보고서 내용</Label>
                <div className="mt-2 p-4 bg-gray-50 rounded-lg whitespace-pre-wrap text-sm font-mono">
                  {selectedReport.content || '내용 없음'}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <Label className="text-gray-500">작성일</Label>
                  <p>{new Date(selectedReport.createdAt).toLocaleString('ko-KR')}</p>
                </div>
                {selectedReport.submittedAt && (
                  <div>
                    <Label className="text-gray-500">제출일</Label>
                    <p>{new Date(selectedReport.submittedAt).toLocaleString('ko-KR')}</p>
                  </div>
                )}
              </div>
            </div>
          )}

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => handleCopyContent(selectedReport?.content || '')}
            >
              <Copy className="h-4 w-4 mr-2" />
              복사
            </Button>
            <Button onClick={() => setIsViewDialogOpen(false)}>
              닫기
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* AI 보고서 생성 다이얼로그 */}
      <Dialog open={isAiDialogOpen} onOpenChange={setIsAiDialogOpen}>
        <DialogContent className="sm:max-w-[800px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-purple-500" />
              AI 주간 보고서 생성
            </DialogTitle>
            <DialogDescription>
              AI가 프로젝트 태스크와 요구사항을 분석하여 자동으로 보고서를 생성합니다.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label>시작일</Label>
                <Input
                  type="date"
                  value={aiPeriod.start}
                  onChange={(e) => setAiPeriod({ ...aiPeriod, start: e.target.value })}
                />
              </div>
              <div className="grid gap-2">
                <Label>종료일</Label>
                <Input
                  type="date"
                  value={aiPeriod.end}
                  onChange={(e) => setAiPeriod({ ...aiPeriod, end: e.target.value })}
                />
              </div>
            </div>

            <div className="grid gap-2">
              <Label>추가 컨텍스트 (선택사항)</Label>
              <Textarea
                value={aiContext}
                onChange={(e) => setAiContext(e.target.value)}
                placeholder="AI가 참고할 추가 정보를 입력하세요 (예: 특별 이슈, 강조할 사항 등)"
                rows={3}
              />
            </div>

            <Button
              onClick={handleGenerateAiReport}
              disabled={isGenerating}
              className="w-full"
            >
              {isGenerating ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  AI가 보고서를 생성하고 있습니다...
                </>
              ) : (
                <>
                  <Sparkles className="h-4 w-4 mr-2" />
                  보고서 생성
                </>
              )}
            </Button>

            {generatedContent && (
              <div className="grid gap-2">
                <div className="flex items-center justify-between">
                  <Label>생성된 보고서</Label>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleCopyContent(generatedContent)}
                  >
                    <Copy className="h-4 w-4 mr-1" />
                    복사
                  </Button>
                </div>
                <div className="p-4 bg-gray-50 rounded-lg whitespace-pre-wrap text-sm font-mono max-h-96 overflow-y-auto">
                  {generatedContent}
                </div>
              </div>
            )}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsAiDialogOpen(false)}>
              취소
            </Button>
            {generatedContent && (
              <Button onClick={handleUseGeneratedContent}>
                이 내용으로 보고서 작성
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
