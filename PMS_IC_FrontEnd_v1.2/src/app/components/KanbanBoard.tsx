import { useState } from 'react';
import { useDrag, useDrop } from 'react-dnd';
import { User, Clock, AlertTriangle, Flame, Lock, Plus, X } from 'lucide-react';
import { UserRole } from '../App';

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

interface Column {
  id: string;
  title: string;
  tasks: Task[];
}

interface TaskCardProps {
  task: Task;
  moveTask: (taskId: number, toColumn: string) => void;
  canDrag: boolean;
  onEdit: (task: Task) => void;
}

const TaskCard = ({ task, moveTask, canDrag, onEdit }: TaskCardProps) => {
  const [{ isDragging }, drag] = useDrag({
    type: 'task',
    item: { id: task.id },
    canDrag: canDrag,
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  });

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'high':
        return 'bg-red-100 text-red-700 border-red-300';
      case 'medium':
        return 'bg-amber-100 text-amber-700 border-amber-300';
      default:
        return 'bg-green-100 text-green-700 border-green-300';
    }
  };

  return (
    <div
      ref={canDrag ? drag : null}
      onClick={() => canDrag && onEdit(task)}
      className={`bg-white rounded-lg border-2 p-4 transition-all hover:shadow-md ${
        task.isFirefighting ? 'border-red-400 bg-red-50' : 'border-gray-200'
      } ${isDragging ? 'opacity-50' : ''} ${canDrag ? 'cursor-pointer' : 'cursor-default'}`}
    >
      {task.isFirefighting && (
        <div className="flex items-center gap-2 mb-2 text-red-600">
          <Flame size={16} />
          <span className="text-xs font-semibold">긴급 처리</span>
        </div>
      )}

      <h4 className="font-medium text-gray-900 text-sm mb-3">{task.title}</h4>

      <div className="flex flex-wrap gap-1 mb-3">
        {task.labels.map((label, idx) => (
          <span key={idx} className="px-2 py-0.5 bg-blue-100 text-blue-700 rounded text-xs">
            {label}
          </span>
        ))}
      </div>

      <div className="flex items-center justify-between text-xs text-gray-600">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 bg-gradient-to-br from-blue-400 to-blue-600 rounded-full flex items-center justify-center text-white text-xs">
            {task.assignee[0]}
          </div>
          <span>{task.assignee}</span>
        </div>
        <div className="flex items-center gap-3">
          <span className={`px-2 py-0.5 rounded border ${getPriorityColor(task.priority)}`}>
            {task.storyPoints}SP
          </span>
        </div>
      </div>

      <div className="flex items-center gap-1 mt-2 text-xs text-gray-500">
        <Clock size={12} />
        <span>{task.dueDate}</span>
      </div>
    </div>
  );
};

interface ColumnProps {
  column: Column;
  moveTask: (taskId: number, toColumn: string) => void;
  canDrag: boolean;
  onEditTask: (task: Task) => void;
}

const Column = ({ column, moveTask, canDrag, onEditTask }: ColumnProps) => {
  const [{ isOver }, drop] = useDrop({
    accept: 'task',
    drop: (item: { id: number }) => {
      moveTask(item.id, column.id);
    },
    canDrop: () => canDrag,
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  });

  return (
    <div
      ref={drop}
      className={`bg-gray-50 rounded-xl p-4 min-h-[600px] transition-colors ${
        isOver && canDrag ? 'bg-blue-50 ring-2 ring-blue-400' : ''
      }`}
    >
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-gray-900">{column.title}</h3>
        <span className="px-2 py-1 bg-gray-200 text-gray-700 rounded-full text-xs font-medium">
          {column.tasks.length}
        </span>
      </div>
      <div className="space-y-3">
        {column.tasks.map((task) => (
          <TaskCard key={task.id} task={task} moveTask={moveTask} canDrag={canDrag} onEdit={onEditTask} />
        ))}
      </div>
    </div>
  );
};

const initialColumns: Column[] = [
  {
    id: 'backlog',
    title: '제품 백로그',
    tasks: [
      {
        id: 1,
        title: '진단서 이미지 전처리 알고리즘 개선',
        assignee: '박민수',
        priority: 'medium',
        storyPoints: 5,
        dueDate: '2025-08-20',
        labels: ['AI모델링', '데이터처리'],
      },
      {
        id: 2,
        title: '약관 해석 NLP 모델 베이스라인 구축',
        assignee: '김지은',
        priority: 'high',
        storyPoints: 8,
        dueDate: '2025-08-25',
        labels: ['AI모델링', 'NLP'],
      },
    ],
  },
  {
    id: 'sprint',
    title: '이번 스프린트',
    tasks: [
      {
        id: 3,
        title: 'OCR 모델 v2.1 학습 데이터 증강',
        assignee: '이영희',
        priority: 'high',
        storyPoints: 8,
        dueDate: '2025-08-18',
        labels: ['AI모델링', 'OCR'],
      },
      {
        id: 4,
        title: '영수증 항목 분류 모델 정확도 개선',
        assignee: '최지훈',
        priority: 'medium',
        storyPoints: 5,
        dueDate: '2025-08-19',
        labels: ['AI모델링', '분류'],
      },
    ],
  },
  {
    id: 'inProgress',
    title: '진행 중',
    tasks: [
      {
        id: 5,
        title: '특정 병원 진단서 양식 데이터 수집',
        assignee: '박민수',
        priority: 'high',
        storyPoints: 3,
        dueDate: '2025-08-17',
        labels: ['데이터수집'],
        isFirefighting: true,
      },
      {
        id: 6,
        title: '하이퍼파라미터 튜닝 실험 (Learning Rate)',
        assignee: '김지은',
        priority: 'medium',
        storyPoints: 5,
        dueDate: '2025-08-18',
        labels: ['AI모델링', '실험'],
      },
    ],
  },
  {
    id: 'review',
    title: '코드 리뷰',
    tasks: [
      {
        id: 7,
        title: '데이터 파이프라인 리팩토링',
        assignee: '이영희',
        priority: 'low',
        storyPoints: 3,
        dueDate: '2025-08-17',
        labels: ['인프라', '최적화'],
      },
    ],
  },
  {
    id: 'testing',
    title: '테스트 중',
    tasks: [
      {
        id: 8,
        title: 'OCR v2.0 통합 테스트',
        assignee: '최지훈',
        priority: 'high',
        storyPoints: 5,
        dueDate: '2025-08-16',
        labels: ['QA', 'OCR'],
      },
    ],
  },
  {
    id: 'done',
    title: '완료',
    tasks: [
      {
        id: 9,
        title: '모델 성능 지표 대시보드 구축',
        assignee: '박민수',
        priority: 'medium',
        storyPoints: 5,
        dueDate: '2025-08-15',
        labels: ['인프라', '모���터링'],
      },
      {
        id: 10,
        title: '데이터 비식별화 자동화 스크립트',
        assignee: '김지은',
        priority: 'high',
        storyPoints: 8,
        dueDate: '2025-08-14',
        labels: ['데이터처리', '보안'],
      },
    ],
  },
];

export default function KanbanBoard({ userRole }: { userRole: UserRole }) {
  const [columns, setColumns] = useState<Column[]>(initialColumns);
  const [showAddTaskModal, setShowAddTaskModal] = useState(false);
  const [showEditTaskModal, setShowEditTaskModal] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [newTask, setNewTask] = useState({
    title: '',
    assignee: '',
    priority: 'medium' as 'high' | 'medium' | 'low',
    storyPoints: 5,
    dueDate: '',
    labels: '',
    isFirefighting: false,
  });

  const canEdit = ['pm', 'developer', 'qa'].includes(userRole);
  const isReadOnly = ['auditor', 'sponsor'].includes(userRole);

  const moveTask = (taskId: number, toColumnId: string) => {
    if (!canEdit) return;

    setColumns((prevColumns) => {
      let taskToMove: Task | null = null;
      let fromColumnId: string | null = null;

      prevColumns.forEach((column) => {
        const taskIndex = column.tasks.findIndex((t) => t.id === taskId);
        if (taskIndex !== -1) {
          taskToMove = column.tasks[taskIndex];
          fromColumnId = column.id;
        }
      });

      if (!taskToMove || !fromColumnId || fromColumnId === toColumnId) {
        return prevColumns;
      }

      return prevColumns.map((column) => {
        if (column.id === fromColumnId) {
          return {
            ...column,
            tasks: column.tasks.filter((t) => t.id !== taskId),
          };
        } else if (column.id === toColumnId) {
          return {
            ...column,
            tasks: [...column.tasks, taskToMove!],
          };
        }
        return column;
      });
    });
  };

  const handleAddTask = () => {
    if (!newTask.title || !newTask.assignee || !newTask.dueDate) {
      alert('모든 필수 항목을 입력해주세요.');
      return;
    }

    const task: Task = {
      id: Date.now(),
      title: newTask.title,
      assignee: newTask.assignee,
      priority: newTask.priority,
      storyPoints: newTask.storyPoints,
      dueDate: newTask.dueDate,
      labels: newTask.labels.split(',').map((l) => l.trim()).filter(Boolean),
      isFirefighting: newTask.isFirefighting,
    };

    setColumns((prev) =>
      prev.map((col) => {
        if (col.id === 'backlog') {
          return { ...col, tasks: [...col.tasks, task] };
        }
        return col;
      })
    );

    setShowAddTaskModal(false);
    setNewTask({
      title: '',
      assignee: '',
      priority: 'medium',
      storyPoints: 5,
      dueDate: '',
      labels: '',
      isFirefighting: false,
    });
  };

  const handleEditTask = (task: Task) => {
    setEditingTask(task);
    setShowEditTaskModal(true);
  };

  const handleUpdateTask = () => {
    if (!editingTask) return;

    setColumns((prev) =>
      prev.map((col) => ({
        ...col,
        tasks: col.tasks.map((t) => (t.id === editingTask.id ? editingTask : t)),
      }))
    );

    setShowEditTaskModal(false);
    setEditingTask(null);
  };

  const handleDeleteTask = () => {
    if (!editingTask || !confirm('이 작업을 삭제하시겠습니까?')) return;

    setColumns((prev) =>
      prev.map((col) => ({
        ...col,
        tasks: col.tasks.filter((t) => t.id !== editingTask.id),
      }))
    );

    setShowEditTaskModal(false);
    setEditingTask(null);
  };

  const totalTasks = columns.reduce((sum, col) => sum + col.tasks.length, 0);
  const completedTasks = columns.find((col) => col.id === 'done')?.tasks.length || 0;
  const inProgressTasks = columns.find((col) => col.id === 'inProgress')?.tasks.length || 0;
  const firefightingTasks = columns.reduce(
    (sum, col) => sum + col.tasks.filter((t) => t.isFirefighting).length,
    0
  );

  return (
    <div className="p-6">
      {isReadOnly && (
        <div className="mb-6 bg-amber-50 border border-amber-200 rounded-lg p-4 flex items-center gap-3">
          <Lock className="text-amber-600" size={20} />
          <div>
            <p className="text-sm font-medium text-amber-900">읽기 전용 모드</p>
            <p className="text-xs text-amber-700">작업을 조회할 수 있지만 수정할 수 없습니다.</p>
          </div>
        </div>
      )}

      <div className="mb-6">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-2xl font-semibold text-gray-900">스크럼 보드</h2>
            <p className="text-sm text-gray-500 mt-1">Sprint 5 - AI 모델링 단계 (2025.08.05 ~ 2025.08.18)</p>
          </div>
          {canEdit && (
            <button
              onClick={() => setShowAddTaskModal(true)}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
            >
              <Plus size={18} />
              새 작업 추가
            </button>
          )}
        </div>

        {/* Stats */}
        <div className="grid grid-cols-4 gap-4">
          <div className="bg-white rounded-lg border border-gray-200 p-4">
            <p className="text-sm text-gray-500">전체 작업</p>
            <p className="text-2xl font-semibold text-gray-900 mt-1">{totalTasks}</p>
          </div>
          <div className="bg-white rounded-lg border border-gray-200 p-4">
            <p className="text-sm text-gray-500">진행 중</p>
            <p className="text-2xl font-semibold text-blue-600 mt-1">{inProgressTasks}</p>
          </div>
          <div className="bg-white rounded-lg border border-gray-200 p-4">
            <p className="text-sm text-gray-500">완료</p>
            <p className="text-2xl font-semibold text-green-600 mt-1">{completedTasks}</p>
          </div>
          <div className="bg-white rounded-lg border border-gray-200 p-4">
            <div className="flex items-center gap-2">
              <AlertTriangle className="text-red-600" size={16} />
              <p className="text-sm text-gray-500">긴급 이슈</p>
            </div>
            <p className="text-2xl font-semibold text-red-600 mt-1">{firefightingTasks}</p>
          </div>
        </div>
      </div>

      {/* Kanban Board */}
      <div className="grid grid-cols-6 gap-4 overflow-x-auto">
        {columns.map((column) => (
          <Column key={column.id} column={column} moveTask={moveTask} canDrag={canEdit} onEditTask={handleEditTask} />
        ))}
      </div>

      {/* Instructions */}
      <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
        <p className="text-sm text-blue-900">
          💡 <span className="font-medium">사용 방법:</span>{' '}
          {canEdit
            ? '작업 카드를 드래그하여 다른 컬럼으로 이동할 수 있습니다. 긴급 처리가 필요한 작업은 빨간색으로 표시됩니다.'
            : '현재 역할은 조회만 가능합니다.'}
        </p>
      </div>

      {/* Add Task Modal */}
      {showAddTaskModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">새 작업 추가</h3>
              <button
                onClick={() => setShowAddTaskModal(false)}
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
                  value={newTask.title}
                  onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="작업 제목을 입력하세요"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">담당자 *</label>
                  <input
                    type="text"
                    value={newTask.assignee}
                    onChange={(e) => setNewTask({ ...newTask, assignee: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    placeholder="이름"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">마감일 *</label>
                  <input
                    type="date"
                    value={newTask.dueDate}
                    onChange={(e) => setNewTask({ ...newTask, dueDate: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">우선순위</label>
                  <select
                    value={newTask.priority}
                    onChange={(e) =>
                      setNewTask({ ...newTask, priority: e.target.value as 'high' | 'medium' | 'low' })
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
                    value={newTask.storyPoints}
                    onChange={(e) => setNewTask({ ...newTask, storyPoints: parseInt(e.target.value) || 0 })}
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
                  value={newTask.labels}
                  onChange={(e) => setNewTask({ ...newTask, labels: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="쉼표로 구분 (예: AI모델링, OCR)"
                />
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={newTask.isFirefighting}
                  onChange={(e) => setNewTask({ ...newTask, isFirefighting: e.target.checked })}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label className="block text-sm font-medium text-gray-700">긴급 처리</label>
              </div>
            </div>

            <div className="flex gap-3 mt-6">
              <button
                onClick={handleAddTask}
                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                추가
              </button>
              <button
                onClick={() => setShowAddTaskModal(false)}
                className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              >
                취소
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Task Modal */}
      {showEditTaskModal && editingTask && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-lg w-full p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">작업 수정</h3>
              <button
                onClick={() => setShowEditTaskModal(false)}
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
                  value={editingTask.title}
                  onChange={(e) => setEditingTask({ ...editingTask, title: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="작업 제목을 입력하세요"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">담당자 *</label>
                  <input
                    type="text"
                    value={editingTask.assignee}
                    onChange={(e) => setEditingTask({ ...editingTask, assignee: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    placeholder="이름"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">마감일 *</label>
                  <input
                    type="date"
                    value={editingTask.dueDate}
                    onChange={(e) => setEditingTask({ ...editingTask, dueDate: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">우선순위</label>
                  <select
                    value={editingTask.priority}
                    onChange={(e) =>
                      setEditingTask({ ...editingTask, priority: e.target.value as 'high' | 'medium' | 'low' })
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
                    value={editingTask.storyPoints}
                    onChange={(e) => setEditingTask({ ...editingTask, storyPoints: parseInt(e.target.value) || 0 })}
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
                  value={editingTask.labels.join(', ')}
                  onChange={(e) => setEditingTask({ ...editingTask, labels: e.target.value.split(',').map((l) => l.trim()).filter(Boolean) })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="쉼표로 구분 (예: AI모델링, OCR)"
                />
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={editingTask.isFirefighting}
                  onChange={(e) => setEditingTask({ ...editingTask, isFirefighting: e.target.checked })}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label className="block text-sm font-medium text-gray-700">긴급 처리</label>
              </div>
            </div>

            <div className="flex gap-3 mt-6">
              <button
                onClick={handleUpdateTask}
                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                수정
              </button>
              <button
                onClick={handleDeleteTask}
                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
              >
                삭제
              </button>
              <button
                onClick={() => setShowEditTaskModal(false)}
                className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
              >
                취소
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}