import { CostsEvent, Event, TenderActiveEvent, TendersEvent } from '../types/schema-request-types';
import { costHandler, tenderActiveHandler, tendersHandler } from '../handlers/api-handler';


const handlerRoute = async (event: Event) => {
  console.log("Received event validated", event)
  switch (event.operation) {
    case "GET_TENDERS":
      return tendersHandler(event as TendersEvent)
    case 'GET_TENDER_ACTIVE':
      return tenderActiveHandler(event as TenderActiveEvent)
    case 'GET_COSTS':
      return costHandler(event as CostsEvent)
  }
}


export default handlerRoute;