import { ScanCommand, ScanInput } from '@aws-sdk/client-dynamodb';
import { buildPnTendersFromDynamoItems, dynamoDBClient } from '../utils/builders';
import { PN_TENDER_TABLE_NAME } from '../config';
import { toPageMapper } from '../utils/mappers';
import { PaperChannelTender } from '../types/dynamo-types';


export const findTenders = async (page: number, size: number, from?: string, to?: string) => {
  let scanInput = {
    TableName: PN_TENDER_TABLE_NAME,
    FilterExpression: "activationDate <= :now",
    ExpressionAttributeValues: {
      ":now": {
        "S": new Date().toDateString()
      }
    }
  } as ScanInput;

  if (from || to) {
    scanInput = {
      TableName: PN_TENDER_TABLE_NAME,
      FilterExpression: "activationDate <= :to AND activationDate >= from",
      ExpressionAttributeValues: {
        ":from": {
          "S": (from) ? from : new Date("1970").toDateString()
        },
        ":to": {
          "S": (to) ? to : new Date().toDateString()
        },
      }
    } as ScanInput
  }
  console.log("Use scan with command ", scanInput);
  const command = new ScanCommand(scanInput);
  const response = await dynamoDBClient.send(command);

  const content = buildPnTendersFromDynamoItems(response.Items || []);
  const totalElements = response.Count || 0

  return toPageMapper<PaperChannelTender>(content, totalElements, page, size);
}


export const findActiveTender = async () => {
  const scanInput = {
    TableName: PN_TENDER_TABLE_NAME,
    FilterExpression: "activationDate <= :now",
    ExpressionAttributeValues: {
      ":now": {
        "S": new Date().toDateString()
      }
    }
  } as ScanInput;
  const command = new ScanCommand(scanInput);
  const response = await dynamoDBClient.send(command);

  const tenders = buildPnTendersFromDynamoItems(response.Items || [])

  if (tenders.length == 0) {
    throw new Error("Not found Tenders");
  }

  tenders.sort((a, b) => {
    const dateA = new Date(a.activationDate);
    const dateB = new Date(b.activationDate);
    return dateB.getTime() - dateA.getTime();
  });

  return tenders[0];
}
