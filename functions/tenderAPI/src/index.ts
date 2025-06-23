import { validatorEvent } from './middlewares/validators';
import { handleError } from './utils/errors';
import { Event, OperationEnum } from './types/schema-request-types';
import { Response } from './types/model-types'
import {
  costHandler,
  costsHandler,
  deliveryDriversHandler,
  tenderActiveHandler,
  tendersHandler,
  geokeyHandler,
  unifiedDeliveryDriversHandler,
} from './handlers/api-handler';

/**
 * Routes the incoming event to the appropriate handler based on the operation type.
 *
 * This asynchronous function logs the validated event and determines which handler to
 * invoke based on the operation specified in the event. It supports two operations:
 * "GET_TENDERS" and "GET_TENDER_ACTIVE", routing to their respective handlers.
 *
 * @param event - The validated Event object containing the operation type and associated data.
 * @returns A Promise that resolves to the response from the corresponding handler function.
 *
 * @throws Will throw an error if the operation type is not recognized or if the handler
 * function fails.
 */
const handleRoute = async (event: Event) => {
  console.log('Received event validated', event);
  switch (event.operation) {
    case OperationEnum.GET_TENDERS:
      return tendersHandler(event);
    case OperationEnum.GET_TENDER_ACTIVE:
      return tenderActiveHandler(event);
    case OperationEnum.GET_COSTS:
      return costsHandler(event);
    case OperationEnum.GET_COST:
      return costHandler(event);
    case OperationEnum.GET_DELIVERY_DRIVERS:
      return deliveryDriversHandler(event);
    case OperationEnum.GET_GEOKEY:
      return geokeyHandler(event);
    case OperationEnum.GET_UNIFIED_DELIVERY_DRIVERS:
      return unifiedDeliveryDriversHandler(event);
  }
};

/**
 * A handler function that processes an event.
 *
 * This function validates the incoming event using the `validatorEvent` function,
 * and then passes the validated event to the `handlerRoute` function.
 * If any errors occur during validation or processing, they are caught and handled
 * using the `handleError` function.
 *
 * @param event - The event to be processed. The type is unknown until validated.
 * @returns The result of the `handlerRoute` function, or an error response from `handleError`.
 *
 * @throws Will throw an error if the event validation fails or if an error occurs
 * during processing.
 */
export const handler = async (event: unknown): Promise<Response<unknown>> => {
  console.log("Event from AWS Lambda received", event);
  try {
    const eventValidated = validatorEvent(event);
    return await handleRoute(eventValidated);
  } catch (error: Error | unknown) {
    return handleError(error);
  }
};
