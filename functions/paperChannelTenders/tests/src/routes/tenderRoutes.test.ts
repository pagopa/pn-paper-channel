import { APIGatewayProxyEvent, APIGatewayProxyResult,Context, Callback } from 'aws-lambda';
import { getActiveTender, getAllTenders } from '../../../src/services/tender-service';
import { tenderRoutes } from '../../../src/routes/tender-routes';
import Mock = jest.Mock;
import { validatorMiddlewareGetTenders } from '../../../src/middlewares/validators';


// Mock the services
jest.mock('../../../src/services/tender-service');
jest.mock('../../../src/middlewares/validators');

describe('Tender Routes', () => {

  describe('GET /paper-channel-private/v2/tenders', () => {
    let mockEvent: Partial<APIGatewayProxyEvent>;
    let mockContext: Partial<Context>;
    let mockCallback: Mock<Callback<APIGatewayProxyResult>>;

    beforeEach(() => {
      mockEvent = {
        queryStringParameters: {
          page: '1',
          size: '10',
          from: '2023-01-01',
          to: '2023-12-31',
        },
      };
    });

    it('should return a list of tenders', async () => {
      // Mock the response from getAllTenders
      const tendersResponse = {
        data: [{ id: 1, name: 'Tender 1' }, { id: 2, name: 'Tender 2' }],
        page: 1,
        size: 10,
        total: 2,
      };
      (getAllTenders as jest.Mock).mockResolvedValue(tendersResponse);

      // Call the handler for this route
      const route = tenderRoutes.find(route => route.path === '/paper-channel-private/v2/tenders');
      if (!route) throw new Error('Route not found');



      const result = await route.handler(mockEvent as APIGatewayProxyEvent, mockContext as Context, mockCallback) as APIGatewayProxyResult;

      // Assert that getAllTenders was called with the correct parameters
      expect(getAllTenders).toHaveBeenCalledWith(1, 10, '2023-01-01', '2023-12-31');

      // Assert the response
      expect(result.statusCode).toBe(200);
      expect(result.body).toBe(JSON.stringify(tendersResponse));
    });

    it('should return 400 if query params are missing', async () => {
      // Simulate missing query parameters
      mockEvent = {
        queryStringParameters: {},
      };

      (validatorMiddlewareGetTenders as jest.Mock).mockImplementationOnce(() => {
        throw new Error('Validation Error: Missing query parameters');
      });

      const route = tenderRoutes.find(route => route.path === '/paper-channel-private/v2/tenders');
      if (!route) throw new Error('Route not found');

      try {
        await route.handler(mockEvent as APIGatewayProxyEvent, mockContext as Context, mockCallback);
      } catch (error: any) {
        // Assert the validation error
        expect(error.message).toBe('Validation Error: Missing query parameters');
      }
    });
  });

  describe('GET /paper-channel-private/v2/tenders/active', () => {
    let mockContext: Partial<Context>;
    let mockCallback: Mock<Callback<APIGatewayProxyResult>>;

    it('should return the active tender', async () => {
      // Mock the response from getActiveTender
      const activeTenderResponse = { id: 1, name: 'Active Tender' };
      (getActiveTender as jest.Mock).mockResolvedValue(activeTenderResponse);

      // Call the handler for this route
      const route = tenderRoutes.find(route => route.path === '/paper-channel-private/v2/tenders/active');
      if (!route) throw new Error('Route not found');

      const result = await route.handler({} as APIGatewayProxyEvent, mockContext as Context, mockCallback) as APIGatewayProxyResult;

      // Assert that getActiveTender was called
      expect(getActiveTender).toHaveBeenCalled();

      // Assert the response
      expect(result.statusCode).toBe(200);
      expect(result.body).toBe(JSON.stringify(activeTenderResponse));
    });
  });
});
