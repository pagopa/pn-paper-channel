import { GenericError, NotFoundError, ValidatorError } from '../../../src/types/error-types';
import { handleError } from '../../../src/middlewares/errors';
import { ValidationField } from '../../../src/types/model-types';


describe("Errors middleware tests", () => {

  test("when throw NotFoundError should return ResponseError", () => {
    // Arrange
    const message = "Tenders not found.";
    const error = new NotFoundError(message);

    // Act
    const response = handleError(error);

    // Assert
    expect(response).toEqual({
      body: undefined,
      description: "NOT_FOUND",
      statusCode: 404,
      errorMessage: message
    })
  });

  test("when throw ValidationError should return ResponseError", () => {
    // Arrange
    const message = "Validation error.";
    const fields: ValidationField[] = [
      {
        fieldId: "tenderId",
        message: "Field is required"
      }
    ]
    const error = new ValidatorError(message, fields);

    // Act
    const response = handleError(error);

    // Assert
    expect(response).toEqual({
      body: undefined,
      description: "BAD_REQUEST",
      statusCode: 400,
      errorMessage: message,
      fields: fields
    })
  });

  test("when throw GenericError should return ResponseError", () => {
    // Arrange
    const message = "Generic error.";
    const error = new GenericError(message);

    // Act
    const response = handleError(error);

    // Assert
    expect(response).toEqual({
      body: undefined,
      description: "BAD_REQUEST",
      statusCode: 400,
      errorMessage: message,
    })
  });

  test("when throw Error should return ResponseError", () => {
    // Arrange
    const message = "Generic error.";
    const error = Error(message);

    // Act
    const response = handleError(error);

    // Assert
    expect(response).toEqual({
      body: undefined,
      description: "BAD_REQUEST",
      statusCode: 400,
      errorMessage: message,
    })
  });

  test("when throw unknown should return ResponseError", () => {
    // Arrange
    const message = "Generic error.";

    // Act
    const response = handleError(message);

    // Assert
    expect(response).toEqual({
      body: undefined,
      description: "BAD_REQUEST",
      statusCode: 400,
      errorMessage: "An unexpected error occurred",
    })
  });

});