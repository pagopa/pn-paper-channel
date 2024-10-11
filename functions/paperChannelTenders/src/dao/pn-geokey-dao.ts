import { QueryCommand } from '@aws-sdk/client-dynamodb';
import { PN_GEOKEY_TABLE_NAME } from '../config';
import { buildGeokeyPartitionKey } from '../utils/builders';
import { PaperChannelGeokey } from '../types/dynamo-types';
import { dynamoDBClient, QueryCommandBuilder } from '../utils/awsClients';
import { unmarshall } from '@aws-sdk/util-dynamodb';

/**
 * Retrieves the geokey array information for a specific tender, product and geokey from the DynamoDB table.
 *
 * @param {string} tenderId - The unique identifier for the tender.
 * @param {string} product - The name of the product associated with the geokey.
 * @param {string} geokey - The specific geokey to be retrieved.
 *
 * @returns {Promise<PaperChannelGeokey[]>}
 * - A promise that resolves to the geokey information of type `PaperChannelGeokey` if found,
 * - or `undefined` if no geokey information is found for the given parameters.
 */
export const findAllGeokeys = async (tenderId: string, product: string, geokey: string): Promise<PaperChannelGeokey[]> => {
  const queryCommandBuilder = new QueryCommandBuilder(PN_GEOKEY_TABLE_NAME);
  queryCommandBuilder.addFilter("tenderProductGeokey", buildGeokeyPartitionKey(tenderId, product, geokey))

  console.log(queryCommandBuilder.build())
  const geokeys = await dynamoDBClient.send(new QueryCommand(queryCommandBuilder.build()));

  return (geokeys.Items || []).map(item => unmarshall(item) as PaperChannelGeokey);
}

/**
 * Retrieves the geokey information for a specific tender, product and geokey from the DynamoDB table.
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
  const geokeys = await findAllGeokeys(tenderId, product, geokey);

  return geokeys.filter(item => !item.dismissed).reduce((latest, current) => {
    return new Date(current.activationDate) > new Date(latest!.activationDate) ? current : latest;
  }, geokeys[0] as PaperChannelGeokey);
}
