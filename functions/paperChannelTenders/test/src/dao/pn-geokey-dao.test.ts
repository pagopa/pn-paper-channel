import { PN_GEOKEY_TABLE_NAME } from '../../../src/config';
import { buildGeokeyPartitionKey, dynamoDBClient } from '../../../src/utils/builders';
import { GetItemCommand, GetItemCommandInput } from '@aws-sdk/client-dynamodb';
import { mockClient } from 'aws-sdk-client-mock';
import { geokeyItem, getItemGeokeyOutput } from '../config/model-mock';
import { findGeokey } from '../../../src/dao/pn-geokey-dao';


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
        Key: {
          "tenderProductGeokey" : {
            "S": buildGeokeyPartitionKey(tenderId, product, geokeyValue)
          }
        }
      } as GetItemCommandInput;

      dynamoMockClient.on(GetItemCommand, inputCommand).resolves(Promise.resolve(getItemGeokeyOutput));

      // Act
      const result = await findGeokey(tenderId, product, geokeyValue);

      // Assert
      expect(result).toEqual(geokeyItem);
    })

    test('when geokey not exists should return undefined', async () => {
      // Arrange:
      const tenderId = "12345";
      const product = "AR";
      const geokeyValue = "85039";
      const inputCommand = {
        TableName: PN_GEOKEY_TABLE_NAME,
        Key: {
          "tenderProductGeokey" : {
            "S": buildGeokeyPartitionKey(tenderId, product, geokeyValue)
          }
        }
      } as GetItemCommandInput;

      const outputCommand = {
        ...getItemGeokeyOutput,
        Item: undefined
      }

      dynamoMockClient.on(GetItemCommand, inputCommand).resolves(Promise.resolve(outputCommand));

      // Act
      const result = await findGeokey(tenderId, product, geokeyValue);

      // Assert
      expect(result).toBeUndefined();
    })

  });
  
})