import { ResponseError, Status, statusDescription, ValidationField } from './model-types';

export class GenericError extends Error {
  status: Status;

  constructor(message: string, status: Status = Status.BAD_REQUEST) {
    super(message);
    this.status = status;
  }

  toResponse(): ResponseError {
    return {
      body: undefined,
      description: statusDescription[this.status],
      statusCode: this.status,
      errorMessage: this.message
    };
  }
}

export class NotFoundError extends GenericError {
  constructor(message: string) {
    super(message, Status.NOT_FOUND);
  }
}

export class ValidatorError extends GenericError {
  fields: ValidationField[];

  constructor(message: string, fields: ValidationField[]) {
    super(message, Status.BAD_REQUEST);
    this.fields = fields;
  }

  toResponse(): ResponseError {
    return {
      ...super.toResponse(),
      fields: this.fields
    };
  }
}