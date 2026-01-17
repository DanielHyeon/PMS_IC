import { useState, useEffect, useCallback } from 'react';
import {
  ClipboardList,
  Search,
  Filter,
  MoreVertical,
  Eye,
  Edit,
  Link2,
  Trash2,
  RefreshCw,
  CheckCircle,
  AlertTriangle,
  Clock,
  XCircle,
  Loader2,
  Plus,
  ChevronRight,
} from 'lucide-react';
import { useProject } from '../../contexts/ProjectContext';
import {
  Requirement,
  RequirementPriority,
  RequirementStatus,
  RequirementCategory,
} from '../../types/project';
import { apiService } from '../../services/api';
import { UserRole } from '../App';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';
import {
  Card,
  CardContent,
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from './ui/table';

interface RequirementManagementProps {
  userRole: UserRole;
}

const priorityConfig: Record<RequirementPriority, { label: string; color: string }> = {
  CRITICAL: { label: '긴급', color: 'bg-red-100 text-red-700' },
  HIGH: { label: '높음', color: 'bg-orange-100 text-orange-700' },
  MEDIUM: { label: '보통', color: 'bg-yellow-100 text-yellow-700' },
  LOW: { label: '낮음', color: 'bg-gray-100 text-gray-700' },
};

const statusConfig: Record<RequirementStatus, { label: string; color: string; icon: typeof CheckCircle }> = {
  IDENTIFIED: { label: '식별됨', color: 'bg-gray-100 text-gray-700', icon: ClipboardList },
  ANALYZED: { label: '분석됨', color: 'bg-blue-100 text-blue-700', icon: Eye },
  APPROVED: { label: '승인됨', color: 'bg-green-100 text-green-700', icon: CheckCircle },
  IMPLEMENTED: { label: '구현됨', color: 'bg-purple-100 text-purple-700', icon: CheckCircle },
  VERIFIED: { label: '검증됨', color: 'bg-teal-100 text-teal-700', icon: CheckCircle },
  DEFERRED: { label: '보류', color: 'bg-yellow-100 text-yellow-700', icon: Clock },
  REJECTED: { label: '거절됨', color: 'bg-red-100 text-red-700', icon: XCircle },
};

const categoryConfig: Record<RequirementCategory, { label: string; color: string; ratio?: string }> = {
  AI: { label: 'AI', color: 'bg-blue-100 text-blue-700', ratio: '30%' },
  SI: { label: 'SI', color: 'bg-green-100 text-green-700', ratio: '30%' },
  COMMON: { label: '공통', color: 'bg-purple-100 text-purple-700', ratio: '15%' },
  NON_FUNCTIONAL: { label: '비기능', color: 'bg-orange-100 text-orange-700', ratio: '25%' },
  FUNCTIONAL: { label: '기능', color: 'bg-blue-50 text-blue-600' },
  TECHNICAL: { label: '기술', color: 'bg-teal-50 text-teal-600' },
  BUSINESS: { label: '비즈니스', color: 'bg-amber-50 text-amber-600' },
  CONSTRAINT: { label: '제약사항', color: 'bg-red-50 text-red-600' },
};

export default function RequirementManagement({ userRole }: RequirementManagementProps) {
  const { currentProject } = useProject();
  const [requirements, setRequirements] = useState<Requirement[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<RequirementStatus | 'ALL'>('ALL');
  const [categoryFilter, setCategoryFilter] = useState<RequirementCategory | 'ALL'>('ALL');

  // 다이얼로그 상태
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isViewDialogOpen, setIsViewDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isLinkDialogOpen, setIsLinkDialogOpen] = useState(false);
  const [selectedRequirement, setSelectedRequirement] = useState<Requirement | null>(null);

  // 새 요구사항 폼 상태
  const [newRequirement, setNewRequirement] = useState({
    title: '',
    description: '',
    category: 'FUNCTIONAL' as RequirementCategory,
    priority: 'MEDIUM' as RequirementPriority,
    acceptanceCriteria: '',
  });
  const [isCreating, setIsCreating] = useState(false);

  // 수정 폼 상태
  const [editRequirement, setEditRequirement] = useState({
    title: '',
    description: '',
    category: 'FUNCTIONAL' as RequirementCategory,
    priority: 'MEDIUM' as RequirementPriority,
    status: 'IDENTIFIED' as RequirementStatus,
    acceptanceCriteria: '',
  });
  const [isEditing, setIsEditing] = useState(false);

  // 태스크 연결 상태
  const [taskIdToLink, setTaskIdToLink] = useState('');
  const [isLinking, setIsLinking] = useState(false);

  // 요구사항 목록 로드
  const loadRequirements = useCallback(async () => {
    if (!currentProject) return;

    setIsLoading(true);
    try {
      const data = await apiService.getRequirements(currentProject.id);
      setRequirements(data || []);
    } catch (error) {
      console.error('Failed to load requirements:', error);
    } finally {
      setIsLoading(false);
    }
  }, [currentProject]);

  useEffect(() => {
    loadRequirements();
  }, [loadRequirements]);

  // 필터링된 요구사항 목록
  const filteredRequirements = requirements.filter((req) => {
    const matchesSearch =
      req.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      req.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
      req.description?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || req.status === statusFilter;
    const matchesCategory = categoryFilter === 'ALL' || req.category === categoryFilter;
    return matchesSearch && matchesStatus && matchesCategory;
  });

  // 요구사항 생성
  const handleCreateRequirement = async () => {
    if (!currentProject || !newRequirement.title.trim()) return;

    setIsCreating(true);
    try {
      await apiService.createRequirement(currentProject.id, {
        ...newRequirement,
        status: 'IDENTIFIED',
        rfpId: '', // 직접 생성 시 RFP 없음
      });
      setIsCreateDialogOpen(false);
      setNewRequirement({
        title: '',
        description: '',
        category: 'FUNCTIONAL',
        priority: 'MEDIUM',
        acceptanceCriteria: '',
      });
      await loadRequirements();
    } catch (error) {
      console.error('Failed to create requirement:', error);
    } finally {
      setIsCreating(false);
    }
  };

  // 태스크 연결
  const handleLinkTask = async () => {
    if (!currentProject || !selectedRequirement || !taskIdToLink.trim()) return;

    setIsLinking(true);
    try {
      await apiService.linkRequirementToTask(currentProject.id, selectedRequirement.id, taskIdToLink);
      setIsLinkDialogOpen(false);
      setTaskIdToLink('');
      await loadRequirements();
    } catch (error) {
      console.error('Failed to link task:', error);
    } finally {
      setIsLinking(false);
    }
  };

  // 요구사항 상세 보기
  const handleViewRequirement = (requirement: Requirement) => {
    setSelectedRequirement(requirement);
    setIsViewDialogOpen(true);
  };

  // 태스크 연결 다이얼로그 열기
  const handleOpenLinkDialog = (requirement: Requirement) => {
    setSelectedRequirement(requirement);
    setIsLinkDialogOpen(true);
  };

  // 수정 다이얼로그 열기
  const handleOpenEditDialog = (requirement: Requirement) => {
    setSelectedRequirement(requirement);
    setEditRequirement({
      title: requirement.title,
      description: requirement.description || '',
      category: requirement.category,
      priority: requirement.priority,
      status: requirement.status,
      acceptanceCriteria: requirement.acceptanceCriteria || '',
    });
    setIsEditDialogOpen(true);
  };

  // 요구사항 수정
  const handleUpdateRequirement = async () => {
    if (!currentProject || !selectedRequirement || !editRequirement.title.trim()) return;

    setIsEditing(true);
    try {
      await apiService.updateRequirement(currentProject.id, selectedRequirement.id, editRequirement);
      setIsEditDialogOpen(false);
      await loadRequirements();
    } catch (error) {
      console.error('Failed to update requirement:', error);
    } finally {
      setIsEditing(false);
    }
  };

  // 권한 체크
  const canCreate = ['pmo_head', 'pm', 'business_analyst', 'admin'].includes(userRole);
  const canEdit = ['pmo_head', 'pm', 'business_analyst', 'admin'].includes(userRole);
  const canLink = ['pm', 'developer', 'admin'].includes(userRole);

  // 통계 계산
  const stats = {
    total: requirements.length,
    identified: requirements.filter((r) => r.status === 'IDENTIFIED').length,
    approved: requirements.filter((r) => r.status === 'APPROVED').length,
    implemented: requirements.filter((r) => r.status === 'IMPLEMENTED').length,
    verified: requirements.filter((r) => r.status === 'VERIFIED').length,
  };

  if (!currentProject) {
    return (
      <div className="p-6 text-center text-gray-500">
        <ClipboardList className="h-12 w-12 mx-auto mb-4 text-gray-300" />
        <p>프로젝트를 먼저 선택해주세요.</p>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">요구사항 관리</h1>
          <p className="text-gray-500 mt-1">
            프로젝트 요구사항을 추적하고 태스크와 연결합니다.
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => setIsCreateDialogOpen(true)}>
            <Plus className="h-4 w-4 mr-2" />
            요구사항 추가
          </Button>
        )}
      </div>

      {/* 통계 카드 */}
      <div className="grid grid-cols-5 gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="text-2xl font-bold">{stats.total}</div>
            <div className="text-sm text-gray-500">전체</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-2xl font-bold text-gray-600">{stats.identified}</div>
            <div className="text-sm text-gray-500">식별됨</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-2xl font-bold text-green-600">{stats.approved}</div>
            <div className="text-sm text-gray-500">승인됨</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-2xl font-bold text-purple-600">{stats.implemented}</div>
            <div className="text-sm text-gray-500">구현됨</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-2xl font-bold text-teal-600">{stats.verified}</div>
            <div className="text-sm text-gray-500">검증됨</div>
          </CardContent>
        </Card>
      </div>

      {/* 필터 영역 */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="요구사항 검색 (코드, 제목, 설명)..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <Select value={statusFilter} onValueChange={(v) => setStatusFilter(v as RequirementStatus | 'ALL')}>
              <SelectTrigger className="w-36">
                <SelectValue placeholder="상태" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체 상태</SelectItem>
                {Object.entries(statusConfig).map(([key, { label }]) => (
                  <SelectItem key={key} value={key}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select value={categoryFilter} onValueChange={(v) => setCategoryFilter(v as RequirementCategory | 'ALL')}>
              <SelectTrigger className="w-36">
                <SelectValue placeholder="분류" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체 분류</SelectItem>
                {Object.entries(categoryConfig).map(([key, { label }]) => (
                  <SelectItem key={key} value={key}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button variant="outline" onClick={loadRequirements} disabled={isLoading}>
              <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 요구사항 테이블 */}
      {isLoading ? (
        <div className="text-center py-12">
          <Loader2 className="h-8 w-8 animate-spin mx-auto text-blue-500" />
          <p className="text-gray-500 mt-2">로딩 중...</p>
        </div>
      ) : filteredRequirements.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <ClipboardList className="h-12 w-12 mx-auto text-gray-300 mb-4" />
            <p className="text-gray-500">
              {searchTerm || statusFilter !== 'ALL' || categoryFilter !== 'ALL'
                ? '검색 결과가 없습니다.'
                : '등록된 요구사항이 없습니다.'}
            </p>
            {canCreate && !searchTerm && statusFilter === 'ALL' && categoryFilter === 'ALL' && (
              <Button
                variant="outline"
                className="mt-4"
                onClick={() => setIsCreateDialogOpen(true)}
              >
                첫 번째 요구사항 추가하기
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <Card>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-24">코드</TableHead>
                <TableHead>제목</TableHead>
                <TableHead className="w-24">분류</TableHead>
                <TableHead className="w-24">우선순위</TableHead>
                <TableHead className="w-24">상태</TableHead>
                <TableHead className="w-24">연결 태스크</TableHead>
                <TableHead className="w-20"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredRequirements.map((req) => {
                const status = statusConfig[req.status] ?? { label: req.status, color: 'bg-gray-100 text-gray-700', icon: ClipboardList };
                const priority = priorityConfig[req.priority] ?? { label: req.priority, color: 'bg-gray-100 text-gray-700' };
                const category = categoryConfig[req.category] ?? { label: req.category, color: 'bg-gray-100 text-gray-700' };

                return (
                  <TableRow key={req.id} className="cursor-pointer hover:bg-gray-50">
                    <TableCell className="font-mono text-sm">{req.code}</TableCell>
                    <TableCell>
                      <div>
                        <span className="font-medium">{req.title}</span>
                        {req.description && (
                          <p className="text-sm text-gray-500 truncate max-w-md">
                            {req.description}
                          </p>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className={category.color}>
                        {category.label}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Badge className={priority.color}>{priority.label}</Badge>
                    </TableCell>
                    <TableCell>
                      <Badge className={status.color}>{status.label}</Badge>
                    </TableCell>
                    <TableCell>
                      {req.linkedTaskIds?.length > 0 ? (
                        <Badge variant="outline" className="bg-blue-50">
                          <Link2 className="h-3 w-3 mr-1" />
                          {req.linkedTaskIds.length}개
                        </Badge>
                      ) : (
                        <span className="text-gray-400 text-sm">없음</span>
                      )}
                    </TableCell>
                    <TableCell>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="sm">
                            <MoreVertical className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => handleViewRequirement(req)}>
                            <Eye className="h-4 w-4 mr-2" />
                            상세 보기
                          </DropdownMenuItem>
                          {canEdit && (
                            <DropdownMenuItem onClick={() => handleOpenEditDialog(req)}>
                              <Edit className="h-4 w-4 mr-2" />
                              수정
                            </DropdownMenuItem>
                          )}
                          {canLink && (
                            <DropdownMenuItem onClick={() => handleOpenLinkDialog(req)}>
                              <Link2 className="h-4 w-4 mr-2" />
                              태스크 연결
                            </DropdownMenuItem>
                          )}
                          <DropdownMenuSeparator />
                          <DropdownMenuItem className="text-red-600">
                            <Trash2 className="h-4 w-4 mr-2" />
                            삭제
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </Card>
      )}

      {/* 요구사항 생성 다이얼로그 */}
      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogHeader>
            <DialogTitle>새 요구사항 추가</DialogTitle>
            <DialogDescription>
              요구사항 정보를 입력하세요.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="title">제목 *</Label>
              <Input
                id="title"
                value={newRequirement.title}
                onChange={(e) => setNewRequirement({ ...newRequirement, title: e.target.value })}
                placeholder="요구사항 제목"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label>분류</Label>
                <Select
                  value={newRequirement.category}
                  onValueChange={(v) => setNewRequirement({ ...newRequirement, category: v as RequirementCategory })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(categoryConfig).map(([key, { label }]) => (
                      <SelectItem key={key} value={key}>
                        {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label>우선순위</Label>
                <Select
                  value={newRequirement.priority}
                  onValueChange={(v) => setNewRequirement({ ...newRequirement, priority: v as RequirementPriority })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(priorityConfig).map(([key, { label }]) => (
                      <SelectItem key={key} value={key}>
                        {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="description">설명</Label>
              <Textarea
                id="description"
                value={newRequirement.description}
                onChange={(e) => setNewRequirement({ ...newRequirement, description: e.target.value })}
                placeholder="요구사항 상세 설명..."
                rows={4}
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="acceptanceCriteria">인수 조건</Label>
              <Textarea
                id="acceptanceCriteria"
                value={newRequirement.acceptanceCriteria}
                onChange={(e) => setNewRequirement({ ...newRequirement, acceptanceCriteria: e.target.value })}
                placeholder="요구사항이 충족되었는지 확인할 수 있는 조건..."
                rows={3}
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
              onClick={handleCreateRequirement}
              disabled={!newRequirement.title.trim() || isCreating}
            >
              {isCreating ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  추가 중...
                </>
              ) : (
                '요구사항 추가'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 요구사항 상세 다이얼로그 */}
      <Dialog open={isViewDialogOpen} onOpenChange={setIsViewDialogOpen}>
        <DialogContent className="sm:max-w-[700px]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <span className="font-mono text-sm text-gray-500">{selectedRequirement?.code}</span>
              <ChevronRight className="h-4 w-4 text-gray-400" />
              {selectedRequirement?.title}
            </DialogTitle>
            <DialogDescription>
              요구사항 상세 정보를 확인합니다.
            </DialogDescription>
          </DialogHeader>

          {selectedRequirement && (() => {
            const selCategory = categoryConfig[selectedRequirement.category] ?? { label: selectedRequirement.category, color: 'bg-gray-100 text-gray-700' };
            const selPriority = priorityConfig[selectedRequirement.priority] ?? { label: selectedRequirement.priority, color: 'bg-gray-100 text-gray-700' };
            const selStatus = statusConfig[selectedRequirement.status] ?? { label: selectedRequirement.status, color: 'bg-gray-100 text-gray-700' };
            return (
            <div className="space-y-4">
              <div className="flex gap-2">
                <Badge className={selCategory.color}>
                  {selCategory.label}
                </Badge>
                <Badge className={selPriority.color}>
                  {selPriority.label}
                </Badge>
                <Badge className={selStatus.color}>
                  {selStatus.label}
                </Badge>
              </div>

              <div>
                <Label className="text-gray-500">설명</Label>
                <p className="mt-1">{selectedRequirement.description || '설명 없음'}</p>
              </div>

              {selectedRequirement.acceptanceCriteria && (
                <div>
                  <Label className="text-gray-500">인수 조건</Label>
                  <p className="mt-1 whitespace-pre-wrap">{selectedRequirement.acceptanceCriteria}</p>
                </div>
              )}

              {selectedRequirement.sourceText && (
                <div>
                  <Label className="text-gray-500">원본 텍스트 (RFP)</Label>
                  <div className="mt-1 p-3 bg-gray-50 rounded text-sm italic">
                    "{selectedRequirement.sourceText}"
                  </div>
                </div>
              )}

              <div>
                <Label className="text-gray-500">연결된 태스크</Label>
                {selectedRequirement.linkedTaskIds?.length > 0 ? (
                  <div className="mt-1 flex flex-wrap gap-2">
                    {selectedRequirement.linkedTaskIds.map((taskId) => (
                      <Badge key={taskId} variant="outline">
                        <Link2 className="h-3 w-3 mr-1" />
                        {taskId}
                      </Badge>
                    ))}
                  </div>
                ) : (
                  <p className="mt-1 text-gray-400">연결된 태스크가 없습니다.</p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <Label className="text-gray-500">예상 공수</Label>
                  <p>{selectedRequirement.estimatedEffort ? `${selectedRequirement.estimatedEffort}시간` : '-'}</p>
                </div>
                <div>
                  <Label className="text-gray-500">실제 공수</Label>
                  <p>{selectedRequirement.actualEffort ? `${selectedRequirement.actualEffort}시간` : '-'}</p>
                </div>
              </div>
            </div>
            );
          })()}

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsViewDialogOpen(false)}>
              닫기
            </Button>
            {canLink && (
              <Button onClick={() => {
                setIsViewDialogOpen(false);
                handleOpenLinkDialog(selectedRequirement!);
              }}>
                <Link2 className="h-4 w-4 mr-2" />
                태스크 연결
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 태스크 연결 다이얼로그 */}
      <Dialog open={isLinkDialogOpen} onOpenChange={setIsLinkDialogOpen}>
        <DialogContent className="sm:max-w-[400px]">
          <DialogHeader>
            <DialogTitle>태스크 연결</DialogTitle>
            <DialogDescription>
              요구사항 "{selectedRequirement?.title}"에 연결할 태스크 ID를 입력하세요.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="taskId">태스크 ID</Label>
              <Input
                id="taskId"
                value={taskIdToLink}
                onChange={(e) => setTaskIdToLink(e.target.value)}
                placeholder="예: TASK-001"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsLinkDialogOpen(false)}
              disabled={isLinking}
            >
              취소
            </Button>
            <Button
              onClick={handleLinkTask}
              disabled={!taskIdToLink.trim() || isLinking}
            >
              {isLinking ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  연결 중...
                </>
              ) : (
                '연결'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 요구사항 수정 다이얼로그 */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogHeader>
            <DialogTitle>요구사항 수정</DialogTitle>
            <DialogDescription>
              {selectedRequirement?.code} - 요구사항 정보를 수정합니다.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="edit-title">제목 *</Label>
              <Input
                id="edit-title"
                value={editRequirement.title}
                onChange={(e) => setEditRequirement({ ...editRequirement, title: e.target.value })}
                placeholder="요구사항 제목"
              />
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="grid gap-2">
                <Label>분류</Label>
                <Select
                  value={editRequirement.category}
                  onValueChange={(v) => setEditRequirement({ ...editRequirement, category: v as RequirementCategory })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(categoryConfig).map(([key, { label }]) => (
                      <SelectItem key={key} value={key}>
                        {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label>우선순위</Label>
                <Select
                  value={editRequirement.priority}
                  onValueChange={(v) => setEditRequirement({ ...editRequirement, priority: v as RequirementPriority })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(priorityConfig).map(([key, { label }]) => (
                      <SelectItem key={key} value={key}>
                        {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label>상태</Label>
                <Select
                  value={editRequirement.status}
                  onValueChange={(v) => setEditRequirement({ ...editRequirement, status: v as RequirementStatus })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(statusConfig).map(([key, { label }]) => (
                      <SelectItem key={key} value={key}>
                        {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="edit-description">설명</Label>
              <Textarea
                id="edit-description"
                value={editRequirement.description}
                onChange={(e) => setEditRequirement({ ...editRequirement, description: e.target.value })}
                placeholder="요구사항 상세 설명..."
                rows={4}
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="edit-acceptanceCriteria">인수 조건</Label>
              <Textarea
                id="edit-acceptanceCriteria"
                value={editRequirement.acceptanceCriteria}
                onChange={(e) => setEditRequirement({ ...editRequirement, acceptanceCriteria: e.target.value })}
                placeholder="요구사항이 충족되었는지 확인할 수 있는 조건..."
                rows={3}
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsEditDialogOpen(false)}
              disabled={isEditing}
            >
              취소
            </Button>
            <Button
              onClick={handleUpdateRequirement}
              disabled={!editRequirement.title.trim() || isEditing}
            >
              {isEditing ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  저장 중...
                </>
              ) : (
                '저장'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
