import { DynamoDBClient } from '@aws-sdk/client-dynamodb';

const dynamoDBClient = new DynamoDBClient({ region: 'eu-south-1' });

export { dynamoDBClient };