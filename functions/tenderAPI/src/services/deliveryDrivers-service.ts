import { PaperChannelDeliveryDriver } from '../types/dynamo-types';
import { findDeliveryDrivers, findDeliveryDriversByDriverIds } from '../dao/pn-deliveryDriver-dao';
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

function chunkArray<T>(requests: T[], size: number): T[][] {
    return Array.from({ length: Math.ceil(requests.length / size) },
    (_, i) => requests.slice(i * size, i * size + size));
}

export const retrieveUnifiedDeliveryDrivers = async (
  deliveryDriverGeoKeyProductTupleMap: Record<string, string[]>
): Promise<PaperChannelUnifiedDeliveryDriver[]> => {
  let response: PaperChannelUnifiedDeliveryDriver[] = [];
  let chunks = chunkArray(Object.keys(deliveryDriverGeoKeyProductTupleMap), 25);
  for (const chunk of chunks) {
      const deliveryDriver = await findDeliveryDriversByDriverIds(chunk);
      if (deliveryDriver.length === 0) {
        throw new NotFoundError(`Delivery driver not found for IDs: ${chunk.join(', ')}`);
      }
      for (const driver of deliveryDriver) {
        if (!driver.unifiedDeliveryDriver) {
          throw new NotFoundError(`Unified delivery driver not found for ID: ${driver.deliveryDriverId}`);
        }
        const geoKeysProduct = deliveryDriverGeoKeyProductTupleMap[driver.deliveryDriverId] || [];
        for (const geoKeyProductTuple of geoKeysProduct) {
            const [geoKey, product] = geoKeyProductTuple.split('#');
            response.push({
              geoKey,
              product,
              unifiedDeliveryDriver: driver.unifiedDeliveryDriver
            } as PaperChannelUnifiedDeliveryDriver);
        }
      }
  }
  console.log('Retrieved unified delivery drivers for given requests');
  return response;
};
