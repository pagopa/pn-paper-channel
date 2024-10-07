import { Event, TenderActiveEvent, TendersEvent } from '../types/schema-request-types';
import { tenderActiveHandler, tendersHandler } from '../handlers/api-handler';


const handlerRoute = async (event: Event) => {
  console.log("Received event validated", event)
  switch (event.operation) {
    case "GET_TENDERS":
      return tendersHandler(event as TendersEvent)
    case 'GET_TENDER_ACTIVE':
      return tenderActiveHandler(event as TenderActiveEvent)
  }
}


export default handlerRoute;