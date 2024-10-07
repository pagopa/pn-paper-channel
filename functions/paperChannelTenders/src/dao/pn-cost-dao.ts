import { PN_COST_TABLE_NAME } from '../config';
import {
  buildCostSortKey,
  buildPnCostFromDynamoItems,
  dynamoDBClient,
} from '../utils/builders';
import { GetItemCommand, GetItemCommandInput } from '@aws-sdk/client-dynamodb';
import { PaperChannelCost } from '../types/dynamo-types';


export const findCost = async (tenderId: string, product: string, lot: string, zone: string): Promise<PaperChannelCost | undefined> => {
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