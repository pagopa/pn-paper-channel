import { Event, TenderActiveEvent, TendersEvent } from '../types/schema-request-types';
import { tenderActiveHandler, tendersHandler } from '../handlers/api-handler';



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
const handlerRoute = async (event: Event) => {
  console.log("Received event validated", event)
  switch (event.operation) {
    case "GET_TENDERS":
      return tendersHandler(event as TendersEvent)
    case 'GET_TENDER_ACTIVE':
      return tenderActiveHandler(event as TenderActiveEvent)
    default:
      throw new Error(`Unknown operation: ${event.operation}`);
  }
}


export default handlerRoute;