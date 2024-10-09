import { ResponseError } from '../types/model-types';
import { GenericError, NotFoundError, ValidatorError } from '../types/error-types';



/**
 * Handles various types of errors and converts them into a standardized ResponseError format.
 *
 * This function checks the type of the provided error and responds accordingly:
 * - If the error is a `NotFoundError`, `ValidatorError`, or `GenericError`, it converts the error
 *   to a ResponseError using the `toResponse` method of the respective error class.
 * - If the error is a generic `Error`, it creates a new `GenericError` with the error's message
 *   and converts it to a ResponseError.
 * - For any other types of errors, it returns a default `GenericError` with a generic message.
 *
 * @param error - The error to handle, which can be of any type.
 * @returns A standardized ResponseError representing the handled error.
 *
 * @throws Will return a ResponseError regardless of the error type.
 */
export function handleError(error: Error | unknown): ResponseError {
  if (error instanceof NotFoundError) {
    return error.toResponse();
  } else if (error instanceof ValidatorError) {
    return error.toResponse();
  } else if (error instanceof GenericError) {
    return error.toResponse();
  } else if (error instanceof Error) {
    return new GenericError(error.message).toResponse()
  }
  return new GenericError("An unexpected error occurred").toResponse();
}