import { tenderActiveHandler, tendersHandler } from '../../../src/handlers/api-handler';
import { TenderActiveEvent, TendersEvent } from '../../../src/types/schema-request-types';
import { getActiveTender, getAllTenders } from '../../../src/services/tender-service';
import { pageTender, tender } from '../config/model-mock';
import { NotFoundError } from '../../../src/types/error-types';


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
        description: "OK",
        body: pageTenders,
      });

    });

  });

  describe("Find active Tender", () => {

    test("when not exist active tender should throw NotFoundError", async () => {
      // Arrange
      const event = {
        operation: "GET_TENDER_ACTIVE"
      } as TenderActiveEvent;

      (getActiveTender as jest.Mock).mockReturnValue(Promise.reject(new NotFoundError("TenderNotFound")));

      // Act & Assert
      await expect(() => tenderActiveHandler(event)).rejects
        .toThrow(new NotFoundError("TenderNotFound"));

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
        description: "OK",
        body: tenderActive,
      });
    });

  });

});