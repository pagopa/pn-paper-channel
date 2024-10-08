import handlerRoute from './routes/tender-routes';
import { validatorEvent } from './middlewares/validators';
import { handleError } from './middlewares/errors';

/*
{
    "operation": "GET_TENDERS", // SCAN
    "page": "3",
    "size": "2",
    "from": "2024-06-15T11:07:38Z",
    "to": "2024-06-15T11:07:38Z"
}

{
    "operation": "GET_TENDER_ACTIVE"
}

{
    "operation": "GET_COSTS",
    "tenderId": "xxxxxxx", // NECESSARIO
    "product": "xxx",
    "lot": "xxx",
    "zone": "xxx",
    "deliveryDriverId": "xxx"
}

{
    "operation": "GET_COST",
    "tenderId": "xxxxxxx", // NECESSARIO
    "product": "xxx",
    "geokey": "xxx",
}

{
    "operation": "GET_DELIVERY_DRIVERS",
    "from": "xxxxxxx", // NECESSARIO
    "to": "xxx"
}
*/

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
    return handlerRoute(eventValidated)
  } catch (error: unknown) {
    return handleError(error);
  }
}