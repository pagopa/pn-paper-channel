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

export const findCosts = async (tenderId: string, product?: string, lot?: string, zone?: string, deliveryDriverId?: string): Promise<PaperChannelTenderCosts[]> => {

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