import { AttributeValue, DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { PaperChannelCost, PaperChannelGeokey, PaperChannelTender } from '../types/dynamo-types';
import { unmarshall } from '@aws-sdk/util-dynamodb';


export const dynamoDBClient = new DynamoDBClient();

export const buildGeokeyPartitionKey = (tenderId: string, product: string, geokey: string): string => {
  return [tenderId, product, geokey].join("#");
}

export const buildCostSortKey = (product: string, lot: string, zone: string): string => {
  return [product, lot, zone].join("#");
}

export const buildPnTendersFromDynamoItems = (
  items: Record<string, AttributeValue>[]
): PaperChannelTender[] =>
  items.map(item => unmarshall(item))
  .map(item => item as PaperChannelTender);


export const buildPnGeokeyFromDynamoItems = (
  item: Record<string, AttributeValue>
): PaperChannelGeokey =>
  unmarshall(item) as PaperChannelGeokey;


export const buildPnCostFromDynamoItems = (
  item: Record<string, AttributeValue>
): PaperChannelCost =>
  unmarshall(item) as PaperChannelCost;