import { validatorEvent } from '../../../src/middlewares/validators';

describe("Validator tests", () => {
  describe("Validator event tests", () => {

    test('should throw error for invalid event', () => {
      const badlyEvent = {
        type: "TENDERS",
        from: "12:234"
      };

      expect(() => validatorEvent(badlyEvent)).toThrow(Error("Invalid event data"));
    });

    test('should return valid Tender Event', () => {
      const event = {
        operation: "GET_TENDERS",
        page: 2,
        size: 2
      };

      const result = validatorEvent(event);

      expect(result).toEqual({
        operation: "GET_TENDERS",
        page: 2,
        size: 2
      });
    });

    test('should return valid Tender Active Event', () => {
      const event = {
        operation: "GET_TENDER_ACTIVE",
        name: "lllll"
      };

      const result = validatorEvent(event);
      expect(result).toEqual({
        operation: "GET_TENDER_ACTIVE",
      });
    });

    test('should return valid Costs Event', () => {
      const event = {
        operation: "GET_COSTS",
        tenderId: "1234",
        product: "AR"
      };


      const result = validatorEvent(event);
      expect(result).toEqual({
        operation: "GET_COSTS",
        tenderId: "1234",
        product: "AR",
      });
    });

    test('should return valid Cost Event', () => {
      const event = {
        operation: "GET_COST",
        tenderId: "1234",
      };


      const result = validatorEvent(event);
      expect(result).toEqual({
        operation: "GET_COST",
        tenderId: "1234",
      });
    });

    test('should return valid Delivery Driver Event', () => {
      const event = {
        operation: "GET_DELIVERY_DRIVERS",
        from: new Date()
      };

      const result = validatorEvent(event);

      expect(result).toEqual({
        operation: "GET_DELIVERY_DRIVERS",
        from: event.from,
      });
    });

  });
});
