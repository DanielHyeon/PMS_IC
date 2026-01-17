/**
 * Pagination service for handling large data sets efficiently
 * Implements cursor-based and offset-based pagination
 */

export interface PaginationOptions {
  pageSize: number;
  currentPage: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface PaginationResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
  hasNextPage: boolean;
  hasPreviousPage: boolean;
}

export interface VirtualizationRange {
  startIndex: number;
  endIndex: number;
  visibleItems: number;
}

class PaginationService {
  /**
   * Paginate array data with sorting
   */
  paginate<T>(
    items: T[],
    options: PaginationOptions,
    compareFn?: (a: T, b: T) => number
  ): PaginationResult<T> {
    let sortedItems = [...items];

    // Sort if compareFn provided
    if (compareFn) {
      sortedItems.sort(compareFn);
    }

    const total = sortedItems.length;
    const pageSize = Math.max(1, options.pageSize);
    const totalPages = Math.ceil(total / pageSize);
    const page = Math.max(1, Math.min(options.currentPage, totalPages));

    const startIndex = (page - 1) * pageSize;
    const endIndex = Math.min(startIndex + pageSize, total);
    const pageItems = sortedItems.slice(startIndex, endIndex);

    return {
      items: pageItems,
      total,
      page,
      pageSize,
      totalPages,
      hasNextPage: page < totalPages,
      hasPreviousPage: page > 1,
    };
  }

  /**
   * Calculate virtualization range for visible items
   * Used for efficient rendering of large lists
   */
  calculateVirtualizationRange(
    itemCount: number,
    itemHeight: number,
    containerHeight: number,
    scrollPosition: number,
    overscan: number = 3
  ): VirtualizationRange {
    const visibleItems = Math.ceil(containerHeight / itemHeight);
    const startIndex = Math.max(
      0,
      Math.floor(scrollPosition / itemHeight) - overscan
    );
    const endIndex = Math.min(
      itemCount,
      Math.ceil((scrollPosition + containerHeight) / itemHeight) + overscan
    );

    return {
      startIndex,
      endIndex,
      visibleItems,
    };
  }

  /**
   * Filter and paginate combined
   */
  filterAndPaginate<T>(
    items: T[],
    filterFn: (item: T) => boolean,
    options: PaginationOptions,
    compareFn?: (a: T, b: T) => number
  ): PaginationResult<T> {
    const filtered = items.filter(filterFn);
    return this.paginate(filtered, options, compareFn);
  }

  /**
   * Get next page
   */
  getNextPage(currentPage: number, totalPages: number): number {
    return Math.min(currentPage + 1, totalPages);
  }

  /**
   * Get previous page
   */
  getPreviousPage(currentPage: number): number {
    return Math.max(currentPage - 1, 1);
  }

  /**
   * Get optimal page size based on container size
   */
  getOptimalPageSize(containerHeight: number, itemHeight: number): number {
    const visibleItems = Math.ceil(containerHeight / itemHeight);
    return Math.max(10, visibleItems * 2); // Buffer for smooth scrolling
  }
}

export const paginationService = new PaginationService();
