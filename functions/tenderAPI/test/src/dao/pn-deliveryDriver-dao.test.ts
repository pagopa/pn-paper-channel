import { mockClient } from 'aws-sdk-client-mock';
import { PN_DELIVERY_DRIVER_TABLE_NAME } from '../../../src/config';
import { ScanCommand, ScanInput } from '@aws-sdk/client-dynamodb';
import {
  deliveryDriverItem,
  getDeliveryDriverListOutput,
  getDeliveryDriverItem,
} from '../config/model-mock';
import { findDeliveryDrivers, findDeliveryDriversByDriverIds } from '../../../src/dao/pn-deliveryDriver-dao';
import { dynamoDBClient } from '../../../src/utils/awsClients';
import { DynamoDBDocumentClient } from "@aws-sdk/lib-dynamodb";
import sinon from 'sinon';

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

});

describe('findDeliveryDriversByDriverIds tests', () => {
  let sendStub: sinon.SinonStub;

  beforeEach(() => {
      sendStub = sinon.stub();
      sinon.replace(
          require("@aws-sdk/lib-dynamodb").DynamoDBDocumentClient.prototype,
          "send",
          sendStub
      );
  });

  afterEach(() => {
      sinon.restore();
  });

  test('findDeliveryDriversByDriverIds returns entities when drivers exist', async () => {
    const tableName = PN_DELIVERY_DRIVER_TABLE_NAME ?? (() => { throw new Error('PN_DELIVERY_DRIVER_TABLE_NAME is undefined'); })();
    const deliveryDriverIds = ['driver1', 'driver2'];
    const mockItems = [
      { deliveryDriverId: 'driver1', unifiedDeliveryDriverId: 'Driver One' },
      { deliveryDriverId: 'driver2', unifiedDeliveryDriverId: 'Driver Two' }
    ];
    const batchGetResponse = {
      Responses: {
        [tableName]: mockItems
      }
    };

    sendStub.resolves(batchGetResponse);

    const result = await findDeliveryDriversByDriverIds(deliveryDriverIds);

    expect(result).toEqual([
      { deliveryDriverId: 'driver1', unifiedDeliveryDriverId: 'Driver One' },
      { deliveryDriverId: 'driver2', unifiedDeliveryDriverId: 'Driver Two' }
    ]);
    expect(sendStub.calledOnce).toBe(true);
  });

  test('findDeliveryDriversByDriverIds returns empty array when no drivers found', async () => {
    const tableName = PN_DELIVERY_DRIVER_TABLE_NAME ?? (() => { throw new Error('PN_DELIVERY_DRIVER_TABLE_NAME is undefined'); })();
    const deliveryDriverIds: string[] = ['driverX'];
    const batchGetResponse = {
      Responses: {
        [tableName]: []
      }
    };

    sendStub.resolves(batchGetResponse);

    const result = await findDeliveryDriversByDriverIds(deliveryDriverIds);

    expect(result).toEqual([]);
    expect(sendStub.calledOnce).toBe(true);
  });
});
