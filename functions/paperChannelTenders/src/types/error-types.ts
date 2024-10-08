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

export class NotFoundError extends Error {
  constructor(message: string) {
    super(message);
  }

  toResponse(): ResponseError {
    return {
      body: undefined,
      description: statusDescription[Status.NOT_FOUND],
      statusCode: Status.NOT_FOUND,
      errorMessage: this.message
    };
  }
}



export class ValidatorError extends Error {
  fields: ValidationField[];

  constructor(message: string, fields: ValidationField[]) {
    super(message);
    this.fields = fields;
  }

  toResponse(): ResponseError {
    return {
      body: undefined,
      statusCode: Status.BAD_REQUEST,
      description: statusDescription[Status.BAD_REQUEST],
      errorMessage: this.message,
      fields: this.fields
    };
  }
}