import { useState, useCallback, useMemo } from 'react';

export interface Task {
  id: number;
  title: string;
  assignee: string;
  priority: 'high' | 'medium' | 'low';
  storyPoints: number;
  dueDate: string;
  isFirefighting?: boolean;
  labels: string[];
}

export interface Column {
  id: string;
  title: string;
  tasks: Task[];
}

export interface KanbanStats {
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  firefightingTasks: number;
}

interface UseKanbanBoardReturn {
  columns: Column[];
  stats: KanbanStats;
  moveTask: (taskId: number, toColumnId: string) => void;
  addTask: (task: Task) => void;
  updateTask: (task: Task) => void;
  deleteTask: (taskId: number) => void;
}

/**
 * Kanban board state management hook
 * Encapsulates all column/task operations and statistics calculation
 */
export function useKanbanBoard(initialColumns: Column[], canEdit: boolean): UseKanbanBoardReturn {
  const [columns, setColumns] = useState<Column[]>(initialColumns);

  const moveTask = useCallback(
    (taskId: number, toColumnId: string) => {
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
    },
    [canEdit]
  );

  const addTask = useCallback((task: Task) => {
    setColumns((prev) =>
      prev.map((col) => {
        if (col.id === 'backlog') {
          return { ...col, tasks: [...col.tasks, task] };
        }
        return col;
      })
    );
  }, []);

  const updateTask = useCallback((task: Task) => {
    setColumns((prev) =>
      prev.map((col) => ({
        ...col,
        tasks: col.tasks.map((t) => (t.id === task.id ? task : t)),
      }))
    );
  }, []);

  const deleteTask = useCallback((taskId: number) => {
    setColumns((prev) =>
      prev.map((col) => ({
        ...col,
        tasks: col.tasks.filter((t) => t.id !== taskId),
      }))
    );
  }, []);

  const stats = useMemo<KanbanStats>(() => {
    const totalTasks = columns.reduce((sum, col) => sum + col.tasks.length, 0);
    const completedTasks = columns.find((col) => col.id === 'done')?.tasks.length || 0;
    const inProgressTasks = columns.find((col) => col.id === 'inProgress')?.tasks.length || 0;
    const firefightingTasks = columns.reduce(
      (sum, col) => sum + col.tasks.filter((t) => t.isFirefighting).length,
      0
    );

    return { totalTasks, completedTasks, inProgressTasks, firefightingTasks };
  }, [columns]);

  return {
    columns,
    stats,
    moveTask,
    addTask,
    updateTask,
    deleteTask,
  };
}
