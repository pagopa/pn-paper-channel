import {
  PaperChannelTenderCosts,
  PaperChannelTender,
  PaperChannelDeliveryDriver,
  PaperChannelGeokey,
} from '../types/dynamo-types';
import {
  CostEvent,
  CostsEvent,
  DeliveryDriversEvent,
  UnifiedDeliveryDriversEvent,
  GeokeyEvent,
  TenderActiveEvent,
  TendersEvent,
} from '../types/schema-request-types';
import { Page, Response, ResponseLambda, PaperChannelUnifiedDeliveryDriver } from '../types/model-types';
import { getActiveTender, getAllTenders } from '../services/tender-service';
import { getCost, getCosts, batchRetrieveCosts } from '../services/cost-service';
import { getAllDeliveryDrivers, retrieveUnifiedDeliveryDrivers } from '../services/deliveryDrivers-service';
import { getGeokeys, getGeokey } from '../services/geokey-service';

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
export const tendersHandler = async (
  event: TendersEvent
): Promise<Response<Page<PaperChannelTender>>> => {
  console.log('Get all tenders from event ', event);
  const response = await getAllTenders(
    event.page,
    event.size,
    event.from,
    event.to
  );
  console.log('Response is ', response);
  return new ResponseLambda<Page<PaperChannelTender>>().toResponseOK(response);
};

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
export const tenderActiveHandler = async (
  event: TenderActiveEvent
): Promise<Response<PaperChannelTender>> => {
  console.log('Find active tender event=', event);
  const response = await getActiveTender();
  console.log('Active is ', response);
  return new ResponseLambda<PaperChannelTender>().toResponseOK(response);
};

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
export const costsHandler = async (
  event: CostsEvent
): Promise<Response<PaperChannelTenderCosts[]>> => {
  console.log('Get cost of tender from event ', event);
  const response = await getCosts(
    event.tenderId,
    event.product,
    event.lot,
    event.zone,
    event.deliveryDriverId
  );
  console.log('Response is ', response);
  return new ResponseLambda<PaperChannelTenderCosts[]>().toResponseOK(response);
};

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
export const costHandler = async (
  event: CostEvent
): Promise<Response<PaperChannelTenderCosts>> => {
  console.log('Get cost from event ', event);
  const response = await getCost(event.tenderId, event.product, event.geokey);
  return new ResponseLambda<PaperChannelTenderCosts>().toResponseOK(response);
};

/**
 * Retrieves the geokey information for a specific tender, product and geokey.
 *
 * This asynchronous function logs the incoming event, fetches the geokey details
 * using the provided tenderId, product, and geokey, and returns the response
 * formatted as a ResponseLambda object containing the cost information.
 *
 * @param event - An object of type GeokeyEvent containing the tenderId, product,
 *                and geokey for which the cost is being requested.
 * @returns A Promise that resolves to a Response containing the cost information
 *          as a PaperChannelGeokey object.
 *
 */
export const geokeyHandler = async (
  event: GeokeyEvent
): Promise<Response<PaperChannelGeokey[]>> => {
  console.log('Get geokey from event ', event);
  const response = await getGeokeys(
    event.tenderId,
    event.product,
    event.geokey
  );
  return new ResponseLambda<PaperChannelGeokey[]>().toResponseOK(response);
};

/**
 * Retrieves all delivery driver information.
 *
 * This asynchronous function logs the incoming event, fetches the delivery driver details
 * and returns the response formatted as a ResponseLambda object containing the information.
 *
 * @param event - An object of type DeliveryDriversEvent.
 * @returns A Promise that resolves to a Response containing the delivery driver information
 *          as a PaperChannelDeliveryDriver object.
 *
 * @throws Will throw an NotFoundError if the delivery driver not found.
 */
export const deliveryDriversHandler = async (
  event: DeliveryDriversEvent
): Promise<Response<PaperChannelDeliveryDriver[]>> => {
  console.log('Get all delivery drivers from event ', event);
  const response = await getAllDeliveryDrivers();
  console.log('Response is ', response);
  return new ResponseLambda<PaperChannelDeliveryDriver[]>().toResponseOK(
    response
  );
};

/**
 * Retrieves unified delivery driver for given geoKeys and product.
 *
 * This asynchronous function logs the incoming event, fetches the unified delivery driver
 * and returns the response formatted as a ResponseLambda object containing the information.
 *
 * @param event - An object of type UnifiedDeliveryDriversRequestEvent.
 * @returns A Promise that resolves to a Response containing the unified delivery driver information
 *          as a PaperChannelUnifiedDeliveryDrivers object.
 *
 * @throws Will throw an NotFoundError if the delivery driver not found.
 */
export const unifiedDeliveryDriversHandler = async (
  event: UnifiedDeliveryDriversEvent
): Promise<Response<PaperChannelUnifiedDeliveryDriver[]>> => {
  console.log('Get unifiedDeliveryDrivers from event', event);

  const geoKeyMap: Record<string, string[]> = {};

  await Promise.all(
    event.requests.map(async req => {
      let geoKey = await getGeokey(event.tenderId, req.product, req.geoKey);
      const key = `${geoKey?.product}#${geoKey?.lot}#${geoKey?.zone}`;
      geoKeyMap[key] = geoKeyMap[key] || [];
      geoKeyMap[key].push(`${geoKey?.geokey}#${geoKey?.product}`);
    })
  );

  const deliveryDriverGeoKeyProductTupleMap: Record<string, string[]> = {}

  const paperCosts = await batchRetrieveCosts(Object.keys(geoKeyMap), event.tenderId);
  for (const cost of paperCosts) {
    const key = `${cost.product}#${cost.lot}#${cost.zone}`;
    const geoKeysForCost = geoKeyMap[key] || [];
    deliveryDriverGeoKeyProductTupleMap[cost.deliveryDriverId] = [
      ...(deliveryDriverGeoKeyProductTupleMap[cost.deliveryDriverId] || []),
      ...geoKeysForCost,
    ];
  }

  const response = await retrieveUnifiedDeliveryDrivers(deliveryDriverGeoKeyProductTupleMap);
  console.log('Response is ', response);
  return new ResponseLambda<PaperChannelUnifiedDeliveryDriver[]>().toResponseOK(response);
};

