
import { GetItemCommand, GetItemCommandInput } from '@aws-sdk/client-dynamodb';
import { PN_GEOKEY_TABLE_NAME } from '../config';
import { buildGeokeyPartitionKey, buildPnGeokeyFromDynamoItems, dynamoDBClient } from '../utils/builders';
import { PaperChannelGeokey } from '../types/dynamo-types';


export const findGeokey = async (tenderId: string, product: string, geokey: string): Promise<PaperChannelGeokey | undefined> => {
  const getCommand = {
    TableName: PN_GEOKEY_TABLE_NAME,
    Key: {
      "tenderProductGeokey" : {
        "S": buildGeokeyPartitionKey(tenderId, product, geokey)
      }
    }
  } as GetItemCommandInput;

  const getItemOutput = await dynamoDBClient.send(new GetItemCommand(getCommand));

  if (!getItemOutput.Item) return undefined;

  return buildPnGeokeyFromDynamoItems(getItemOutput.Item)
}