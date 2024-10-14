import {
  costHandler,
  costsHandler,
  geokeyHandler,
  tenderActiveHandler,
  tendersHandler,
} from '../../../src/handlers/api-handler';
import {
  CostEvent,
  CostsEvent, GeokeyEvent,
  OperationEnum,
  TenderActiveEvent,
  TendersEvent,
} from '../../../src/types/schema-request-types';
import { getActiveTender, getAllTenders } from '../../../src/services/tender-service';
import { costItem, geokeyItem, pageTender, tender } from '../config/model-mock';
import { NotFoundError } from '../../../src/types/error-types';
import { getCost, getCosts } from '../../../src/services/cost-service';
import { getGeokeys } from '../../../src/services/geokey-service';


jest.mock('../../../src/services/tender-service');
jest.mock('../../../src/services/cost-service');
jest.mock('../../../src/services/geokey-service');

describe("API Handlers", () => {

  describe("Tenders handler test", () => {

    test("when event have only page info should return page tender", async () => {
      // Arrange
      const event = {
        operation: OperationEnum.GET_TENDERS,
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

  describe("Tender active handler test", () => {

    test("when not exist active tender should throw NotFoundError", async () => {
      // Arrange
      const event = {
        operation: OperationEnum.GET_TENDER_ACTIVE
      } as TenderActiveEvent;

      (getActiveTender as jest.Mock).mockReturnValue(Promise.reject(new NotFoundError("TenderNotFound")));

      // Act & Assert
      await expect(() => tenderActiveHandler(event)).rejects
        .toThrow(new NotFoundError("TenderNotFound"));

    });

    test("when exist active tender should return 200 with tender", async () => {
      // Arrange
      const event = {
        operation: OperationEnum.GET_TENDER_ACTIVE
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

  describe("Costs handler test", () => {

    test("when array cost is empty should return empty array", async () => {
      // Arrange
      (getCosts as jest.Mock).mockReturnValue(Promise.resolve([]));

      const event: CostsEvent = {
        operation: OperationEnum.GET_COSTS,
        tenderId: "1234",
        product: "AR",
      }

      // Act
      const result = await costsHandler(event);

      // Assert
      expect(result).toEqual({
        statusCode: 200,
        description: "OK",
        body: []
      });

    });

    test("when array cost is not empty should return costs", async () => {
      // Arrange
      const costs = [costItem, costItem];
      (getCosts as jest.Mock).mockReturnValue(Promise.resolve(costs));

      const event: CostsEvent = {
        operation: OperationEnum.GET_COSTS,
        tenderId: "1234",
        product: "AR",
        lot: "LOT_1"
      }

      // Act
      const result = await costsHandler(event);

      // Assert
      expect(result).toEqual({
        statusCode: 200,
        description: "OK",
        body: costs
      });

    });

  });

  describe("Cost handler test", () => {

    test("when not exist cost should throw NotFoundError", async () => {
      // Arrange
      (getCost as jest.Mock).mockReturnValue(Promise.reject(new NotFoundError("CostNotFound")));

      const event: CostEvent = {
        operation: OperationEnum.GET_COST,
        tenderId: "1234",
        product: "AR",
        geokey: "95869"
      }

      // Act
      await expect(() => costHandler(event)).rejects
        .toThrow(new NotFoundError("CostNotFound"));
    });

    test("when not exist geokey should throw NotFoundError", async () => {
      // Arrange
      (getCost as jest.Mock).mockReturnValue(Promise.reject(new NotFoundError("GeokeyNotFound")));

      const event: CostEvent = {
        operation: OperationEnum.GET_COST,
        tenderId: "1234",
        product: "AR",
        geokey: "95869"
      }

      // Act
      await expect(() => costHandler(event)).rejects
        .toThrow(new NotFoundError("GeokeyNotFound"));
    });

    test("when cost exist should return 200 with data", async () => {
      // Arrange

      const costEntity = costItem;

      (getCost as jest.Mock).mockReturnValue(Promise.resolve(costEntity));

      const event: CostEvent = {
        operation: OperationEnum.GET_COST,
        tenderId: "1234",
        product: "AR",
        geokey: "95869"
      }

      // Act
      const result = await costHandler(event);

      // Assert
      expect(result).toEqual({
        statusCode: 200,
        description: "OK",
        body: costEntity
      });

    });

  });

  describe("Geokey handler test", () => {

    test("when geokey array is empty should return empty array", async () => {
      // Arrange
      (getGeokeys as jest.Mock).mockReturnValue(Promise.resolve([]));

      const event: GeokeyEvent = {
        operation: OperationEnum.GET_GEOKEY,
        tenderId: "1234",
        product: "AR",
        geokey: "95869"
      }

      // Act
      const result = await geokeyHandler(event);

      // Assert
      expect(result).toEqual({
        statusCode: 200,
        description: "OK",
        body: []
      });

    });

    test("when geokey array is not empty should return geokeys", async () => {
      // Arrange
      const geokeys = [geokeyItem, geokeyItem, geokeyItem];
      (getGeokeys as jest.Mock).mockReturnValue(Promise.resolve(geokeys));

      const event: GeokeyEvent = {
        operation: OperationEnum.GET_GEOKEY,
        tenderId: "1234",
        product: "AR",
        geokey: "95869"
      }

      // Act
      const result = await geokeyHandler(event);

      // Assert
      expect(result).toEqual({
        statusCode: 200,
        description: "OK",
        body: geokeys
      });

    });

  });

});