import { PaperChannelDeliveryDriver } from '../types/dynamo-types';
import { ScanCommand, ScanInput } from '@aws-sdk/client-dynamodb';
import { PN_DELIVERY_DRIVER_TABLE_NAME } from '../config';
import { dynamoDBClient } from '../utils/awsClients';
import { unmarshall } from '@aws-sdk/util-dynamodb';
import { DynamoDBDocumentClient, BatchGetCommand} from '@aws-sdk/lib-dynamodb';

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

export const findDeliveryDriversByDriverIds = async (
  deliveryDriverIds: string[]
): Promise<PaperChannelDeliveryDriver[]> => {
  const docClient = DynamoDBDocumentClient.from(dynamoDBClient);
  const tableName: string = PN_DELIVERY_DRIVER_TABLE_NAME ?? (() => { throw new Error('PN_DELIVERY_DRIVER_TABLE_NAME is undefined'); })();
  const params = {
    RequestItems: {
      [tableName]: {
        Keys: deliveryDriverIds.map(key => (
            {
              deliveryDriverId: key
            }
        ))
      }
    }
  };
  const command = new BatchGetCommand(params);
  return await docClient.send(command).then(response => {
    const items = response?.Responses?.[PN_DELIVERY_DRIVER_TABLE_NAME as string];
    if (!items || items.length === 0) {
      return [];
    }
    return items as PaperChannelDeliveryDriver[];
  });
}
