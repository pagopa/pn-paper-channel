import { validatorEvent } from '../../../src/middlewares/validators';
import { ValidatorError } from '../../../src/types/error-types';
import { ValidationField } from '../../../src/types/model-types';

describe('Validator tests', () => {
  describe('Validator event tests', () => {
    test('should throw error for invalid operation event', () => {
      const badlyEvent = {
        operation: 'XXX',
        from: '12:234',
      };

      const validationField: ValidationField[] = [
        {
          fieldId: 'operation',
          message:
            "Invalid enum value. Expected 'GET_TENDERS' | 'GET_TENDER_ACTIVE' | 'GET_COSTS' | 'GET_COST' | 'GET_DELIVERY_DRIVERS' | 'GET_GEOKEY', received 'XXX'",
        },
      ];

      const validatorError = new ValidatorError(
        'Event badly format',
        validationField
      );

      try {
        validatorEvent(badlyEvent);
      } catch (e) {
        expect(e).toBeInstanceOf(ValidatorError);
        expect((e as ValidatorError).message).toEqual(validatorError.message);
        expect((e as ValidatorError).fields).toEqual(validationField);
      }
    });

    test('should throw error for invalid field of event', () => {
      const badlyEvent = {
        operation: 'GET_TENDERS',
        from: '12:234',
      };

      const validationField: ValidationField[] = [
        { fieldId: 'page', message: 'Required' },
        { fieldId: 'size', message: 'Required' },
        { fieldId: 'from', message: 'Invalid datetime' },
      ];

      const validatorError = new ValidatorError(
        'Event badly format',
        validationField
      );

      try {
        validatorEvent(badlyEvent);
      } catch (e) {
        expect(e).toBeInstanceOf(ValidatorError);
        expect((e as ValidatorError).message).toEqual(validatorError.message);
        expect((e as ValidatorError).fields).toEqual(validationField);
      }
    });

    test('should return valid Tender Event', () => {
      const event = {
        operation: 'GET_TENDERS',
        page: 2,
        size: 2,
      };

      const result = validatorEvent(event);

      expect(result).toEqual({
        operation: 'GET_TENDERS',
        page: 2,
        size: 2,
      });
    });

    test('should return valid Tender Active Event', () => {
      const event = {
        operation: 'GET_TENDER_ACTIVE',
        name: 'lllll',
      };

      const result = validatorEvent(event);
      expect(result).toEqual({
        operation: 'GET_TENDER_ACTIVE',
      });
    });

    test('should return valid Costs Event', () => {
      const event = {
        operation: 'GET_COSTS',
        tenderId: '1234',
        product: 'AR',
      };

      const result = validatorEvent(event);
      expect(result).toEqual({
        operation: 'GET_COSTS',
        tenderId: '1234',
        product: 'AR',
      });
    });

    test('should return valid Cost Event', () => {
      const event = {
        operation: 'GET_COST',
        tenderId: '1234',
        geokey: '85994',
        product: 'AR',
      };

      const result = validatorEvent(event);
      expect(result).toEqual({
        operation: 'GET_COST',
        tenderId: '1234',
        geokey: '85994',
        product: 'AR',
      });
    });

    test('should return valid Delivery Driver Event', () => {
      const event = {
        operation: 'GET_DELIVERY_DRIVERS'
      };

      const result = validatorEvent(event);

      expect(result).toEqual({
        operation: 'GET_DELIVERY_DRIVERS'
      });
    });
  });
});
