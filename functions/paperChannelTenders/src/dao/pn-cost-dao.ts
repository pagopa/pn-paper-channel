import { PN_COST_TABLE_NAME } from '../config';
import {
  buildCostSortKey,
  buildPnCostFromDynamoItems, buildPnCostsTendersFromDynamoItems,
  dynamoDBClient,
} from '../utils/builders';
import {
  AttributeValue,
  GetItemCommand,
  GetItemCommandInput, QueryCommand, QueryInput,
} from '@aws-sdk/client-dynamodb';
import { PaperChannelTenderCosts } from '../types/dynamo-types';



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
export const findCost = async (tenderId: string, product: string, lot: string, zone: string): Promise<PaperChannelTenderCosts | undefined> => {
  const getCommand: GetItemCommandInput = {
    TableName: PN_COST_TABLE_NAME,
    Key: {
      "tenderId": {
        "S": tenderId
      },
      "productLotZone": {
        "S": buildCostSortKey(product, lot, zone)
      }
    }
  };

  const getItemOutput = await dynamoDBClient.send(new GetItemCommand(getCommand));

  if (!getItemOutput.Item) return undefined;

  return buildPnCostFromDynamoItems(getItemOutput.Item);
}


/**
 * Fetches cost information for a specific tender, optionally filtered by product, lot, zone, and delivery driver ID.
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
 */
export const findCosts = async (tenderId: string, product?: string, lot?: string, zone?: string, deliveryDriverId?: string): Promise<PaperChannelTenderCosts[] | undefined> => {

  let expressionValues: Record<string, AttributeValue> = {
    ":tenderId": {
      "S": tenderId
    }
  }

  const filterExpression: string[] = ["tenderId = :tenderId"]

  if(product) {
    filterExpression.push("product = :product")
    expressionValues = {
      ...expressionValues,
      ":product": { "S": product}
    }
  }

  if(lot) {
    filterExpression.push("lot = :lot")
    expressionValues = {
      ...expressionValues,
      ":lot": { "S": lot}
    }
  }

  if(zone) {
    filterExpression.push("zone = :zone")
    expressionValues = {
      ...expressionValues,
      ":zone": { "S": zone}
    }
  }

  if(deliveryDriverId) {
    filterExpression.push("deliveryDriverId = :deliveryDriverId")
    expressionValues = {
      ...expressionValues,
      ":deliveryDriverId": { "S": deliveryDriverId}
    }
  }

  const queryInput = {
    TableName: PN_COST_TABLE_NAME,
    FilterExpression: filterExpression.join(" AND "),
    ExpressionAttributeValues: expressionValues
  } as QueryInput;

  console.log("Used queryInput ", queryInput);
  const command = new QueryCommand(queryInput);
  const response = await dynamoDBClient.send(command);

  return buildPnCostsTendersFromDynamoItems(response.Items || []);
}