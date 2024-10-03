import { AttributeValue } from '@aws-sdk/client-dynamodb';
import { buildPnTendersFromDynamoItems } from '../../../src/utils/builders';

describe("Builders test", () => {

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


})