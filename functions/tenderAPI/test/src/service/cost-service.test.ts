import { findGeokey } from '../../../src/dao/pn-geokey-dao';
import { getCost, getCosts } from '../../../src/services/cost-service';
import { NotFoundError } from '../../../src/types/error-types';
import { costItem, geokeyItem } from '../config/model-mock';
import { findCost, findCosts } from '../../../src/dao/pn-cost-dao';
import { batchRetrieveCosts } from '../../../src/services/cost-service';
import { batchGetCost } from '../../../src/dao/pn-cost-dao';

jest.mock('../../../src/dao/pn-geokey-dao');
jest.mock('../../../src/dao/pn-cost-dao');

describe('Cost Service Test', () => {

  describe('get costs Service Test', () => {
    test('when tenderId is invalid should throw NotFoundError', async () => {
      // Arrange
      const tenderId = '1234';

      (findCosts as jest.Mock).mockReturnValue(Promise.resolve([]));

      // Act && Assert
      await expect(() =>
        getCosts(tenderId)
      ).rejects.toThrowError(
        new NotFoundError('Entity Cost [1234] not found')
      );
    });
  });

  describe('get Cost Service Test', () => {
    test('when geokey not exists should throw NotFoundError', async () => {
      // Arrange
      const tenderId = '1234';
      const product = 'AR';
      const geokey = '84758';
      (findGeokey as jest.Mock).mockReturnValue(Promise.resolve(undefined));

      // Act && Assert
      await expect(() =>
        getCost(tenderId, product, geokey)
      ).rejects.toThrowError(
        new NotFoundError('Entity Geokey [84758] not found')
      );
    });

    test('when cost exist should throw NotFoundError', async () => {
      // Arrange
      const tenderId = '1234';
      const product = 'AR';
      const geokey = '84758';
      const geokeyEntity = geokeyItem;

      (findGeokey as jest.Mock).mockReturnValue(Promise.resolve(geokeyEntity));
      (findCost as jest.Mock).mockReturnValue(Promise.resolve(undefined));

      // Act && Assert
      await expect(() =>
        getCost(tenderId, product, geokey)
      ).rejects.toThrowError(
        new NotFoundError('Entity Cost [AR - ZON1 - EU] not found')
      );
    });

    test('when cost exist should return cost', async () => {
      // Arrange
      const tenderId = '1234';
      const product = 'AR';
      const geokey = '84758';
      const geokeyEntity = geokeyItem;
      const costEntity = costItem;

      (findGeokey as jest.Mock).mockReturnValue(Promise.resolve(geokeyEntity));
      (findCost as jest.Mock).mockReturnValue(Promise.resolve(costEntity));

      // Act && Assert
      const result = await getCost(tenderId, product, geokey);

      expect(result).toEqual(costEntity);
    });

    describe('batchRetrieveCosts Service Test', () => {
      afterEach(() => {
        jest.clearAllMocks();
      });

      test('should return costs when batchGetCost resolves', async () => {
        const requests = Array.from({ length: 30 }, (_, i) => `req${i}`);
        const tenderId = 'tender1';
        const mockCost = [{ id: 'cost1' }, { id: 'cost2' }];
        (batchGetCost as jest.Mock).mockResolvedValue(mockCost);

        const result = await batchRetrieveCosts(requests, tenderId);

        expect(batchGetCost).toHaveBeenCalledTimes(2); 
        expect(result).toEqual([...mockCost, ...mockCost]);
      });

      test('should handle errors and continue processing other chunks', async () => {
        const requests = Array.from({ length: 50 }, (_, i) => `req${i}`);
        const tenderId = 'tender2';
        (batchGetCost as jest.Mock)
          .mockRejectedValueOnce(new Error('fail'))
          .mockResolvedValueOnce([{ id: 'cost3' }]);

        const result = await batchRetrieveCosts(requests, tenderId);

        expect(batchGetCost).toHaveBeenCalledTimes(2);
        expect(result).toEqual([{ id: 'cost3' }]);
      });

      test('should return empty array if no costs found', async () => {
        const requests: string[] = [];
        const tenderId = 'tender3';

        const result = await batchRetrieveCosts(requests, tenderId);

        expect(batchGetCost).not.toHaveBeenCalled();
        expect(result).toEqual([]);
      });
    });
  });
});