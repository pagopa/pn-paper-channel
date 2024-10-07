import { CostsEvent, TenderActiveEvent, TendersEvent } from '../types/schema-request-types';
import { Page, Response } from '../types/model-types';
import { PaperChannelTenderCosts, PaperChannelTender } from '../types/dynamo-types';
import { getActiveTender, getAllTenders } from '../services/tender-service';
import { getCosts } from '../services/cost-service';


export const tendersHandler = async (event: TendersEvent): Promise<Response<Page<PaperChannelTender>>> => {
  console.log("Get all tenders from event ", event);
  const response = await getAllTenders(event.page, event.size, event.from, event.to);
  console.log("Response is ", response);
  return {
    statusCode: 200,
    description: "Get all tenders",
    body: response as Page<PaperChannelTender>,
  }
}

export const tenderActiveHandler = async (event: TenderActiveEvent): Promise<Response<PaperChannelTender>> => {
  console.log("Find active tender event=", event)
  const response = await getActiveTender();
  console.log("Active is ", response);
  return {
    statusCode: response ? 200 : 404,
    description: "Get tender active",
    body: response,
  }
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