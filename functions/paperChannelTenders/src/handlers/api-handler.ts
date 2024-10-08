import { CostsEvent, TenderActiveEvent, TendersEvent } from '../types/schema-request-types';
import { Page, Response } from '../types/model-types';
import { PaperChannelTenderCosts, PaperChannelTender } from '../types/dynamo-types';
import { TenderActiveEvent, TendersEvent } from '../types/schema-request-types';
import { Page, Response, ResponseLambda } from '../types/model-types';
import { PaperChannelTender } from '../types/dynamo-types';
import { getActiveTender, getAllTenders } from '../services/tender-service';
import { getCosts } from '../services/cost-service';

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

export const costHandler = async (event: CostsEvent): Promise<Response<PaperChannelTenderCosts[]>> => {
  console.log("Get cost of tender from event ", event);
  const response = await getCosts(event.tenderId, event.product, event.lot, event.zone, event.deliveryDriverId);
  console.log("Response is ", response);
  return {
    statusCode: 200,
    description: "OK",
    body: response,
  }
}