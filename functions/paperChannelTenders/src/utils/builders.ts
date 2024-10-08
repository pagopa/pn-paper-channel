import { AttributeValue, DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { PaperChannelTenderCosts, PaperChannelGeokey, PaperChannelTender } from '../types/dynamo-types';
import { unmarshall } from '@aws-sdk/util-dynamodb';


export const dynamoDBClient = new DynamoDBClient();

export const buildGeokeyPartitionKey = (tenderId: string, product: string, geokey: string): string => {
  return [tenderId, product, geokey].join("#");
}

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

export const buildPnCostsTendersFromDynamoItems = (
  items: Record<string, AttributeValue>[]
): PaperChannelTenderCosts[] =>
  items.map(item => buildPnCostFromDynamoItems(item));

export const buildPnGeokeyFromDynamoItems = (
  item: Record<string, AttributeValue>
): PaperChannelGeokey =>
  unmarshall(item) as PaperChannelGeokey;

export const buildPnCostFromDynamoItems = (
  item: Record<string, AttributeValue>
): PaperChannelTenderCosts =>
  unmarshall(item) as PaperChannelTenderCosts;