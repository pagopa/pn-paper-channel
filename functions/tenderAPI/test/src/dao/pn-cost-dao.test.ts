import { PN_COST_TABLE_NAME } from '../../../src/config';
import { buildCostSortKey } from '../../../src/utils/builders';
import {
  AttributeValue,
  GetItemCommand,
  GetItemCommandInput,
  QueryCommand,
  QueryInput,
} from '@aws-sdk/client-dynamodb';
import { mockClient } from 'aws-sdk-client-mock';
import {
  costItem,
  getItemCostOutput,
  getItemCostListOutput,
} from '../config/model-mock';
import { findCost, findCosts } from '../../../src/dao/pn-cost-dao';
import { dynamoDBClient } from '../../../src/utils/awsClients';

describe('Cost DAO tests', () => {
  const dynamoMockClient = mockClient(dynamoDBClient);

  describe('Find Cost', () => {
    beforeEach(() => {
      dynamoMockClient.reset();
    });

    test('when cost exists should return entity', async () => {
      // Arrange:
      const tenderId = '12345';
      const product = 'AR';
      const lot = 'LOT_1';
      const zone = 'EU';
      const inputCommand = {
        TableName: PN_COST_TABLE_NAME,
        Key: {
          tenderId: {
            S: tenderId,
          },
          productLotZone: {
            S: buildCostSortKey(product, lot, zone),
          },
        },
      } as GetItemCommandInput;

      dynamoMockClient
        .on(GetItemCommand, inputCommand)
        .resolves(Promise.resolve(getItemCostOutput));

      // Act
      const result = await findCost(tenderId, product, lot, zone);

      // Assert
      expect(result).toEqual(costItem);
    });

    test('when cost not exists should return undefined', async () => {
      // Arrange:
      const tenderId = '12345';
      const product = 'AR';
      const lot = 'LOT_1';
      const zone = 'EU';
      const inputCommand = {
        TableName: PN_COST_TABLE_NAME,
        Key: {
          tenderId: {
            S: tenderId,
          },
          productLotZone: {
            S: buildCostSortKey(product, lot, zone),
          },
        },
      } as GetItemCommandInput;

      const outputCommand = {
        ...getItemCostOutput,
        Item: undefined,
      };

      dynamoMockClient
        .on(GetItemCommand, inputCommand)
        .resolves(Promise.resolve(outputCommand));

      // Act
      const result = await findCost(tenderId, product, lot, zone);

      // Assert
      expect(result).toBeUndefined();
    });

    test('when tenderId is found then a list of costs is returned', async () => {
      // Arrange
      const tenderId = '12345';
      const keyCondition: string = 'tenderId = :tenderId';
      const expressionValues: Record<string, AttributeValue> = {
        ':tenderId': {
          S: tenderId,
        },
      };

      const queryInput = getQueryInput(undefined, expressionValues, keyCondition);
      const queryOutput = getQueryOutput();

      dynamoMockClient
        .on(QueryCommand, queryInput)
        .resolves(Promise.resolve(queryOutput));

      // Act
      const result = await findCosts(
        tenderId,
        undefined,
        undefined,
        undefined,
        undefined
      );

      // Assert
      expect(result).toBeDefined();
      expect(result![0]!.tenderId).toEqual(tenderId);
      expect(dynamoMockClient.calls()).toHaveLength(1);
    });

    test('when tenderId and product are found then a list of costs is returned', async () => {
      // Arrange
      const tenderId = '12345';
      const product = 'AR';
      const keyCondition: string = "tenderId = :tenderId"
      const expressionValues: Record<string, AttributeValue> = {
        ':tenderId': {
          S: tenderId,
        }
      };

      const queryInput = getQueryInput(undefined, expressionValues, keyCondition);
      const queryOutput = getQueryOutput();

      dynamoMockClient
        .on(QueryCommand, queryInput)
        .resolves(Promise.resolve(queryOutput));

      // Act
      const result = await findCosts(
        tenderId,
        product,
        undefined,
        undefined,
        undefined
      );

      // Assert
      expect(result).toBeDefined();
      expect(result![0]!.tenderId).toEqual(tenderId);
      expect(result![0]!.product).toEqual(product);
      expect(dynamoMockClient.calls()).toHaveLength(1);
    });

    test('when tenderId, product and lot are found then a list of costs is returned', async () => {
      // Arrange
      const tenderId = '12345';
      const product = 'AR';
      const lot = 'LOT_1';
      const keyCondition: string = "tenderId = :tenderId"
      const expressionValues: Record<string, AttributeValue> = {
        ':tenderId': {
          S: tenderId,
        },
      };

      const queryInput = getQueryInput(undefined, expressionValues, keyCondition);
      const queryOutput = getQueryOutput();

      dynamoMockClient
        .on(QueryCommand, queryInput)
        .resolves(Promise.resolve(queryOutput));

      // Act
      const result = await findCosts(
        tenderId,
        product,
        lot,
        undefined,
        undefined
      );

      // Assert
      expect(result).toBeDefined();
      expect(result![0]!.tenderId).toEqual(tenderId);
      expect(result![0]!.product).toEqual(product);
      expect(result![0]!.lot).toEqual(lot);
      expect(dynamoMockClient.calls()).toHaveLength(1);
    });

    test('when tenderId, product, lot and zone are found then a list of costs is returned', async () => {
      // Arrange
      const tenderId = '12345';
      const product = 'AR';
      const lot = 'LOT_1';
      const zone = 'EU';
      const keyCondition: string = "tenderId = :tenderId";
      const expressionValues: Record<string, AttributeValue> = {
        ':tenderId': {
          S: tenderId,
        },
      };

      const queryInput = getQueryInput(undefined, expressionValues, keyCondition);
      const queryOutput = getQueryOutput();

      dynamoMockClient
        .on(QueryCommand, queryInput)
        .resolves(Promise.resolve(queryOutput));

      // Act
      const result = await findCosts(tenderId, product, lot, zone, undefined);

      // Assert
      expect(result).toBeDefined();
      expect(result![0]!.tenderId).toEqual(tenderId);
      expect(result![0]!.product).toEqual(product);
      expect(result![0]!.lot).toEqual(lot);
      expect(result![0]!.zone).toEqual(zone);
      expect(dynamoMockClient.calls()).toHaveLength(1);
    });

    test('when tenderId, product, lot, zone and deliveryDriverId are found then a list of costs is returned', async () => {
      // Arrange
      const tenderId = '12345';
      const product = 'AR';
      const lot = 'LOT_1';
      const zone = 'EU';
      const deliveryDriverId = '121212';
      const keyCondition: string = "tenderId = :tenderId";
      const expressionValues: Record<string, AttributeValue> = {
        ':tenderId': {
          S: tenderId,
        }
      };

      const queryInput = getQueryInput(undefined, expressionValues, keyCondition);
      const queryOutput = getQueryOutput();

      dynamoMockClient
        .on(QueryCommand, queryInput)
        .resolves(Promise.resolve(queryOutput));

      // Act
      const result = await findCosts(
        tenderId,
        product,
        lot,
        zone,
        deliveryDriverId
      );

      // Assert
      expect(result).toBeDefined();
      expect(result![0]!.tenderId).toEqual(tenderId);
      expect(result![0]!.product).toEqual(product);
      expect(result![0]!.lot).toEqual(lot);
      expect(result![0]!.zone).toEqual(zone);
      expect(result![0]!.deliveryDriverId).toEqual(deliveryDriverId);
      expect(dynamoMockClient.calls()).toHaveLength(1);
    });
  });


});

const getQueryInput = (
  filterExpression: string | undefined,
  expressionValues: Record<string, AttributeValue>,
  keyExpression: string | undefined = undefined
): QueryInput => {
  return {
    TableName: PN_COST_TABLE_NAME,
    KeyConditionExpression: keyExpression,
    FilterExpression: filterExpression,
    ExpressionAttributeValues: expressionValues,
  } as QueryInput;
};

const getQueryOutput = () => {
  return {
    ...getItemCostListOutput,
  };
};