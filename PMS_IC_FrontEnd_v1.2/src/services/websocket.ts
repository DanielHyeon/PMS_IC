/**
 * WebSocket service for real-time WIP updates
 * Handles connection, reconnection, and message dispatch
 */

export type WipUpdateEvent = {
  type: 'COLUMN_UPDATE' | 'SPRINT_UPDATE' | 'BOTTLENECK_DETECTED' | 'WIP_VIOLATION';
  data: {
    columnId?: string;
    columnName?: string;
    currentWip?: number;
    wipLimitSoft?: number;
    wipLimitHard?: number;
    health?: 'GREEN' | 'YELLOW' | 'RED';
    sprintId?: string;
    sprintName?: string;
    conwipLimit?: number;
    conwipPercentage?: number;
    violationType?: 'SOFT_LIMIT' | 'HARD_LIMIT' | 'CONWIP_LIMIT';
    timestamp: number;
  };
};

type MessageListener = (event: WipUpdateEvent) => void;
type StatusListener = (status: 'connecting' | 'connected' | 'disconnected' | 'error') => void;

class WebSocketService {
  private ws: WebSocket | null = null;
  private url: string;
  private projectId: string | null = null;
  private messageListeners: Set<MessageListener> = new Set();
  private statusListeners: Set<StatusListener> = new Set();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;
  private messageQueue: WipUpdateEvent[] = [];
  private heartbeatInterval: NodeJS.Timeout | null = null;

  constructor() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = import.meta.env.VITE_WS_URL || `${protocol}//${window.location.host}`;
    this.url = `${host}/api/wip/subscribe`;
  }

  connect(projectId: string) {
    if (this.ws) return;

    this.projectId = projectId;
    this.notifyStatus('connecting');

    try {
      this.ws = new WebSocket(`${this.url}?projectId=${projectId}`);

      this.ws.onopen = () => {
        console.log('WebSocket connected');
        this.reconnectAttempts = 0;
        this.notifyStatus('connected');
        this.processMessageQueue();
        this.startHeartbeat();
      };

      this.ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data) as WipUpdateEvent;
          this.messageListeners.forEach(listener => listener(message));
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error);
        }
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        this.notifyStatus('error');
      };

      this.ws.onclose = () => {
        console.log('WebSocket disconnected');
        this.ws = null;
        this.stopHeartbeat();
        this.notifyStatus('disconnected');
        this.attemptReconnect();
      };
    } catch (error) {
      console.error('Failed to create WebSocket:', error);
      this.notifyStatus('error');
      this.attemptReconnect();
    }
  }

  disconnect() {
    this.stopHeartbeat();
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.notifyStatus('disconnected');
  }

  subscribe(listener: MessageListener): () => void {
    this.messageListeners.add(listener);
    return () => this.messageListeners.delete(listener);
  }

  subscribeStatus(listener: StatusListener): () => void {
    this.statusListeners.add(listener);
    return () => this.statusListeners.delete(listener);
  }

  send(message: WipUpdateEvent) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      this.messageQueue.push(message);
    }
  }

  private processMessageQueue() {
    while (this.messageQueue.length > 0 && this.ws?.readyState === WebSocket.OPEN) {
      const message = this.messageQueue.shift();
      if (message) {
        this.ws.send(JSON.stringify(message));
      }
    }
  }

  private attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      setTimeout(() => {
        if (this.projectId) {
          this.connect(this.projectId);
        }
      }, this.reconnectDelay * this.reconnectAttempts);
    }
  }

  private startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ type: 'PING' }));
      }
    }, 30000); // Ping every 30 seconds
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  private notifyStatus(status: 'connecting' | 'connected' | 'disconnected' | 'error') {
    this.statusListeners.forEach(listener => listener(status));
  }

  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }
}

export const webSocketService = new WebSocketService();
