import { PN_COST_TABLE_NAME } from '../../../src/config';
import { buildCostSortKey, dynamoDBClient } from '../../../src/utils/builders';
import { GetItemCommand, GetItemCommandInput } from '@aws-sdk/client-dynamodb';
import { mockClient } from 'aws-sdk-client-mock';
import { costItem, getItemCostOutput } from '../config/model-mock';
import { findCost } from '../../../src/dao/pn-cost-dao';


describe("Cost DAO tests", () => {

  const dynamoMockClient = mockClient(dynamoDBClient);

  describe('Find Cost', () => {

    test('when cost exists should return entity', async () => {
      // Arrange:
      const tenderId = "12345";
      const product = "AR";
      const lot = "LOT_1";
      const zone = "EU";
      const inputCommand = {
        TableName: PN_COST_TABLE_NAME,
        Key: {
          "tenderId" : {
            "S": tenderId
          },
          "productLotZone": {
            "S": buildCostSortKey(product, lot, zone)
          }
        }
      } as GetItemCommandInput;

      dynamoMockClient.on(GetItemCommand, inputCommand).resolves(Promise.resolve(getItemCostOutput));

      // Act
      const result = await findCost(tenderId, product, lot, zone);

      // Assert
      expect(result).toEqual(costItem);
    })

    test('when cost not exists should return undefined', async () => {
      // Arrange:
      const tenderId = "12345";
      const product = "AR";
      const lot = "LOT_1";
      const zone = "EU";
      const inputCommand = {
        TableName: PN_COST_TABLE_NAME,
        Key: {
          "tenderId" : {
            "S": tenderId
          },
          "productLotZone": {
            "S": buildCostSortKey(product, lot, zone)
          }
        }
      } as GetItemCommandInput;

      const outputCommand = {
        ...getItemCostOutput,
        Item: undefined
      }

      dynamoMockClient.on(GetItemCommand, inputCommand).resolves(Promise.resolve(outputCommand));

      // Act
      const result = await findCost(tenderId, product, lot, zone);

      // Assert
      expect(result).toBeUndefined();
    })

  });
  
})