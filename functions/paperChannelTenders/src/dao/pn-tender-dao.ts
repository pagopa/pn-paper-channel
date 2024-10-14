import { ScanCommand, ScanInput } from '@aws-sdk/client-dynamodb';
import { PN_TENDER_TABLE_NAME } from '../config';
import { toPageMapper } from '../utils/mappers';
import { PaperChannelTender } from '../types/dynamo-types';
import { Page } from '../types/model-types';
import { dynamoDBClient } from '../utils/awsClients';
import { unmarshall } from '@aws-sdk/util-dynamodb';

/**
 * Retrieves a paginated list of tenders filtered by activation date.
 *
 * @param page - The page number to retrieve (0-based index).
 * @param size - The number of tenders per page.
 * @param from - Optional date to filter tenders activated from this date onwards.
 * @param to - Optional date to filter tenders activated up to this date.
 *
 * @returns A promise that resolves to a paginated response containing the tenders.
 */
export const findTenders = async (
  page: number,
  size: number,
  from?: Date,
  to?: Date
): Promise<Page<PaperChannelTender>> => {
  const defaultFrom = new Date('1970').toISOString();
  const now = new Date().toISOString();

  const scanInput: ScanInput = {
    TableName: PN_TENDER_TABLE_NAME,
    FilterExpression: 'activationDate <= :to AND activationDate >= :from',
    ExpressionAttributeValues: {
      ':from': { S: from ? from.toISOString() : defaultFrom },
      ':to': { S: to ? to.toISOString() : now },
    },
  };

  console.log('Use scan with command ', scanInput);
  const command = new ScanCommand(scanInput);
  const response = await dynamoDBClient.send(command);

  const content = (response.Items || []).map(
    (item) => unmarshall(item) as PaperChannelTender
  );
  const totalElements = response.Count || 0;

  return toPageMapper<PaperChannelTender>(content, totalElements, page, size);
};

/**
 * Retrieves the currently active tender based on the activation date.
 *
 * @returns A promise that resolves to the latest active tender, or throws an error if no tenders are found.
 *
 * @throws {Error} If no active tenders are found.
 */
export const findActiveTender = async (): Promise<PaperChannelTender> => {
  const scanInput = {
    TableName: PN_TENDER_TABLE_NAME,
    FilterExpression: 'activationDate <= :now',
    ExpressionAttributeValues: {
      ':now': {
        S: new Date().toISOString(),
      },
    },
  } as ScanInput;
  const command = new ScanCommand(scanInput);
  const response = await dynamoDBClient.send(command);

  const tenders = (response.Items || []).map(
    (item) => unmarshall(item) as PaperChannelTender
  );

  if (tenders.length == 0) {
    throw new Error('Not found Tenders');
  }

  return tenders.reduce((latest, current) => {
    return new Date(current.activationDate) > new Date(latest!.activationDate)
      ? current
      : latest;
  }, tenders[0] as PaperChannelTender);
};
