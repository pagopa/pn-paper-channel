import { Event } from './types/model-types';

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



export const handler = (event: Event) => {}
