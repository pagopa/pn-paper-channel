/*
import { apiHandler } from '../../../src/handlers/api-handler';
import { APIGatewayProxyEvent } from 'aws-lambda';
import mockContext from 'aws-lambda-mock-context';
import { MiddyHandlerObject } from '@middy/core';
import { expect } from 'chai';



describe("Api Handler Routes", () => {


  it("call GetAllTenders when path is /tenders", async () => {

    const event: APIGatewayProxyEvent = {
      multiValueHeaders: {},
      pathParameters: {},
      multiValueQueryStringParameters: {},
      stageVariables: {},
      "headers": {},
      "resource": "lambda-test-api-gateway",
      "httpMethod": "GET",
      "path": "/paper-channel-private/v2/tenders",
      "queryStringParameters": {
        "pippo": "2000-07-15T11:07:38Z",
        "to": "2025-07-15T11:07:38Z",
        "page": "1",
        "size": "10"
      },
      "requestContext": {
        "resourceId": "2gxmpl",
        "resourcePath": "/",
        "httpMethod": "GET",
        "path": "/paper-channel-private/v2/tenders",
        "accountId": "123456789012",
        "protocol": "HTTP/1.1",
        "stage": "local",
        "requestTimeEpoch": 1583798639428,
        "requestId": "77375676-xmpl-4b79-853a-f982474efe18",
        "apiId": "70ixmpl4fl",
        authorizer: {},
        identity: {
          accessKey: null,
          accountId: null,
          apiKey: null,
          apiKeyId: null,
          caller: null,
          clientCert: null,
          cognitoAuthenticationProvider: null,
          cognitoAuthenticationType: null,
          cognitoIdentityId: null,
          cognitoIdentityPoolId: null,
          principalOrgId: null,
          sourceIp: '',
          user: null,
          userAgent: null,
          userArn: null,
        }
      },
      "body": null,
      "isBase64Encoded": false
    }

    const result = await apiHandler(event, mockContext(), { } as MiddyHandlerObject)

    expect(result).not.to.deep.equal({});


  })

})*/