import { PaperChannelDeliveryDriver } from '../types/dynamo-types';
import { ScanCommand, ScanInput } from '@aws-sdk/client-dynamodb';
import { PN_DELIVERY_DRIVER_TABLE_NAME } from '../config';
import { buildPnDeliveryDriverFromDynamoItems, dynamoDBClient } from '../utils/builders';


/**
 * Retrieves a paginated list of tenders filtered by activation date.
 *
 * @returns {Promise<PaperChannelDeliveryDriver[]>}
 * - A promise that resolves to an array of delivery driver information of type `PaperChannelDeliveryDriver`.
 *
 */
export const findDeliveryDrivers = async (): Promise<PaperChannelDeliveryDriver[]> => {

  const scanInput: ScanInput = {
    TableName: PN_DELIVERY_DRIVER_TABLE_NAME,
  } as ScanInput;

  console.log("Use scan with command ", scanInput);
  const command = new ScanCommand(scanInput);
  const response = await dynamoDBClient.send(command);

  const drivers = buildPnDeliveryDriverFromDynamoItems(response.Items || []);

  if (drivers.length == 0) {
    throw new Error("Not found delivery driver");
  }

  return drivers
}