import { Page } from '../types/model-types';


function toPageMapper<T>(
  content: T[],
  totalElements: number,
  pageNumber: number,
  pageSize: number
): Page<T> {
  const totalPages = Math.ceil(totalElements / pageSize);
  return {
    content: content,
    totalElements: totalElements,
    totalPages: totalPages,
    number: pageNumber,
    size: pageSize,
    isFirstPage: pageNumber === 1,
    isLastPage: pageNumber === totalPages,
  }
}

export {
  toPageMapper
}