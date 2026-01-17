/**
 * Export service for generating PDF and Excel reports
 * Uses jsPDF for PDF and xlsx for Excel
 */

export interface ProgressItem {
  id: string;
  title: string;
  progressPercentage: number;
  progressStage: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'DELAYED';
  dueDate?: string;
  completedTasks: number;
  totalTasks: number;
  storyPointsCompleted?: number;
  storyPointsPlanned?: number;
  estimatedHours?: number;
  actualHours?: number;
  assignee?: string;
}

export interface WipStatusData {
  projectId: string;
  totalWip: number;
  columnStatuses: Array<{
    columnId: string;
    columnName: string;
    currentWip: number;
    wipLimitSoft?: number;
    wipLimitHard?: number;
    health: 'RED' | 'YELLOW' | 'GREEN';
  }>;
  bottleneckCount: number;
}

export class ExportService {
  /**
   * Export progress data to CSV (compatible with Excel)
   */
  async exportProgressToCsv(items: ProgressItem[], filename: string = 'progress.csv'): Promise<void> {
    const headers = [
      'ID',
      'Title',
      'Progress %',
      'Stage',
      'Completed Tasks',
      'Total Tasks',
      'Story Points Completed',
      'Story Points Planned',
      'Estimated Hours',
      'Actual Hours',
      'Assignee',
      'Due Date',
    ];

    const rows = items.map(item => [
      item.id,
      item.title,
      item.progressPercentage,
      item.progressStage,
      item.completedTasks,
      item.totalTasks,
      item.storyPointsCompleted || '',
      item.storyPointsPlanned || '',
      item.estimatedHours || '',
      item.actualHours || '',
      item.assignee || '',
      item.dueDate || '',
    ]);

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => this.escapeCsvCell(cell)).join(',')),
    ].join('\n');

    this.downloadFile(csvContent, filename, 'text/csv');
  }

  /**
   * Export WIP status data to CSV
   */
  async exportWipStatusToCsv(data: WipStatusData, filename: string = 'wip-status.csv'): Promise<void> {
    const headers = ['Column Name', 'Current WIP', 'Soft Limit', 'Hard Limit', 'Health', 'Soft Limit %', 'Hard Limit %'];

    const rows = data.columnStatuses.map(col => {
      const softPercentage = col.wipLimitSoft ? Math.round((col.currentWip / col.wipLimitSoft) * 100) : 'N/A';
      const hardPercentage = col.wipLimitHard ? Math.round((col.currentWip / col.wipLimitHard) * 100) : 'N/A';

      return [
        col.columnName,
        col.currentWip,
        col.wipLimitSoft || 'Unlimited',
        col.wipLimitHard || 'Unlimited',
        col.health,
        softPercentage,
        hardPercentage,
      ];
    });

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => this.escapeCsvCell(cell)).join(',')),
      '',
      ['Project Summary'],
      ['Total WIP', data.totalWip],
      ['Total Columns', data.columnStatuses.length],
      ['Bottleneck Columns', data.bottleneckCount],
    ].join('\n');

    this.downloadFile(csvContent, filename, 'text/csv');
  }

  /**
   * Export to JSON format
   */
  async exportProgressToJson(
    items: ProgressItem[],
    filename: string = 'progress.json'
  ): Promise<void> {
    const json = JSON.stringify(
      {
        exported: new Date().toISOString(),
        totalItems: items.length,
        items,
      },
      null,
      2
    );

    this.downloadFile(json, filename, 'application/json');
  }

  /**
   * Export WIP status to JSON
   */
  async exportWipStatusToJson(
    data: WipStatusData,
    filename: string = 'wip-status.json'
  ): Promise<void> {
    const json = JSON.stringify(
      {
        exported: new Date().toISOString(),
        ...data,
      },
      null,
      2
    );

    this.downloadFile(json, filename, 'application/json');
  }

  /**
   * Generate HTML table for printing
   */
  generateProgressHtmlTable(items: ProgressItem[]): string {
    const tableRows = items
      .map(item => {
        const stageColor = this.getStageColor(item.progressStage);
        return `
        <tr>
          <td style="padding: 8px; border: 1px solid #ddd;">${item.id}</td>
          <td style="padding: 8px; border: 1px solid #ddd;">${item.title}</td>
          <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${item.progressPercentage}%</td>
          <td style="padding: 8px; border: 1px solid #ddd; background-color: ${stageColor};">${item.progressStage}</td>
          <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${item.completedTasks}/${item.totalTasks}</td>
          <td style="padding: 8px; border: 1px solid #ddd;">${item.assignee || 'Unassigned'}</td>
          <td style="padding: 8px; border: 1px solid #ddd;">${item.dueDate || 'N/A'}</td>
        </tr>
      `;
      })
      .join('');

    return `
      <table style="border-collapse: collapse; width: 100%;">
        <thead>
          <tr style="background-color: #f3f4f6;">
            <th style="padding: 8px; border: 1px solid #ddd; text-align: left;">ID</th>
            <th style="padding: 8px; border: 1px solid #ddd; text-align: left;">Title</th>
            <th style="padding: 8px; border: 1px solid #ddd; text-align: left;">Progress</th>
            <th style="padding: 8px; border: 1px solid #ddd; text-align: left;">Stage</th>
            <th style="padding: 8px; border: 1px solid #ddd; text-align: left;">Tasks</th>
            <th style="padding: 8px; border: 1px solid #ddd; text-align: left;">Assignee</th>
            <th style="padding: 8px; border: 1px solid #ddd; text-align: left;">Due Date</th>
          </tr>
        </thead>
        <tbody>
          ${tableRows}
        </tbody>
      </table>
    `;
  }

  /**
   * Print progress report
   */
  printProgressReport(items: ProgressItem[], title: string = 'Progress Report'): void {
    const htmlContent = `
      <!DOCTYPE html>
      <html>
        <head>
          <title>${title}</title>
          <style>
            body { font-family: Arial, sans-serif; margin: 20px; }
            h1 { color: #1f2937; }
            .summary { margin: 20px 0; padding: 10px; background-color: #f3f4f6; border-radius: 4px; }
            .summary-item { display: inline-block; margin-right: 30px; }
          </style>
        </head>
        <body>
          <h1>${title}</h1>
          <p>Generated: ${new Date().toLocaleString()}</p>
          <div class="summary">
            <div class="summary-item"><strong>Total Items:</strong> ${items.length}</div>
            <div class="summary-item"><strong>Completed:</strong> ${items.filter(i => i.progressStage === 'COMPLETED').length}</div>
            <div class="summary-item"><strong>In Progress:</strong> ${items.filter(i => i.progressStage === 'IN_PROGRESS').length}</div>
            <div class="summary-item"><strong>Delayed:</strong> ${items.filter(i => i.progressStage === 'DELAYED').length}</div>
          </div>
          ${this.generateProgressHtmlTable(items)}
        </body>
      </html>
    `;

    const printWindow = window.open('', '', 'width=1200,height=800');
    if (printWindow) {
      printWindow.document.write(htmlContent);
      printWindow.document.close();
      printWindow.print();
    }
  }

  /**
   * Download file helper
   */
  private downloadFile(content: string, filename: string, mimeType: string): void {
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Escape CSV cell content
   */
  private escapeCsvCell(cell: any): string {
    if (cell === null || cell === undefined) return '';
    const cellStr = String(cell);
    if (cellStr.includes(',') || cellStr.includes('"') || cellStr.includes('\n')) {
      return `"${cellStr.replace(/"/g, '""')}"`;
    }
    return cellStr;
  }

  /**
   * Get stage color for HTML
   */
  private getStageColor(stage: string): string {
    switch (stage) {
      case 'COMPLETED':
        return '#d1fae5'; // emerald-100
      case 'IN_PROGRESS':
        return '#bfdbfe'; // blue-100
      case 'DELAYED':
        return '#fee2e2'; // red-100
      case 'NOT_STARTED':
        return '#f3f4f6'; // gray-100
      default:
        return '#ffffff';
    }
  }
}

export const exportService = new ExportService();
