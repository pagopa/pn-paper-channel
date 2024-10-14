import { Page, ValidationField } from '../types/model-types';
import { ZodError } from 'zod';

/**
 * Maps an array of content into a paginated structure.
 *
 * This function takes a list of items along with pagination parameters and returns
 * a Page object that includes metadata about the pagination, such as the total number
 * of elements, total pages, and whether the current page is the first or last.
 *
 * @param content - An array of items of type T to be included in the page.
 * @param totalElements - The total number of elements available (before pagination).
 * @param pageNumber - The current page number (1-based index).
 * @param pageSize - The number of items per page.
 * @returns A Page<T> object containing the content and pagination metadata.
 *
 * @template T - The type of items contained in the page.
 */
export const toPageMapper = <T>(
  content: T[],
  totalElements: number,
  pageNumber: number,
  pageSize: number
): Page<T> => {
  const totalPages = Math.ceil(totalElements / pageSize);
  return {
    content: content,
    totalElements: totalElements,
    totalPages: totalPages,
    number: pageNumber,
    size: pageSize,
    isFirstPage: pageNumber === 1,
    isLastPage: pageNumber === totalPages,
  };
};

/**
 * Converts a Zod validation error into an array of validation field objects.
 *
 * @param error - The ZodError instance containing validation error details.
 *
 * @returns An array of ValidationField objects, each containing the field ID and error message.
 */
export const zodErrorToValidationField = (
  error: ZodError
): ValidationField[] => {
  return error.errors.map((err) => ({
    fieldId: err.path.join('.'),
    message: err.message,
  }));
};
