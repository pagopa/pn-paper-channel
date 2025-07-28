import {
  AttributeValue,
  DynamoDBClient,
  QueryInput,
} from '@aws-sdk/client-dynamodb';

const dynamoDBClient = new DynamoDBClient();

class QueryCommandBuilder {
  private readonly tableName: string | undefined;
  private filterExpression: string[] = [];
  private keyConditionExpression: string[] = [];
  private expressionValues: Record<string, AttributeValue> = {};

  constructor(tableName: string | undefined) {
    this.tableName = tableName;
  }

  public addKeyCondition(key: string, value: string | undefined) {
    if(value) {
      this.keyConditionExpression.push(`${key} = :${key}`);
      this.expressionValues[`:${key}`] = { S: value };
    }
    return this;
  }

  public addFilter(key: string, value: string | undefined): this {
    if (value) {
      this.filterExpression.push(`${key} = :${key}`);
      this.expressionValues[`:${key}`] = { S: value };
    }
    return this;
  }

  public build(): QueryInput {
    return {
      TableName: this.tableName,
      FilterExpression: (this.filterExpression.length == 0) ? undefined : this.filterExpression.join(' AND '),
      KeyConditionExpression: (this.keyConditionExpression.length == 0) ? undefined : this.keyConditionExpression.join(' AND '),
      ExpressionAttributeValues: this.expressionValues,
    };
  }
}

export { QueryCommandBuilder, dynamoDBClient };
