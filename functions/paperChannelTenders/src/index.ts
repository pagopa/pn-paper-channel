import lambdaApi, { Request, Response } from 'lambda-api';
import { APIGatewayProxyEvent, Context } from 'aws-lambda';
const api = lambdaApi();


// Definizione dell'endpoint /status
api.get('/status', async (req: Request, res: Response) => {
  res.json({ status: 'API is up and running' });
});

// Definizione dell'endpoint /paper-channel-private/v2/tenders

api.get('/paper-channel-private/v2/tenders', async (req: Request, res: Response) => {
  res.status(501);
});

// Definizione dell'endpoint /paper-channel-private/v2/tenders/:tenderId/costs
api.get('/paper-channel-private/v2/tenders/:tenderId/costs', async (req: Request, res: Response) => {
  res.status(501)
});

// Definizione dell'endpoint /paper-channel/v2/tenders/:tenderId/geokeys/:geokey/:product/costs
api.get('/paper-channel/v2/tenders/:tenderId/geokeys/:geokey/:product/costs', async (req: Request, res: Response) => {
  res.status(501)
});

// Definizione dell'endpoint /paper-channel-private/v2/delivery-drivers
api.get('/paper-channel-private/v2/delivery-drivers', async (req: Request, res: Response) => {
  res.status(501)
});

// Gestione degli errori
api.use((err: Error, req: Request, res: Response) => {
  console.error(err);
  res.status(500).json({ error: 'Internal Server Error' });
});


export const handler = async (event: APIGatewayProxyEvent, context: Context) => {
  return await api.run(event, context);
};































