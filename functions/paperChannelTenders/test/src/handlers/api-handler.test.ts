import { tenderActiveHandler, tendersHandler } from '../../../src/handlers/api-handler';
import { TenderActiveEvent, TendersEvent } from '../../../src/types/schema-request-types';
import { getActiveTender, getAllTenders } from '../../../src/services/tender-service';
import { pageTender, tender } from '../config/model-mock';


jest.mock('../../../src/services/tender-service');

describe("API Handlers", () => {

  describe("Get all tenders", () => {

    test("when event have only page info should return page tender", async () => {
      // Arrange
      const event = {
        operation: "GET_TENDERS",
        page: 1,
        size: 20
      } as TendersEvent;

      const pageTenders = pageTender;

      (getAllTenders as jest.Mock).mockReturnValue(Promise.resolve(pageTenders));

      // Act
      const result = await tendersHandler(event);

      // Assert
      expect(result).toEqual({
        statusCode: 200,
        description: "Get all tenders",
        body: pageTenders,
      });

    });

  });

  describe("Find active Tender", () => {

    test("when not exist active tender should return 404", async () => {
      // Arrange
      const event = {
        operation: "GET_TENDER_ACTIVE"
      } as TenderActiveEvent;

      (getActiveTender as jest.Mock).mockReturnValue(Promise.resolve(undefined));

      // Act
      const result = await tenderActiveHandler(event);

      // Assert
      expect(result).toEqual({
        statusCode: 404,
        description: "Get tender active",
        body: undefined,
      });
    });

    test("when exist active tender should return 200 with tender", async () => {
      // Arrange
      const event = {
        operation: "GET_TENDER_ACTIVE"
      } as TenderActiveEvent;

      const tenderActive = tender;

      (getActiveTender as jest.Mock).mockReturnValue(Promise.resolve(tenderActive));

      // Act
      const result = await tenderActiveHandler(event);

      // Assert
      expect(result).toEqual({
        statusCode: 200,
        description: "Get tender active",
        body: tenderActive,
      });
    });

  });

});