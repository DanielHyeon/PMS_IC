/**
 * Toast notification service for displaying temporary messages
 * Supports different types: success, error, warning, info
 */

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: string;
  type: ToastType;
  message: string;
  title?: string;
  duration?: number; // ms, 0 = persistent
  action?: {
    label: string;
    onClick: () => void;
  };
}

type ToastListener = (toast: Toast) => void;
type ToastRemoveListener = (toastId: string) => void;

class ToastService {
  private listeners: Set<ToastListener> = new Set();
  private removeListeners: Set<ToastRemoveListener> = new Set();
  private toasts: Map<string, Toast> = new Map();

  subscribe(listener: ToastListener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  subscribeRemove(listener: ToastRemoveListener): () => void {
    this.removeListeners.add(listener);
    return () => this.removeListeners.delete(listener);
  }

  private notify(toast: Toast) {
    this.toasts.set(toast.id, toast);
    this.listeners.forEach(listener => listener(toast));

    if (toast.duration !== 0) {
      const duration = toast.duration || 5000;
      setTimeout(() => this.remove(toast.id), duration);
    }
  }

  private remove(toastId: string) {
    this.toasts.delete(toastId);
    this.removeListeners.forEach(listener => listener(toastId));
  }

  success(message: string, title?: string, duration?: number) {
    this.notify({
      id: this.generateId(),
      type: 'success',
      message,
      title,
      duration,
    });
  }

  error(message: string, title?: string, duration?: number) {
    this.notify({
      id: this.generateId(),
      type: 'error',
      message,
      title: title || 'Error',
      duration: duration || 8000,
    });
  }

  warning(message: string, title?: string, duration?: number) {
    this.notify({
      id: this.generateId(),
      type: 'warning',
      message,
      title,
      duration: duration || 6000,
    });
  }

  info(message: string, title?: string, duration?: number) {
    this.notify({
      id: this.generateId(),
      type: 'info',
      message,
      title,
      duration,
    });
  }

  wipViolation(column: string, limit: 'soft' | 'hard') {
    const message = `Column "${column}" has exceeded its ${limit} WIP limit`;
    this.warning(message, 'WIP Limit Violation', 0); // Persistent until dismissed
  }

  bottleneckDetected(column: string) {
    const message = `Bottleneck detected in column "${column}"`;
    this.warning(message, 'Bottleneck Alert', 0);
  }

  private generateId(): string {
    return `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}

export const toastService = new ToastService();
