import { PaperChannelTenderCosts } from '../types/dynamo-types';
import { findCost, findCosts } from '../dao/pn-cost-dao';
import { findGeokey } from '../dao/pn-geokey-dao';
import { NotFoundError } from '../types/error-types';


/**
 * Retrieves cost information for a specific tender, optionally filtered by product, lot, zone, and delivery driver ID.
 *
 * @param {string} tenderId - The unique identifier for the tender.
 * @param {string} [product] - The optional name of the product to filter by.
 * @param {string} [lot] - The optional lot number to filter by.
 * @param {string} [zone] - The optional geographical zone to filter by.
 * @param {string} [deliveryDriverId] - The optional delivery driver ID to filter by.
 *
 * @returns {Promise<PaperChannelTenderCosts[] | undefined>}
 * - A promise that resolves to an array of cost information of type `PaperChannelTenderCosts` if found,
 * - or `undefined` if no cost information is found for the given parameters.
 *
 * @throws {Error} Throws an error if the underlying `findCosts` function fails.
 */
export const getCosts = async (tenderId: string, product?: string, lot?: string, zone?: string, deliveryDriverId?: string): Promise<PaperChannelTenderCosts[]> => {
  return await findCosts(tenderId, product, lot, zone, deliveryDriverId);
}

/**
 * Retrieves the cost information for a specific tender, product, and geokey.
 *
 * @param {string} tenderId - The unique identifier for the tender.
 * @param {string} product - The name of the product associated with the cost.
 * @param {string} geokey - The specific geokey to be used in the cost retrieval.
 *
 * @returns {Promise<PaperChannelTenderCosts>}
 * - A promise that resolves to the cost information of type `PaperChannelTenderCosts`.
 *
 * @throws {NotFoundError}
 * - Throws a `NotFoundError` if the geokey entity or cost entity cannot be found.
 */
export const getCost = async (tenderId: string, product: string, geokey: string): Promise<PaperChannelTenderCosts> => {
  console.log("Get cost from ", tenderId, " - ", product, " - ", geokey);
  const geokeyEntity = await findGeokey(tenderId, product, geokey);
  if (!geokeyEntity) {
    throw new NotFoundError("Geokey entity not found")
  }
  console.log("Geokey retrieved : ", geokeyEntity.lot, " - ", geokeyEntity.zone);
  const costEntity = await findCost(tenderId, product, geokeyEntity.lot, geokeyEntity.zone);
  if (!costEntity) {
    throw new NotFoundError("Cost entity not found")
  }
  console.log("Cost retrieved : ", costEntity)
  return costEntity;
}