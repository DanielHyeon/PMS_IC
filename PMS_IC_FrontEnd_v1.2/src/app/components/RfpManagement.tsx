import { useState, useEffect, useCallback } from 'react';
import {
  FileText,
  Upload,
  Search,
  Filter,
  MoreVertical,
  Eye,
  Trash2,
  RefreshCw,
  CheckCircle,
  XCircle,
  Clock,
  AlertCircle,
  Loader2,
  FileUp,
  Sparkles,
} from 'lucide-react';
import { useProject } from '../../contexts/ProjectContext';
import { Rfp, RfpStatus, ProcessingStatus } from '../../types/project';
import { apiService } from '../../services/api';
import { UserRole } from '../App';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';
import {
  Card,
  CardContent,
  CardDescription,
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

interface RfpManagementProps {
  userRole: UserRole;
}

const statusConfig: Record<RfpStatus, { label: string; color: string; icon: typeof CheckCircle }> = {
  DRAFT: { label: '초안', color: 'bg-gray-100 text-gray-700', icon: FileText },
  SUBMITTED: { label: '제출됨', color: 'bg-blue-100 text-blue-700', icon: Clock },
  UNDER_REVIEW: { label: '검토 중', color: 'bg-yellow-100 text-yellow-700', icon: Eye },
  APPROVED: { label: '승인됨', color: 'bg-green-100 text-green-700', icon: CheckCircle },
  REJECTED: { label: '반려됨', color: 'bg-red-100 text-red-700', icon: XCircle },
};

const processingConfig: Record<ProcessingStatus, { label: string; color: string }> = {
  PENDING: { label: '대기 중', color: 'bg-gray-100 text-gray-600' },
  EXTRACTING: { label: '추출 중', color: 'bg-blue-100 text-blue-600' },
  COMPLETED: { label: '완료', color: 'bg-green-100 text-green-600' },
  FAILED: { label: '실패', color: 'bg-red-100 text-red-600' },
};

export default function RfpManagement({ userRole }: RfpManagementProps) {
  const { currentProject } = useProject();
  const [rfps, setRfps] = useState<Rfp[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<RfpStatus | 'ALL'>('ALL');

  // 다이얼로그 상태
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isViewDialogOpen, setIsViewDialogOpen] = useState(false);
  const [selectedRfp, setSelectedRfp] = useState<Rfp | null>(null);

  // 새 RFP 폼 상태
  const [newRfp, setNewRfp] = useState({
    title: '',
    content: '',
  });
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [isExtracting, setIsExtracting] = useState(false);

  // RFP 목록 로드
  const loadRfps = useCallback(async () => {
    if (!currentProject) return;

    setIsLoading(true);
    try {
      const data = await apiService.getRfps(currentProject.id);
      setRfps(data || []);
    } catch (error) {
      console.error('Failed to load RFPs:', error);
    } finally {
      setIsLoading(false);
    }
  }, [currentProject]);

  useEffect(() => {
    loadRfps();
  }, [loadRfps]);

  // 필터링된 RFP 목록
  const filteredRfps = rfps.filter((rfp) => {
    const matchesSearch =
      rfp.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      rfp.content?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || rfp.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  // RFP 생성
  const handleCreateRfp = async () => {
    if (!currentProject || !newRfp.title.trim()) return;

    setIsCreating(true);
    try {
      if (uploadFile) {
        await apiService.uploadRfpFile(currentProject.id, uploadFile, newRfp.title);
      } else {
        await apiService.createRfp(currentProject.id, {
          title: newRfp.title,
          content: newRfp.content,
          status: 'DRAFT',
          processingStatus: 'PENDING',
        });
      }
      setIsCreateDialogOpen(false);
      setNewRfp({ title: '', content: '' });
      setUploadFile(null);
      await loadRfps();
    } catch (error) {
      console.error('Failed to create RFP:', error);
    } finally {
      setIsCreating(false);
    }
  };

  // 요구사항 추출 시작
  const handleExtractRequirements = async (rfp: Rfp) => {
    if (!currentProject) return;

    setIsExtracting(true);
    try {
      await apiService.extractRequirements(currentProject.id, rfp.id, rfp.content);
      await loadRfps();
    } catch (error) {
      console.error('Failed to extract requirements:', error);
    } finally {
      setIsExtracting(false);
    }
  };

  // RFP 상세 보기
  const handleViewRfp = (rfp: Rfp) => {
    setSelectedRfp(rfp);
    setIsViewDialogOpen(true);
  };

  // 파일 선택 핸들러
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setUploadFile(file);
      if (!newRfp.title) {
        setNewRfp({ ...newRfp, title: file.name.replace(/\.[^/.]+$/, '') });
      }
    }
  };

  // 권한 체크
  const canCreate = ['sponsor', 'pmo_head', 'pm', 'business_analyst', 'admin'].includes(userRole);
  const canExtract = ['pmo_head', 'pm', 'business_analyst', 'admin'].includes(userRole);

  if (!currentProject) {
    return (
      <div className="p-6 text-center text-gray-500">
        <FileText className="h-12 w-12 mx-auto mb-4 text-gray-300" />
        <p>프로젝트를 먼저 선택해주세요.</p>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">RFP 관리</h1>
          <p className="text-gray-500 mt-1">
            프로젝트 제안 요청서(RFP)를 관리하고 요구사항을 추출합니다.
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => setIsCreateDialogOpen(true)}>
            <Upload className="h-4 w-4 mr-2" />
            RFP 등록
          </Button>
        )}
      </div>

      {/* 필터 영역 */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="RFP 검색..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <Select value={statusFilter} onValueChange={(v) => setStatusFilter(v as RfpStatus | 'ALL')}>
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
            <Button variant="outline" onClick={loadRfps} disabled={isLoading}>
              <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* RFP 목록 */}
      {isLoading ? (
        <div className="text-center py-12">
          <Loader2 className="h-8 w-8 animate-spin mx-auto text-blue-500" />
          <p className="text-gray-500 mt-2">로딩 중...</p>
        </div>
      ) : filteredRfps.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <FileText className="h-12 w-12 mx-auto text-gray-300 mb-4" />
            <p className="text-gray-500">
              {searchTerm || statusFilter !== 'ALL'
                ? '검색 결과가 없습니다.'
                : '등록된 RFP가 없습니다.'}
            </p>
            {canCreate && !searchTerm && statusFilter === 'ALL' && (
              <Button
                variant="outline"
                className="mt-4"
                onClick={() => setIsCreateDialogOpen(true)}
              >
                첫 번째 RFP 등록하기
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4">
          {filteredRfps.map((rfp) => {
            const status = statusConfig[rfp.status];
            const processing = processingConfig[rfp.processingStatus];
            const StatusIcon = status.icon;

            return (
              <Card key={rfp.id} className="hover:shadow-md transition-shadow">
                <CardContent className="p-6">
                  <div className="flex items-start justify-between">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="font-semibold text-lg truncate">{rfp.title}</h3>
                        <Badge className={status.color}>
                          <StatusIcon className="h-3 w-3 mr-1" />
                          {status.label}
                        </Badge>
                        {rfp.processingStatus !== 'PENDING' && (
                          <Badge className={processing.color}>
                            {rfp.processingStatus === 'EXTRACTING' && (
                              <Loader2 className="h-3 w-3 mr-1 animate-spin" />
                            )}
                            {processing.label}
                          </Badge>
                        )}
                      </div>
                      <p className="text-gray-500 text-sm line-clamp-2">
                        {rfp.content?.substring(0, 200) || '내용 없음'}
                        {rfp.content && rfp.content.length > 200 && '...'}
                      </p>
                      <div className="flex items-center gap-4 mt-3 text-sm text-gray-400">
                        <span>등록: {new Date(rfp.createdAt).toLocaleDateString('ko-KR')}</span>
                        {rfp.submittedAt && (
                          <span>제출: {new Date(rfp.submittedAt).toLocaleDateString('ko-KR')}</span>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 ml-4">
                      {canExtract && rfp.processingStatus === 'PENDING' && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleExtractRequirements(rfp)}
                          disabled={isExtracting}
                        >
                          <Sparkles className="h-4 w-4 mr-1" />
                          요구사항 추출
                        </Button>
                      )}
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="sm">
                            <MoreVertical className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => handleViewRfp(rfp)}>
                            <Eye className="h-4 w-4 mr-2" />
                            상세 보기
                          </DropdownMenuItem>
                          {canExtract && rfp.processingStatus !== 'EXTRACTING' && (
                            <DropdownMenuItem onClick={() => handleExtractRequirements(rfp)}>
                              <Sparkles className="h-4 w-4 mr-2" />
                              요구사항 추출
                            </DropdownMenuItem>
                          )}
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

      {/* RFP 생성 다이얼로그 */}
      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogHeader>
            <DialogTitle>새 RFP 등록</DialogTitle>
            <DialogDescription>
              RFP 문서를 업로드하거나 직접 내용을 입력하세요.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            {/* 파일 업로드 */}
            <div className="grid gap-2">
              <Label>파일 업로드 (선택사항)</Label>
              <div className="border-2 border-dashed border-gray-200 rounded-lg p-6 text-center hover:border-blue-400 transition-colors">
                <input
                  type="file"
                  accept=".pdf,.doc,.docx,.txt,.hwp"
                  onChange={handleFileChange}
                  className="hidden"
                  id="rfp-file-upload"
                />
                <label htmlFor="rfp-file-upload" className="cursor-pointer">
                  {uploadFile ? (
                    <div className="flex items-center justify-center gap-2 text-blue-600">
                      <FileUp className="h-6 w-6" />
                      <span>{uploadFile.name}</span>
                    </div>
                  ) : (
                    <>
                      <FileUp className="h-8 w-8 mx-auto text-gray-400 mb-2" />
                      <p className="text-sm text-gray-500">
                        클릭하여 파일을 선택하거나 드래그하세요
                      </p>
                      <p className="text-xs text-gray-400 mt-1">
                        PDF, DOC, DOCX, TXT, HWP 지원
                      </p>
                    </>
                  )}
                </label>
              </div>
            </div>

            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <span className="w-full border-t" />
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-white px-2 text-gray-500">또는</span>
              </div>
            </div>

            {/* 수동 입력 */}
            <div className="grid gap-2">
              <Label htmlFor="title">제목 *</Label>
              <Input
                id="title"
                value={newRfp.title}
                onChange={(e) => setNewRfp({ ...newRfp, title: e.target.value })}
                placeholder="RFP 제목을 입력하세요"
              />
            </div>

            <div className="grid gap-2">
              <Label htmlFor="content">내용</Label>
              <Textarea
                id="content"
                value={newRfp.content}
                onChange={(e) => setNewRfp({ ...newRfp, content: e.target.value })}
                placeholder="RFP 내용을 입력하세요..."
                rows={8}
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
              onClick={handleCreateRfp}
              disabled={!newRfp.title.trim() || isCreating}
            >
              {isCreating ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  등록 중...
                </>
              ) : (
                'RFP 등록'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* RFP 상세 다이얼로그 */}
      <Dialog open={isViewDialogOpen} onOpenChange={setIsViewDialogOpen}>
        <DialogContent className="sm:max-w-[700px] max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{selectedRfp?.title}</DialogTitle>
            <DialogDescription>
              RFP 상세 정보
            </DialogDescription>
          </DialogHeader>

          {selectedRfp && (
            <div className="space-y-4">
              <div className="flex gap-2">
                <Badge className={statusConfig[selectedRfp.status].color}>
                  {statusConfig[selectedRfp.status].label}
                </Badge>
                <Badge className={processingConfig[selectedRfp.processingStatus].color}>
                  AI 처리: {processingConfig[selectedRfp.processingStatus].label}
                </Badge>
              </div>

              <div>
                <Label className="text-gray-500">내용</Label>
                <div className="mt-2 p-4 bg-gray-50 rounded-lg whitespace-pre-wrap text-sm">
                  {selectedRfp.content || '내용 없음'}
                </div>
              </div>

              {selectedRfp.processingMessage && (
                <div>
                  <Label className="text-gray-500">처리 메시지</Label>
                  <p className="mt-1 text-sm">{selectedRfp.processingMessage}</p>
                </div>
              )}

              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <Label className="text-gray-500">등록일</Label>
                  <p>{new Date(selectedRfp.createdAt).toLocaleString('ko-KR')}</p>
                </div>
                <div>
                  <Label className="text-gray-500">수정일</Label>
                  <p>{new Date(selectedRfp.updatedAt).toLocaleString('ko-KR')}</p>
                </div>
              </div>
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsViewDialogOpen(false)}>
              닫기
            </Button>
            {canExtract && selectedRfp?.processingStatus === 'PENDING' && (
              <Button onClick={() => selectedRfp && handleExtractRequirements(selectedRfp)}>
                <Sparkles className="h-4 w-4 mr-2" />
                요구사항 추출
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
