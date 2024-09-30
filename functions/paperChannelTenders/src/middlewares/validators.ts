import validatorMiddleware from '@middy/validator';
import { transpileSchema } from '@middy/validator/transpile';
import { schemaEventGetTenders } from '../types/schema-request-types';


export const validatorMiddlewareGetTenders = validatorMiddleware({
  eventSchema: transpileSchema(schemaEventGetTenders)
})