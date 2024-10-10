import { AttributeValue } from '@aws-sdk/client-dynamodb';
import { PaperChannelTender } from '../types/dynamo-types';
import { unmarshall } from '@aws-sdk/util-dynamodb';


/**
 * Build partition key of PaperChannelGeokey Dynamo object.
 *
 * @param {string} [tenderId] - Identifier of tender.
 * @param {string} [product] - The name of the product.
 * @param {string} [geokey] - The geographical identification.
 * @returns String concat with #.
 */
export const buildGeokeyPartitionKey = (tenderId: string, product: string, geokey: string): string => {
  return [tenderId, product, geokey].join("#");
}


/**
 * Build sort key of PaperChannelTenderCosts Dynamo object.
 *
 * @param {string} [product] - The name of the product.
 * @param {string} [lot] - The lot of geokey.
 * @param {string} [zone] - The geographical zone.
 * @returns String concat with #.
 */
export const buildCostSortKey = (product: string, lot: string, zone: string): string => {
  return [product, lot, zone].join("#");
}
