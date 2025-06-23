export enum Status {
  OK = 200,
  NOT_FOUND = 404,
  BAD_REQUEST = 400,
}

export const statusDescription = {
  200: 'OK',
  404: 'NOT_FOUND',
  400: 'BAD_REQUEST',
};

export type Response<T> = {
  statusCode: Status;
  description: string;
  body?: T;
};

export type ValidationField = {
  fieldId: string;
  message: string;
};

export type ResponseError = Response<undefined> & {
  errorMessage: string;
  fields?: ValidationField[];
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  isFirstPage: boolean;
  isLastPage: boolean;
};

export class ResponseLambda<T> {
  toResponseOK(body: T): Response<T> {
    return {
      body: body,
      description: statusDescription[Status.OK],
      statusCode: Status.OK,
    };
  }
}
