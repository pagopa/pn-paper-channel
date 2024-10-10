import { mockClient } from 'aws-sdk-client-mock';
import { PN_DELIVERY_DRIVER_TABLE_NAME } from '../../../src/config';
import { ScanCommand, ScanInput } from '@aws-sdk/client-dynamodb';
import { deliveryDriverItem, getDeliveryDriverListOutput } from '../config/model-mock';
import { findDeliveryDrivers } from '../../../src/dao/pn-deliveryDriver-dao';
import { dynamoDBClient } from '../../../src/utils/awsClients';


describe("Delivery Driver DAO tests", () => {
  const dynamoMockClient = mockClient(dynamoDBClient);

  beforeEach(() => {
    dynamoMockClient.reset();
  });

  test('when delivery driver exists return entity', async () => {
    // Arrange
    const scanInputCommand: ScanInput = {
      TableName: PN_DELIVERY_DRIVER_TABLE_NAME,
    } as ScanInput;

    dynamoMockClient.on(ScanCommand, scanInputCommand)
      .resolves(Promise.resolve(getDeliveryDriverListOutput));

    // Act
    const result = await findDeliveryDrivers();

    // Assert
    expect(result[0])
      .toEqual(deliveryDriverItem);
    expect(dynamoMockClient.calls())
      .toHaveLength(1);
  });
});