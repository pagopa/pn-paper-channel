
export type Response<T> = {
  statusCode: 200 | 400 | 404;
  description: string;
  body?: T ;
}

export type Page<T> = {
  content: T[],
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  isFirstPage: boolean;
  isLastPage: boolean;
}