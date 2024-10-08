import { AttributeValue, DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { PaperChannelTenderCosts, PaperChannelGeokey, PaperChannelTender } from '../types/dynamo-types';
import { unmarshall } from '@aws-sdk/util-dynamodb';


export const dynamoDBClient = new DynamoDBClient();


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

/**
 * Transforms an array of DynamoDB items into an array of PaperChannelTender objects.
 *
 * This function takes in raw DynamoDB items, unmarshall them into JavaScript objects,
 * and casts them to the PaperChannelTender type. It returns an array of the transformed
 * objects.
 *
 * @param items - An array of DynamoDB items represented as records with string keys
 *                and AttributeValue values.
 * @returns An array of PaperChannelTender objects derived from the provided DynamoDB items.
 */
export const buildPnTendersFromDynamoItems = (
  items: Record<string, AttributeValue>[]
): PaperChannelTender[] =>
  items.map(item => unmarshall(item))
  .map(item => item as PaperChannelTender);


/**
 * Transforms an array of DynamoDB items into an array of PaperChannelTenderCosts objects.
 *
 * This function takes in raw DynamoDB items, unmarshall them into JavaScript objects,
 * and casts them to the PaperChannelTenderCosts type. It returns an array of the transformed
 * objects.
 *
 * @param items - An array of DynamoDB items represented as records with string keys
 *                and AttributeValue values.
 * @returns An array of PaperChannelTenderCosts objects derived from the provided DynamoDB items.
 */
export const buildPnCostsTendersFromDynamoItems = (
  items: Record<string, AttributeValue>[]
): PaperChannelTenderCosts[] =>
  items.map(item => buildPnCostFromDynamoItems(item));


/**
 * Transforms an DynamoDB item into an PaperChannelGeokey object.
 *
 * This function takes in raw DynamoDB item, unmarshall them into JavaScript object,
 * and cast them to the PaperChannelGeokey type. It returns an object.
 *
 * @param item - An DynamoDB item represented as records with string keys
 *                and AttributeValue values.
 * @returns PaperChannelGeokey object derived from the provided DynamoDB item.
 */
export const buildPnGeokeyFromDynamoItems = (
  item: Record<string, AttributeValue>
): PaperChannelGeokey =>
  unmarshall(item) as PaperChannelGeokey;


/**
 * Transforms an DynamoDB item into an PaperChannelTenderCosts object.
 *
 * This function takes in raw DynamoDB item, unmarshall them into JavaScript object,
 * and cast them to the PaperChannelTenderCosts type. It returns an object.
 *
 * @param item - An DynamoDB item represented as records with string keys
 *                and AttributeValue values.
 * @returns PaperChannelTenderCosts object derived from the provided DynamoDB item.
 */
export const buildPnCostFromDynamoItems = (
  item: Record<string, AttributeValue>
): PaperChannelTenderCosts =>
  unmarshall(item) as PaperChannelTenderCosts;