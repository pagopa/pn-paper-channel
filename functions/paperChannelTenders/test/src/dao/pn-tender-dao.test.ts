import { dynamoDBClient } from '../../../src/utils/builders';
import { AttributeValue, ScanCommand, ScanInput } from '@aws-sdk/client-dynamodb';
import { findActiveTender, findTenders } from '../../../src/dao/pn-tender-dao';
import { PaperChannelTender } from '../../../src/types/dynamo-types';
import { Page } from '../../../src/types/model-types';
import { mockClient } from 'aws-sdk-client-mock';
import { PN_TENDER_TABLE_NAME } from '../../../src/config';


describe('findTenders', () => {

  const dynamoMockClient = mockClient(dynamoDBClient);

  beforeEach(() => {
    const mockedDate = new Date();

    jest.useFakeTimers();
    jest.setSystemTime(mockedDate);
  });

  afterEach(() => {
    jest.useRealTimers();
  });

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
      FilterExpression: "activationDate <= :to AND activationDate >= :from",
      ExpressionAttributeValues: {
        ":from": {
          "S": new Date("1970").toISOString()
        },
        ":to": {
          "S": new Date().toISOString()
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

  test('return page results when date range is provided', async () => {
    // Arrange
    const from = new Date("2012-01-01");
    const to = new Date("2024-10-03");

    const mockResponse = {
      Items: [{
        tenderId: {
          "S": "12344"
        } as AttributeValue,
        activationDate: {
          "S": "2023-01-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "45678"
        } as AttributeValue,
        activationDate: {
          "S": "2022-06-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "67890"
        } as AttributeValue,
        activationDate: {
          "S": "2024-10-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "18860"
        } as AttributeValue,
        activationDate: {
          "S": "2019-08-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "72245"
        } as AttributeValue,
        activationDate: {
          "S": "2015-05-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "35341"
        } as AttributeValue,
        activationDate: {
          "S": "2013-01-01"
        } as AttributeValue
      }],
      Count: 6
    };

    const scanInput = {
      TableName: PN_TENDER_TABLE_NAME,
      FilterExpression: "activationDate <= :to AND activationDate >= :from",
      ExpressionAttributeValues: {
        ":from": {
          "S": from.toISOString()
        },
        ":to": {
          "S": to.toISOString()
        },
      }
    } as ScanInput


    dynamoMockClient.on(ScanCommand, scanInput).resolves(Promise.resolve(mockResponse))

    // Act
    const result = await findTenders(1, 10, from, to);

    // Assert
    expect(result).toEqual({
      content: [{
        tenderId: "12344",
        activationDate: "2023-01-01"
      }, {
        tenderId: "45678",
        activationDate: "2022-06-01"
      }, {
        tenderId: "67890",
        activationDate: "2024-10-01"
      }, {
        tenderId: "18860",
        activationDate: "2019-08-01"
      }, {
        tenderId: "72245",
        activationDate: "2015-05-01"
      }, {
        tenderId: "35341",
        activationDate: "2013-01-01"
      }],
      isFirstPage: true,
      isLastPage: true,
      totalPages: 1,
      number: 1,
      size: 10,
      totalElements: 6
    } as Page<PaperChannelTender>);
  });

  test('return page results when only from is provided', async () => {
    // Arrange
    const from = new Date("2012-01-01");

    const mockResponse = {
      Items: [{
        tenderId: {
          "S": "12344"
        } as AttributeValue,
        activationDate: {
          "S": "2023-01-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "45678"
        } as AttributeValue,
        activationDate: {
          "S": "2022-06-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "67890"
        } as AttributeValue,
        activationDate: {
          "S": "2024-10-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "18860"
        } as AttributeValue,
        activationDate: {
          "S": "2019-08-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "72245"
        } as AttributeValue,
        activationDate: {
          "S": "2015-05-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "35341"
        } as AttributeValue,
        activationDate: {
          "S": "2013-01-01"
        } as AttributeValue
      }],
      Count: 6
    };

    const scanInput = {
      TableName: PN_TENDER_TABLE_NAME,
      FilterExpression: "activationDate <= :to AND activationDate >= :from",
      ExpressionAttributeValues: {
        ":from": {
          "S": from.toISOString()
        },
        ":to": {
          "S": new Date().toISOString()
        },
      }
    } as ScanInput

    dynamoMockClient.on(ScanCommand, scanInput).resolves(Promise.resolve(mockResponse))

    // Act
    const result = await findTenders(1, 10, from, undefined);

    // Assert
    expect(result).toEqual({
      content: [{
        tenderId: "12344",
        activationDate: "2023-01-01"
      }, {
        tenderId: "45678",
        activationDate: "2022-06-01"
      }, {
        tenderId: "67890",
        activationDate: "2024-10-01"
      }, {
        tenderId: "18860",
        activationDate: "2019-08-01"
      }, {
        tenderId: "72245",
        activationDate: "2015-05-01"
      }, {
        tenderId: "35341",
        activationDate: "2013-01-01"
      }],
      isFirstPage: true,
      isLastPage: true,
      totalPages: 1,
      number: 1,
      size: 10,
      totalElements: 6
    } as Page<PaperChannelTender>);
  });

  test('return page results when only to is provided', async () => {
    // Arrange
    const to = new Date("2024-10-01");

    const mockResponse = {
      Items: [{
        tenderId: {
          "S": "12344"
        } as AttributeValue,
        activationDate: {
          "S": "2023-01-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "45678"
        } as AttributeValue,
        activationDate: {
          "S": "2022-06-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "67890"
        } as AttributeValue,
        activationDate: {
          "S": "2024-10-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "18860"
        } as AttributeValue,
        activationDate: {
          "S": "2019-08-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "72245"
        } as AttributeValue,
        activationDate: {
          "S": "2015-05-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "35341"
        } as AttributeValue,
        activationDate: {
          "S": "2013-01-01"
        } as AttributeValue
      }],
      Count: 6
    };

    const scanInput = {
      TableName: PN_TENDER_TABLE_NAME,
      FilterExpression: "activationDate <= :to AND activationDate >= :from",
      ExpressionAttributeValues: {
        ":from": {
          "S": new Date("1970").toISOString()
        },
        ":to": {
          "S": to.toISOString()
        },
      }
    } as ScanInput

    dynamoMockClient.on(ScanCommand, scanInput).resolves(Promise.resolve(mockResponse))

    // Act
    const result = await findTenders(1, 10, undefined, to);

    // Assert
    expect(result).toEqual({
      content: [{
        tenderId: "12344",
        activationDate: "2023-01-01"
      }, {
        tenderId: "45678",
        activationDate: "2022-06-01"
      }, {
        tenderId: "67890",
        activationDate: "2024-10-01"
      }, {
        tenderId: "18860",
        activationDate: "2019-08-01"
      }, {
        tenderId: "72245",
        activationDate: "2015-05-01"
      }, {
        tenderId: "35341",
        activationDate: "2013-01-01"
      }],
      isFirstPage: true,
      isLastPage: true,
      totalPages: 1,
      number: 1,
      size: 10,
      totalElements: 6
    } as Page<PaperChannelTender>);
  });

  test('find and return the active tender', async () => {
    // Arrange
    const mockResponse = {
      Items: [{
        tenderId: {
          "S": "12344"
        } as AttributeValue,
        activationDate: {
          "S": "2023-01-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "45678"
        } as AttributeValue,
        activationDate: {
          "S": "2022-06-01"
        } as AttributeValue
      }, {
        tenderId: {
          "S": "67890"
        } as AttributeValue,
        activationDate: {
          "S": "2024-10-01"
        } as AttributeValue
      }],
      Count: 3
    };

    const scanInput = {
      TableName: PN_TENDER_TABLE_NAME,
      FilterExpression: "activationDate <= :now",
      ExpressionAttributeValues: {
        ":now": {
          "S": new Date().toISOString()
        }
      }
    } as ScanInput;

    dynamoMockClient.on(ScanCommand, scanInput).resolves(Promise.resolve(mockResponse))

    //Act
    const result = await findActiveTender();

    //Assert
    expect(result).toEqual({
        tenderId: "67890",
        activationDate: "2024-10-01"
      });
  });

  test('try to return an active tender but it was not found', async () => {
    // Arrange
    const mockResponse = {
      Items: [],
      Count: 0
    };

    const scanInput = {
      TableName: PN_TENDER_TABLE_NAME,
      FilterExpression: "activationDate <= :now",
      ExpressionAttributeValues: {
        ":now": {
          "S": new Date().toISOString()
        }
      }
    } as ScanInput;

    dynamoMockClient.on(ScanCommand, scanInput).resolves(Promise.resolve(mockResponse))

    //Act & Assert
    await expect(findActiveTender())
      .rejects
      .toThrow(Error("Not found Tenders"));
  });
});