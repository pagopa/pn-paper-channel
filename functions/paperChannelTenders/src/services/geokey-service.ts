import { findAllGeokeys } from '../dao/pn-geokey-dao';
import { PaperChannelGeokey } from '../types/dynamo-types';

/**
 * Retrieves cost information for a specific tender, optionally filtered by product, lot, zone, and delivery driver ID.
 *
 * @param {string} tenderId - The unique identifier for the tender.
 * @param {string} product - The name of the product to filter by.
 * @param {string} geokey - The geokey to filter by.
 *
 * @returns {Promise<PaperChannelGeokey[]>}
 * - A promise that resolves to an array of cost information of type `PaperChannelGeokey`.
 *
 * @throws {Error} Throws an error if the underlying `findAllGeokeys` function fails.
 */
export const getGeokeys = async (tenderId: string, product: string, geokey: string): Promise<PaperChannelGeokey[]> => {
  return await findAllGeokeys(tenderId, product, geokey);
}