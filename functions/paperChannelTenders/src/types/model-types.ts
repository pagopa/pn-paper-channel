

export type Page<T> = {
  content: T[],
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  isFirstPage: boolean;
  isLastPage: boolean;
}