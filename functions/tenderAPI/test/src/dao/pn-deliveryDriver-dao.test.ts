import { mockClient } from 'aws-sdk-client-mock';
import { PN_DELIVERY_DRIVER_TABLE_NAME } from '../../../src/config';
import { ScanCommand, ScanInput, GetItemCommand, GetItemCommandInput } from '@aws-sdk/client-dynamodb';
import {
  deliveryDriverItem,
  getDeliveryDriverListOutput,
  getDeliveryDriverItem,
} from '../config/model-mock';
import { findDeliveryDrivers, findDeliveryDriverByDriverId } from '../../../src/dao/pn-deliveryDriver-dao';
import { dynamoDBClient } from '../../../src/utils/awsClients';
import {
  DynamoDBDocumentClient,
  GetCommand
} from '@aws-sdk/lib-dynamodb';

describe('Delivery Driver DAO tests', () => {
  const dynamoMockClient = mockClient(dynamoDBClient);
  let ddbMock: ReturnType<typeof mockClient>;

  beforeEach(() => {
    dynamoMockClient.reset();
    ddbMock = mockClient(DynamoDBDocumentClient);
  });

  afterEach(() => {
    ddbMock.reset();
    ddbMock.restore();
  });

  test('when delivery driver exists return entity', async () => {
    // Arrange
    const scanInputCommand: ScanInput = {
      TableName: PN_DELIVERY_DRIVER_TABLE_NAME,
    } as ScanInput;

    dynamoMockClient
      .on(ScanCommand, scanInputCommand)
      .resolves(Promise.resolve(getDeliveryDriverListOutput));

    // Act
    const result = await findDeliveryDrivers();

    // Assert
    expect(result[0]).toEqual(deliveryDriverItem);
    expect(dynamoMockClient.calls()).toHaveLength(1);
  });

  
  test('when delivery driver with given deliveryDriverId exists return entity', async () => {


    const params = {
      TableName: PN_DELIVERY_DRIVER_TABLE_NAME,
      Key: {
        deliveryDriverId: "deliveryDriverId"
      }
    };
    ddbMock.on(GetCommand, params).resolves({ Item: getDeliveryDriverItem })

    // Act
    const result = await findDeliveryDriverByDriverId("deliveryDriverId");

    // Assert
    expect(result).toEqual(deliveryDriverItem);
    expect(ddbMock.calls()).toHaveLength(1);
  });
});
