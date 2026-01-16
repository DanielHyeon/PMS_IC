import React from 'react';
import { X } from 'lucide-react';

interface Task {
  id: number;
  title: string;
  assignee: string;
  priority: 'high' | 'medium' | 'low';
  storyPoints: number;
  dueDate: string;
  isFirefighting?: boolean;
  labels: string[];
}

interface TaskFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  task?: Task | null;
  onSubmit: (task: Task) => void;
  onDelete?: () => void;
  isEditMode?: boolean;
}

export default function TaskFormModal({
  isOpen,
  onClose,
  task,
  onSubmit,
  onDelete,
  isEditMode = false,
}: TaskFormModalProps) {
  if (!isOpen) return null;

  const [formData, setFormData] = React.useState({
    title: task?.title || '',
    assignee: task?.assignee || '',
    priority: (task?.priority || 'medium') as 'high' | 'medium' | 'low',
    storyPoints: task?.storyPoints || 5,
    dueDate: task?.dueDate || '',
    labels: task?.labels.join(', ') || '',
    isFirefighting: task?.isFirefighting || false,
  });

  React.useEffect(() => {
    if (task) {
      setFormData({
        title: task.title,
        assignee: task.assignee,
        priority: task.priority,
        storyPoints: task.storyPoints,
        dueDate: task.dueDate,
        labels: task.labels.join(', '),
        isFirefighting: task.isFirefighting || false,
      });
    } else {
      setFormData({
        title: '',
        assignee: '',
        priority: 'medium',
        storyPoints: 5,
        dueDate: '',
        labels: '',
        isFirefighting: false,
      });
    }
  }, [task, isOpen]);

  const handleSubmit = () => {
    if (!formData.title || !formData.assignee || !formData.dueDate) {
      alert('모든 필수 항목을 입력해주세요.');
      return;
    }

    const taskData: Task = {
      id: task?.id || Date.now(),
      title: formData.title,
      assignee: formData.assignee,
      priority: formData.priority,
      storyPoints: formData.storyPoints,
      dueDate: formData.dueDate,
      labels: formData.labels.split(',').map((l) => l.trim()).filter(Boolean),
      isFirefighting: formData.isFirefighting,
    };

    onSubmit(taskData);
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">
            {isEditMode ? '작업 수정' : '새 작업 추가'}
          </h3>
          <button
            onClick={onClose}
            className="p-1 hover:bg-gray-100 rounded transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">작업 제목 *</label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="작업 제목을 입력하세요"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">담당자 *</label>
              <input
                type="text"
                value={formData.assignee}
                onChange={(e) => setFormData({ ...formData, assignee: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder="이름"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">마감일 *</label>
              <input
                type="date"
                value={formData.dueDate}
                onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">우선순위</label>
              <select
                value={formData.priority}
                onChange={(e) =>
                  setFormData({ ...formData, priority: e.target.value as 'high' | 'medium' | 'low' })
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="low">낮음</option>
                <option value="medium">보통</option>
                <option value="high">높음</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Story Points</label>
              <input
                type="number"
                value={formData.storyPoints}
                onChange={(e) => setFormData({ ...formData, storyPoints: parseInt(e.target.value) || 0 })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                min="1"
                max="21"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">레이블</label>
            <input
              type="text"
              value={formData.labels}
              onChange={(e) => setFormData({ ...formData, labels: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="쉼표로 구분 (예: AI모델링, OCR)"
            />
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={formData.isFirefighting}
              onChange={(e) => setFormData({ ...formData, isFirefighting: e.target.checked })}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label className="block text-sm font-medium text-gray-700">긴급 처리</label>
          </div>
        </div>

        <div className="flex gap-3 mt-6">
          <button
            onClick={handleSubmit}
            className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            {isEditMode ? '수정' : '추가'}
          </button>
          {isEditMode && onDelete && (
            <button
              onClick={onDelete}
              className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
            >
              삭제
            </button>
          )}
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
          >
            취소
          </button>
        </div>
      </div>
    </div>
  );
}
