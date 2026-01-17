import React, { useState, useEffect } from 'react';
import { X, CheckCircle, AlertCircle, AlertTriangle, Info } from 'lucide-react';
import { toastService, Toast, ToastType } from '../../services/toast';

interface ToastItemProps {
  toast: Toast;
  onClose: (id: string) => void;
}

const ToastItem: React.FC<ToastItemProps> = ({ toast, onClose }) => {
  const getIcon = (type: ToastType) => {
    switch (type) {
      case 'success':
        return <CheckCircle className="w-5 h-5 text-emerald-600" />;
      case 'error':
        return <AlertCircle className="w-5 h-5 text-red-600" />;
      case 'warning':
        return <AlertTriangle className="w-5 h-5 text-amber-600" />;
      case 'info':
        return <Info className="w-5 h-5 text-blue-600" />;
    }
  };

  const getStyles = (type: ToastType) => {
    const baseClasses = 'rounded-lg border p-4 shadow-lg';
    switch (type) {
      case 'success':
        return `${baseClasses} bg-emerald-50 border-emerald-200`;
      case 'error':
        return `${baseClasses} bg-red-50 border-red-200`;
      case 'warning':
        return `${baseClasses} bg-amber-50 border-amber-200`;
      case 'info':
        return `${baseClasses} bg-blue-50 border-blue-200`;
    }
  };

  const getTitleColor = (type: ToastType) => {
    switch (type) {
      case 'success':
        return 'text-emerald-900';
      case 'error':
        return 'text-red-900';
      case 'warning':
        return 'text-amber-900';
      case 'info':
        return 'text-blue-900';
    }
  };

  const getMessageColor = (type: ToastType) => {
    switch (type) {
      case 'success':
        return 'text-emerald-700';
      case 'error':
        return 'text-red-700';
      case 'warning':
        return 'text-amber-700';
      case 'info':
        return 'text-blue-700';
    }
  };

  return (
    <div className={`${getStyles(toast.type)} flex items-start gap-3 animate-slide-in`}>
      <div className="flex-shrink-0 pt-0.5">{getIcon(toast.type)}</div>
      <div className="flex-1 min-w-0">
        {toast.title && (
          <p className={`font-semibold ${getTitleColor(toast.type)}`}>
            {toast.title}
          </p>
        )}
        <p className={`text-sm ${getMessageColor(toast.type)} ${toast.title ? 'mt-1' : ''}`}>
          {toast.message}
        </p>
        {toast.action && (
          <button
            onClick={() => {
              toast.action!.onClick();
              onClose(toast.id);
            }}
            className="mt-2 text-sm font-medium underline hover:no-underline"
          >
            {toast.action.label}
          </button>
        )}
      </div>
      <button
        onClick={() => onClose(toast.id)}
        className="flex-shrink-0 text-gray-400 hover:text-gray-600 transition-colors"
      >
        <X className="w-5 h-5" />
      </button>
    </div>
  );
};

const ToastContainer: React.FC = () => {
  const [toasts, setToasts] = useState<Map<string, Toast>>(new Map());

  useEffect(() => {
    const unsubscribe = toastService.subscribe((toast) => {
      setToasts((prev) => new Map(prev).set(toast.id, toast));
    });

    return unsubscribe;
  }, []);

  useEffect(() => {
    const unsubscribe = toastService.subscribeRemove((toastId) => {
      setToasts((prev) => {
        const newToasts = new Map(prev);
        newToasts.delete(toastId);
        return newToasts;
      });
    });

    return unsubscribe;
  }, []);

  const handleClose = (toastId: string) => {
    setToasts((prev) => {
      const newToasts = new Map(prev);
      newToasts.delete(toastId);
      return newToasts;
    });
  };

  return (
    <div className="fixed bottom-4 right-4 z-50 space-y-3 max-w-md">
      {Array.from(toasts.values()).map((toast) => (
        <ToastItem
          key={toast.id}
          toast={toast}
          onClose={handleClose}
        />
      ))}
      <style jsx>{`
        @keyframes slideIn {
          from {
            transform: translateX(400px);
            opacity: 0;
          }
          to {
            transform: translateX(0);
            opacity: 1;
          }
        }
        .animate-slide-in {
          animation: slideIn 0.3s ease-out;
        }
      `}</style>
    </div>
  );
};

export default ToastContainer;
