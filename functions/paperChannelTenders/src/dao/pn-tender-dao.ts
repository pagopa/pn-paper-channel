import { ScanCommand, ScanInput } from '@aws-sdk/client-dynamodb';
import { buildPnTendersFromDynamoItems, dynamoDBClient } from '../utils/builders';
import { PN_TENDER_TABLE_NAME } from '../config';
import { toPageMapper } from '../utils/mappers';
import { PaperChannelTender } from '../types/dynamo-types';
import { Page } from '../types/model-types';


export const findTenders = async (page: number, size: number, from?: Date, to?: Date): Promise<Page<PaperChannelTender>> => {
  const defaultFrom = new Date("1970").toISOString();
  const now = new Date().toISOString();

  const scanInput: ScanInput = {
    TableName: PN_TENDER_TABLE_NAME,
    FilterExpression: "activationDate <= :to AND activationDate >= :from",
    ExpressionAttributeValues: {
      ":from": { S: from ? from.toISOString() : defaultFrom },
      ":to": { S: to ? to.toISOString() : now },
    },
  };

  console.log("Use scan with command ", scanInput);
  const command = new ScanCommand(scanInput);
  const response = await dynamoDBClient.send(command);

  const content = buildPnTendersFromDynamoItems(response.Items || []);
  const totalElements = response.Count || 0

  return toPageMapper<PaperChannelTender>(content, totalElements, page, size);
}


export const findActiveTender = async () : Promise<PaperChannelTender | undefined> => {
  const scanInput = {
    TableName: PN_TENDER_TABLE_NAME,
    FilterExpression: "activationDate <= :now",
    ExpressionAttributeValues: {
      ":now": {
        "S": new Date().toISOString()
      }
    }
  } as ScanInput;
  const command = new ScanCommand(scanInput);
  const response = await dynamoDBClient.send(command);

  const tenders = buildPnTendersFromDynamoItems(response.Items || [])

  if (tenders.length == 0) {
    throw new Error("Not found Tenders");
  }

  const latestTender = tenders.reduce((latest, current) => {
    return new Date(current.activationDate) > new Date(latest!.activationDate) ? current : latest;
  }, tenders[0]);

  return latestTender;
}
