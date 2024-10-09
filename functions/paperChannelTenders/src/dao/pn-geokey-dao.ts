import { GetItemCommand, GetItemCommandInput } from '@aws-sdk/client-dynamodb';
import { PN_GEOKEY_TABLE_NAME } from '../config';
import { buildGeokeyPartitionKey, buildPnGeokeyFromDynamoItems } from '../utils/builders';
import { PaperChannelGeokey } from '../types/dynamo-types';
import { dynamoDBClient } from '../utils/awsClients';



/**
 * Retrieves the geokey information for a specific tender and product from the DynamoDB table.
 *
 * @param {string} tenderId - The unique identifier for the tender.
 * @param {string} product - The name of the product associated with the geokey.
 * @param {string} geokey - The specific geokey to be retrieved.
 *
 * @returns {Promise<PaperChannelGeokey | undefined>}
 * - A promise that resolves to the geokey information of type `PaperChannelGeokey` if found,
 * - or `undefined` if no geokey information is found for the given parameters.
 */
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