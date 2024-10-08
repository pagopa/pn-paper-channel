import { AttributeValue, DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { PaperChannelTender } from '../types/dynamo-types';
import { unmarshall } from '@aws-sdk/util-dynamodb';


export const dynamoDBClient = new DynamoDBClient();


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