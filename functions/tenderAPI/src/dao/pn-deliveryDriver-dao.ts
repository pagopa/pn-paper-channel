import { PaperChannelDeliveryDriver } from '../types/dynamo-types';
import { ScanCommand, ScanInput } from '@aws-sdk/client-dynamodb';
import { PN_DELIVERY_DRIVER_TABLE_NAME } from '../config';
import { dynamoDBClient } from '../utils/awsClients';
import { unmarshall } from '@aws-sdk/util-dynamodb';
import { GetItemCommand } from '@aws-sdk/client-dynamodb';
import { GetCommand, DynamoDBDocumentClient} from '@aws-sdk/lib-dynamodb';

/**
 * Retrieves a paginated list of tenders filtered by activation date.
 *
 * @returns {Promise<PaperChannelDeliveryDriver[]>}
 * - A promise that resolves to an array of delivery driver information of type `PaperChannelDeliveryDriver`.
 *
 */
export const findDeliveryDrivers = async (): Promise<
  PaperChannelDeliveryDriver[]
> => {
  const scanInput: ScanInput = {
    TableName: PN_DELIVERY_DRIVER_TABLE_NAME,
  } as ScanInput;

  console.log('Use scan with command ', scanInput);
  const command = new ScanCommand(scanInput);
  const response = await dynamoDBClient.send(command);

  const drivers = (response.Items || []).map(
    (item) => unmarshall(item) as PaperChannelDeliveryDriver
  );

  if (drivers.length == 0) {
    throw new Error('Not found delivery driver');
  }

  return drivers;
};

export const findDeliveryDriverByDriverId = async (
  deliveryDriverId: string
): Promise<PaperChannelDeliveryDriver | null> => {
  const ddbDocClient = DynamoDBDocumentClient.from(dynamoDBClient);
  const command = new GetCommand({
    TableName: PN_DELIVERY_DRIVER_TABLE_NAME,
    Key: {
      deliveryDriverId: deliveryDriverId 
    }
  });

  const response = await ddbDocClient.send(command);

  if (!response.Item) {
    return null;
  }

  return unmarshall(response.Item) as PaperChannelDeliveryDriver;
};
