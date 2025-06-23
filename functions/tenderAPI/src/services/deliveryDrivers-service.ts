import { PaperChannelDeliveryDriver } from '../types/dynamo-types';
import { findDeliveryDrivers, findDeliveryDriverByDriverId } from '../dao/pn-deliveryDriver-dao';
import { getCost } from './cost-service';
import { PaperChannelUnifiedDeliveryDriver } from '../types/model-types';
import { NotFoundError } from '../types/error-types';

/**
 * Retrieves a list of all delivery driver.
 *
 * @returns {Promise<PaperChannelDeliveryDriver[]>}
 * - A promise that resolves delivery driver information of type `PaperChannelDeliveryDriver`.
 */
export const getAllDeliveryDrivers = async (): Promise<
  PaperChannelDeliveryDriver[]
> => {
  const response = await findDeliveryDrivers();
  console.log('Get all delivery drivers response ', response);
  return response;
};

export const retrieveUnifiedDeliveryDriverForGivenRequests = async (
  body: PaperChannelUnifiedDeliveryDriver[],
  tenderId: string
): Promise<PaperChannelUnifiedDeliveryDriver[]> => {
  for (const driver of body) {
    let paperChannelCost = await getCost(tenderId, driver.product, driver.geoKey);
    if( !paperChannelCost) {
      throw new NotFoundError(`Cost not found for tenderId: ${tenderId}, product: ${driver.product}, geokey: ${driver.geoKey}`);
    }
    const deliveryDriver = await findDeliveryDriverByDriverId(paperChannelCost.deliveryDriverId);
    if (!deliveryDriver) {
      throw new NotFoundError(`Delivery driver not found for ID: ${paperChannelCost.deliveryDriverId}`);
    }
    driver.unifiedDeliveryDriver = deliveryDriver.unifiedDeliveryDriver;
  }
  console.log('Retrieved unified delivery drivers for given requests');
  return body;
};
