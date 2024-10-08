import { PaperChannelTenderCosts, PaperChannelTender } from '../types/dynamo-types';
import { CostEvent, CostsEvent, TenderActiveEvent, TendersEvent } from '../types/schema-request-types';
import { Page, Response, ResponseLambda } from '../types/model-types';
import { getActiveTender, getAllTenders } from '../services/tender-service';
import { getCost, getCosts } from '../services/cost-service';

/**
 * Handles the retrieval of all tenders based on the provided event parameters.
 *
 * This asynchronous function logs the incoming event, fetches all tenders using the specified
 * pagination parameters (page, size, from, to), and returns the response formatted as a
 * ResponseLambda object.
 *
 * @param event - An object of type TendersEvent containing pagination and filtering parameters.
 * @returns A Promise that resolves to a Response containing a paginated list of PaperChannelTender objects.
 *
 */
export const tendersHandler = async (event: TendersEvent): Promise<Response<Page<PaperChannelTender>>> => {
  console.log("Get all tenders from event ", event);
  const response = await getAllTenders(event.page, event.size, event.from, event.to);
  console.log("Response is ", response);
  return new ResponseLambda<Page<PaperChannelTender>>().toResponseOK(response);
}

/**
 * Handles the retrieval of the currently active tender.
 *
 * This asynchronous function logs the incoming event, fetches the active tender,
 * and returns the response formatted as a ResponseLambda object.
 *
 * @param event - An object of type TenderActiveEvent that may contain filtering parameters.
 * @returns A Promise that resolves to a Response containing the active PaperChannelTender object.
 *
 * @throws Will throw an NotFoundError if the retrieval of the active tender not found.
 */
export const tenderActiveHandler = async (event: TenderActiveEvent): Promise<Response<PaperChannelTender>> => {
  console.log("Find active tender event=", event)
  const response = await getActiveTender();
  console.log("Active is ", response);
  return new ResponseLambda<PaperChannelTender>().toResponseOK(response);
}

/**
 * Handles the retrieval of cost information based on the incoming event.
 *
 * @param {CostsEvent} event - The event containing information needed to retrieve costs, including
 *                              tender ID, optional filters for product, lot, zone, and delivery driver ID.
 *
 * @returns {Promise<Response<PaperChannelTenderCosts[]>>}
 * - A promise that resolves to a response object containing the status code, description,
 *   and the retrieved cost information.
 *
 * @throws {Error} Throws an error if the underlying `getCosts` function fails to retrieve the data.
 */
export const costHandler = async (event: CostsEvent): Promise<Response<PaperChannelTenderCosts[]>> => {
  console.log("Get cost of tender from event ", event);
  const response = await getCosts(event.tenderId, event.product, event.lot, event.zone, event.deliveryDriverId);
  console.log("Response is ", response);
  return new ResponseLambda<PaperChannelTenderCosts[]>().toResponseOK(response);
}

/**
 * Retrieves the cost information for a specific tender and product.
 *
 * This asynchronous function logs the incoming event, fetches the cost details
 * using the provided tenderId, product, and geokey, and returns the response
 * formatted as a ResponseLambda object containing the cost information.
 *
 * @param event - An object of type CostEvent containing the tender ID, product,
 *                and geokey for which the cost is being requested.
 * @returns A Promise that resolves to a Response containing the cost information
 *          as a PaperChannelTenderCosts object.
 *
 * @throws Will throw an NotFoundError if the cost or geokey not found.
 */
export const singleCostHandler = async (event: CostEvent): Promise<Response<PaperChannelTenderCosts>> => {
  console.log("Get cost from event ", event);
  const response = await getCost(event.tenderId, event.product, event.geokey)
  return new ResponseLambda<PaperChannelTenderCosts>().toResponseOK(response);
}