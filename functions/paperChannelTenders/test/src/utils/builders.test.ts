import { AttributeValue } from '@aws-sdk/client-dynamodb';
import {
  buildCostSortKey,
  buildGeokeyPartitionKey, buildPnCostFromDynamoItems,
  buildPnGeokeyFromDynamoItems,
  buildPnTendersFromDynamoItems,
} from '../../../src/utils/builders';
import { costItem, geokeyItem, getItemCostOutput, getItemGeokeyOutput } from '../config/model-mock';

describe("Builders test", () => {

  test("buildGeokeyPartitionKey_shouldReturnCorrectPartitionKey", () => {
    // Arrange
    const tenderId = "1234";
    const product = "AR";
    const geokey = "86950";

    // Act
    const result = buildGeokeyPartitionKey(tenderId, product, geokey);

    expect(result).toEqual(tenderId+"#"+product+"#"+geokey);
  });

  test("buildCostSortKey_shouldReturnCorrectSortKey", () => {
    // Arrange
    const product = "AR";
    const lot = "LOT1";
    const zone = "EU"

    // Act
    const result = buildCostSortKey(product, lot, zone);

    expect(result).toEqual(product+"#"+lot+"#"+zone);
  })

  test("buildPnTendersFromDynamoItems_shouldReturnCorrectTenders", () => {

   // Arrange
   const tenderRecord: Record<string, AttributeValue> = {
     tenderId: {
       "S": "12344"
     }as AttributeValue,
     activationDate: {
       "S": "2023-01-01"
     } as AttributeValue
   }

   // Act
   const result = buildPnTendersFromDynamoItems([tenderRecord]);

   // Assert
   expect(result).toEqual([{
     tenderId: "12344",
     activationDate: "2023-01-01"
   }]);
  })

  test("buildPnGeokeyFromDynamoItems_shouldReturnCorrectGeokey", () => {
   // Arrange
   const geokeyRecord: Record<string, AttributeValue> = getItemGeokeyOutput!.Item!

   // Act
   const result = buildPnGeokeyFromDynamoItems(geokeyRecord);

   // Assert
   expect(result).toEqual(geokeyItem);
  })

  test("buildPnCostFromDynamoItems_shouldReturnCorrectCost", () => {
    // Arrange
    const costRecord: Record<string, AttributeValue> = getItemCostOutput!.Item!

    // Act
    const result = buildPnCostFromDynamoItems(costRecord);

    // Assert
    expect(result).toEqual(costItem);
  })

})