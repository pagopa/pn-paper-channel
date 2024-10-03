import { dynamoDBClient } from '../../../src/utils/builders';
import { AttributeValue, ScanCommand, ScanInput } from '@aws-sdk/client-dynamodb';
import { findTenders } from '../../../src/dao/pn-tender-dao';
import { PaperChannelTender } from '../../../src/types/dynamo-types';
import { Page } from '../../../src/types/model-types';
import { mockClient } from 'aws-sdk-client-mock';
import { PN_TENDER_TABLE_NAME } from '../../../src/config';

describe('findTenders', () => {

  const dynamoMockClient = mockClient(dynamoDBClient);

  test('should return paged results when no date filters are provided', async () => {
    // Arrange
    const mockResponse = {
      Items: [{
        tenderId: {
          "S": "12344"
        } as AttributeValue,
        activationDate: {
          "S": "2023-01-01"
        } as AttributeValue
      }],
      Count: 1,
    };

    const scanInput = {
      TableName: PN_TENDER_TABLE_NAME,
      FilterExpression: "activationDate <= :now",
      ExpressionAttributeValues: {
        ":now": {
          "S": new Date().toDateString()
        }
      }
    } as ScanInput

    dynamoMockClient.on(ScanCommand, scanInput).resolves(Promise.resolve(mockResponse))

    // Act
    const result = await findTenders(1, 10);

    // Assert
    expect(result).toEqual({
      content: [{
        tenderId: "12344",
        activationDate: "2023-01-01"
      }],
      isFirstPage: true,
      isLastPage: true,
      totalPages: 1,
      number: 1,
      size: 10,
      totalElements: 1
    } as Page<PaperChannelTender>);
  });

});
