import { singleCostHandler, tenderActiveHandler, tendersHandler } from '../../../src/handlers/api-handler';
import { CostEvent, TenderActiveEvent, TendersEvent } from '../../../src/types/schema-request-types';
import { getActiveTender, getAllTenders } from '../../../src/services/tender-service';
import { costItem, pageTender, tender } from '../config/model-mock';
import { NotFoundError } from '../../../src/types/error-types';
import { getCost } from '../../../src/services/cost-service';


jest.mock('../../../src/services/tender-service');
jest.mock('../../../src/services/cost-service');

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

  describe("Get single cost", () => {

    test("when not exist cost should return 404", async () => {
      // Arrange
      (getCost as jest.Mock).mockReturnValue(Promise.resolve(undefined));

      const event: CostEvent = {
        operation: "GET_COST",
        tenderId: "1234",
        product: "AR",
        geokey: "95869"
      }

      // Act
      const result = await singleCostHandler(event);

      // Assert
      expect(result).toEqual({
        statusCode: 404,
        description: "NOT_FOUND",
        body: undefined
      });

    });

    test("when cost exist should return 200 with data", async () => {
      // Arrange

      const costEntity = costItem;

      (getCost as jest.Mock).mockReturnValue(Promise.resolve(costEntity));

      const event: CostEvent = {
        operation: "GET_COST",
        tenderId: "1234",
        product: "AR",
        geokey: "95869"
      }

      // Act
      const result = await singleCostHandler(event);

      // Assert
      expect(result).toEqual({
        statusCode: 200,
        description: "OK",
        body: costEntity
      });

    });

  });

});