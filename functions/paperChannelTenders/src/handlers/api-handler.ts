import httpRouterHandler from '@middy/http-router';
import { tenderRoutes } from '../routes/tender-routes';
import middy from '@middy/core';


export const apiHandler = middy(httpRouterHandler(tenderRoutes));