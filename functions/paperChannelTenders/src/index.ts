import { validatorEvent } from './middlewares/validators';
import { handleError } from './utils/errors';
import { CostsEvent, Event, OperationEnum, TenderActiveEvent, TendersEvent } from './types/schema-request-types';
import { costHandler, costsHandler, tenderActiveHandler, tendersHandler } from './handlers/api-handler';


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
  console.log("Received event validated", event)
  switch (event.operation) {
    case OperationEnum.GET_TENDERS:
      return tendersHandler(event as TendersEvent)
    case OperationEnum.GET_TENDER_ACTIVE:
      return tenderActiveHandler(event as TenderActiveEvent)
    case OperationEnum.GET_COSTS:
      return costsHandler(event as CostsEvent)
    case OperationEnum.GET_COST:
      return costHandler(event)
    default:
      throw new Error(`Unknown operation: ${event.operation}`);
  }
}

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
export const handler = (event: unknown) => {
  try {
    const eventValidated = validatorEvent(event)
    return handleRoute(eventValidated)
  } catch (error: Error | unknown) {
    return handleError(error);
  }
}