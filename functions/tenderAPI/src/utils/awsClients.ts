import {
  AttributeValue,
  DynamoDBClient,
  QueryInput,
} from '@aws-sdk/client-dynamodb';

const dynamoDBClient = new DynamoDBClient({ region: 'eu-south-1' });

class QueryCommandBuilder {
  private readonly tableName: string | undefined;
  private filterExpression: string[] = [];
  private expressionValues: Record<string, AttributeValue> = {};

  constructor(tableName: string | undefined) {
    this.tableName = tableName;
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
      FilterExpression: this.filterExpression.join(' AND '),
      ExpressionAttributeValues: this.expressionValues,
    };
  }
}

export { QueryCommandBuilder, dynamoDBClient };
