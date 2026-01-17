/**
 * Performance monitoring and optimization utilities
 * Tracks rendering performance and provides optimization recommendations
 */

interface PerformanceMetric {
  name: string;
  duration: number;
  timestamp: number;
}

interface PerformanceStats {
  totalOperations: number;
  averageDuration: number;
  minDuration: number;
  maxDuration: number;
  totalDuration: number;
}

class PerformanceMonitor {
  private metrics: Map<string, PerformanceMetric[]> = new Map();
  private marks: Map<string, number> = new Map();
  private readonly MAX_METRICS_PER_NAME = 100;

  /**
   * Start measuring a named operation
   */
  start(name: string): void {
    this.marks.set(name, performance.now());
  }

  /**
   * End measurement and record metric
   */
  end(name: string): number {
    const startTime = this.marks.get(name);
    if (!startTime) {
      console.warn(`No start mark found for: ${name}`);
      return 0;
    }

    const duration = performance.now() - startTime;
    this.recordMetric(name, duration);
    this.marks.delete(name);

    return duration;
  }

  /**
   * Record a metric directly
   */
  private recordMetric(name: string, duration: number): void {
    if (!this.metrics.has(name)) {
      this.metrics.set(name, []);
    }

    const metricList = this.metrics.get(name)!;
    metricList.push({
      name,
      duration,
      timestamp: Date.now(),
    });

    // Keep only last N metrics to avoid memory bloat
    if (metricList.length > this.MAX_METRICS_PER_NAME) {
      metricList.shift();
    }
  }

  /**
   * Get statistics for a named metric
   */
  getStats(name: string): PerformanceStats | null {
    const metricList = this.metrics.get(name);
    if (!metricList || metricList.length === 0) {
      return null;
    }

    const durations = metricList.map(m => m.duration);
    const totalDuration = durations.reduce((a, b) => a + b, 0);

    return {
      totalOperations: durations.length,
      averageDuration: totalDuration / durations.length,
      minDuration: Math.min(...durations),
      maxDuration: Math.max(...durations),
      totalDuration,
    };
  }

  /**
   * Get all metrics
   */
  getAllMetrics(): Record<string, PerformanceStats | null> {
    const result: Record<string, PerformanceStats | null> = {};

    this.metrics.forEach((_, name) => {
      result[name] = this.getStats(name);
    });

    return result;
  }

  /**
   * Measure render performance
   */
  measureRender<T>(name: string, fn: () => T): T {
    this.start(name);
    try {
      const result = fn();
      this.end(name);
      return result;
    } catch (error) {
      this.end(name);
      throw error;
    }
  }

  /**
   * Measure async operation
   */
  async measureAsync<T>(name: string, fn: () => Promise<T>): Promise<T> {
    this.start(name);
    try {
      const result = await fn();
      this.end(name);
      return result;
    } catch (error) {
      this.end(name);
      throw error;
    }
  }

  /**
   * Clear metrics for a name
   */
  clear(name: string): void {
    this.metrics.delete(name);
  }

  /**
   * Clear all metrics
   */
  clearAll(): void {
    this.metrics.clear();
    this.marks.clear();
  }

  /**
   * Get slow operations (above threshold)
   */
  getSlowOperations(thresholdMs: number = 100): PerformanceMetric[] {
    const slowOps: PerformanceMetric[] = [];

    this.metrics.forEach(metricList => {
      metricList.forEach(metric => {
        if (metric.duration > thresholdMs) {
          slowOps.push(metric);
        }
      });
    });

    return slowOps.sort((a, b) => b.duration - a.duration);
  }

  /**
   * Log performance summary
   */
  logSummary(): void {
    console.group('Performance Summary');
    const allMetrics = this.getAllMetrics();

    Object.entries(allMetrics).forEach(([name, stats]) => {
      if (stats) {
        console.log(
          `${name}: avg=${stats.averageDuration.toFixed(2)}ms, min=${stats.minDuration.toFixed(2)}ms, max=${stats.maxDuration.toFixed(2)}ms, total=${stats.totalDuration.toFixed(2)}ms`
        );
      }
    });

    console.groupEnd();
  }
}

export const performanceMonitor = new PerformanceMonitor();
