import { PN_COST_TABLE_NAME } from '../config';
import { buildCostSortKey } from '../utils/builders';
import {
  GetItemCommand,
  GetItemCommandInput,
  QueryCommand,
} from '@aws-sdk/client-dynamodb';
import { PaperChannelTenderCosts } from '../types/dynamo-types';
import { dynamoDBClient, QueryCommandBuilder } from '../utils/awsClients';
import { unmarshall } from '@aws-sdk/util-dynamodb';

/**
 * Fetches the cost information for a specific tender, product, lot, and zone from the DynamoDB table.
 *
 * @param {string} tenderId - The unique identifier for the tender.
 * @param {string} product - The name of the product.
 * @param {string} lot - The lot number associated with the product.
 * @param {string} zone - The geographical zone for which the cost is being queried.
 *
 * @returns {Promise<PaperChannelTenderCosts | undefined>}
 * - A promise that resolves to the cost information of type `PaperChannelTenderCosts` if found,
 * - or `undefined` if no cost information is found for the given parameters.
 */
export const findCost = async (
  tenderId: string,
  product: string,
  lot: string,
  zone: string
): Promise<PaperChannelTenderCosts | undefined> => {
  const getCommand: GetItemCommandInput = {
    TableName: PN_COST_TABLE_NAME,
    Key: {
      tenderId: {
        S: tenderId,
      },
      productLotZone: {
        S: buildCostSortKey(product, lot, zone),
      },
    },
  };

  const getItemOutput = await dynamoDBClient.send(
    new GetItemCommand(getCommand)
  );

  if (!getItemOutput.Item) return undefined;

  return unmarshall(getItemOutput.Item) as PaperChannelTenderCosts;
};

/**
 * Fetches cost information for a specific tender, optionally filtered by product, lot, zone, and delivery driver ID.
 *
 * @param {string} tenderId - The unique identifier for the tender.
 * @param {string} [product] - The optional name of the product to filter by.
 * @param {string} [lot] - The optional lot number to filter by.
 * @param {string} [zone] - The optional geographical zone to filter by.
 * @param {string} [deliveryDriverId] - The optional delivery driver ID to filter by.
 *
 * @returns {Promise<PaperChannelTenderCosts[]>}
 * - A promise that resolves to an array of cost information of type `PaperChannelTenderCosts` if found,
 * - or `undefined` if no cost information is found for the given parameters.
 */
export const findCosts = async (
  tenderId: string,
  product?: string,
  lot?: string,
  zone?: string,
  deliveryDriverId?: string
): Promise<PaperChannelTenderCosts[]> => {
  const queryCommandBuilder = new QueryCommandBuilder(PN_COST_TABLE_NAME);
  queryCommandBuilder.addKeyCondition('tenderId', tenderId)
  const queryInput = queryCommandBuilder.build();

  console.log('Used queryInput ', queryInput);
  const command = new QueryCommand(queryInput);
  const response = await dynamoDBClient.send(command);

  const filter: {[key:string]: string | undefined} = {
    product,
    lot,
    zone,
    deliveryDriverId
  }

  if (!response.Items || response.Items.length === 0) {
    throw new Error('Invalid tenderId');
  }

  return (response.Items).filter(item => {
    return Object.keys(filter).every(key => !filter[key] || (item[key] && item[key].S === filter[key]))
  }).map(item =>
    unmarshall(item) as PaperChannelTenderCosts
  );
};
