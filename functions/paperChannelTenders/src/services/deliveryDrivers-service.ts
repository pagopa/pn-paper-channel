import { PaperChannelDeliveryDriver } from '../types/dynamo-types';
import { findDeliveryDrivers } from '../dao/pn-deliveryDriver-dao';


/**
 * Retrieves a list of all delivery driver.
 *
 * @returns {Promise<PaperChannelDeliveryDriver[]>}
 * - A promise that resolves delivery driver information of type `PaperChannelDeliveryDriver`.
 */
export const getAllDeliveryDrivers = async (): Promise<PaperChannelDeliveryDriver[]> => {
  const response = await findDeliveryDrivers();
  console.log("Get all delivery drivers response ", response);
  return response;
}