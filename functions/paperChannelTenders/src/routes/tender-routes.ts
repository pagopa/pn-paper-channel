import { Event } from '../types/schema-request-types';


const handlerRoute = (event: Event) => {
  return {
    statusCode: 200,
    body: JSON.stringify(event)
  }
}


export default handlerRoute;