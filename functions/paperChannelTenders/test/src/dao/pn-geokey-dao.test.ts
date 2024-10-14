import { PN_GEOKEY_TABLE_NAME } from '../../../src/config';
import { buildGeokeyPartitionKey } from '../../../src/utils/builders';
import { QueryCommand, QueryCommandInput } from '@aws-sdk/client-dynamodb';
import { mockClient } from 'aws-sdk-client-mock';
import { geokeyItem, getGeokey, getItemGeokeyOutput } from '../config/model-mock';
import { findGeokey } from '../../../src/dao/pn-geokey-dao';
import { dynamoDBClient } from '../../../src/utils/awsClients';


describe("Geokey DAO tests", () => {

  const dynamoMockClient = mockClient(dynamoDBClient);

  describe('Find Geokey', () => {

    test('when geokey exists should return entity', async () => {
      // Arrange:
      const tenderId = "12345";
      const product = "AR";
      const geokeyValue = "85039";
      const inputCommand = {
        TableName: PN_GEOKEY_TABLE_NAME,
        FilterExpression: 'tenderProductGeokey = :tenderProductGeokey',
        ExpressionAttributeValues: {
          ":tenderProductGeokey" : {
            "S": buildGeokeyPartitionKey(tenderId, product, geokeyValue)
          }
        }
      } as QueryCommandInput;

      dynamoMockClient.on(QueryCommand, inputCommand).resolves(Promise.resolve(getItemGeokeyOutput));

      // Act
      const result = await findGeokey(tenderId, product, geokeyValue);

      // Assert
      expect(result).toEqual(geokeyItem);
    });

    test('when geokey not exists should return undefined', async () => {
      // Arrange:
      const tenderId = "12345";
      const product = "AR";
      const geokeyValue = "85039";
      const inputCommand = {
        TableName: PN_GEOKEY_TABLE_NAME,
        FilterExpression: 'tenderProductGeokey = :tenderProductGeokey',
        ExpressionAttributeValues: { ":tenderProductGeokey" : { "S": buildGeokeyPartitionKey(tenderId, product, geokeyValue) } }
      } as QueryCommandInput;

      const outputCommand = {
        ...getItemGeokeyOutput,
        Items: undefined
      }

      console.log(inputCommand);

      dynamoMockClient.on(QueryCommand, inputCommand).resolves(Promise.resolve(outputCommand));

      // Act
      const result = await findGeokey(tenderId, product, geokeyValue);

      // Assert
      expect(result).toBeUndefined();
    });

    test('when all geokey are dismissed should return undefined', async () => {
      // Arrange:
      const tenderId = "12345";
      const product = "AR";
      const geokeyValue = "85039";
      const inputCommand = {
        TableName: PN_GEOKEY_TABLE_NAME,
        FilterExpression: 'tenderProductGeokey = :tenderProductGeokey',
        ExpressionAttributeValues: { ":tenderProductGeokey" : { "S": buildGeokeyPartitionKey(tenderId, product, geokeyValue) } }
      } as QueryCommandInput;

      const outputCommand = {
        ...getItemGeokeyOutput,
        Items: [
          getGeokey(true, "2024-09-07T14:30:15.000Z"),
          getGeokey(true, "2024-10-07T14:30:15.000Z"),
        ],
        Counts: 2
      }

      console.log(inputCommand);

      dynamoMockClient.on(QueryCommand, inputCommand).resolves(Promise.resolve(outputCommand));

      // Act
      const result = await findGeokey(tenderId, product, geokeyValue);

      // Assert
      expect(result).toBeUndefined();
    });

  });
  
})