// import  { Route } from "@middy/http-router";
// import { APIGatewayProxyEvent, APIGatewayProxyResult } from 'aws-lambda';
// import middy from '@middy/core';
// import { validatorMiddlewareGetTenders } from '../middlewares/validators';
// import { getActiveTender, getAllTenders } from '../services/tender-service';




// const getTendersHandler = middy()
//   .use(validatorMiddlewareGetTenders)
//   .handler(async (event: APIGatewayProxyEvent): Promise<APIGatewayProxyResult> => {
//     console.log("GET TENDERS")
//     const { queryStringParameters } = event
//     console.log("Query Parameters ", queryStringParameters)

//     const pageInt = parseInt(queryStringParameters!.page!)
//     const sizeInt = parseInt(queryStringParameters!.size!)
//     const from = queryStringParameters?.from
//     const to = queryStringParameters?.to

//     const response = await getAllTenders(pageInt, sizeInt, from, to);
//     console.log("Response ", response)
//     return {
//       statusCode: 200,
//       body: JSON.stringify(response),
//     }
//   })

// const getActiveTenderHandler = middy()
//   .handler(async (): Promise<APIGatewayProxyResult> => {
//     console.log("GET ACTIVE TENDER")

//     const active = await getActiveTender()

//     console.log("Response ", active)

//     return {
//       statusCode: 200,
//       body: JSON.stringify(active)
//     }
//   })


// export const tenderRoutes:Route<APIGatewayProxyEvent, APIGatewayProxyResult>[] = [
//   {
//     method: 'GET',
//     path: '/paper-channel-private/v2/tenders',
//     handler: getTendersHandler
//   },
//   {
//     method: 'GET',
//     path: '/paper-channel-private/v2/tenders/active',
//     handler: getActiveTenderHandler
//   }
// ]