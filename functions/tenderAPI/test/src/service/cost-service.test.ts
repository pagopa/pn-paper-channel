import { findGeokey } from '../../../src/dao/pn-geokey-dao';
import { getCost, getCosts } from '../../../src/services/cost-service';
import { NotFoundError } from '../../../src/types/error-types';
import { costItem, geokeyItem } from '../config/model-mock';
import { findCost, findCosts } from '../../../src/dao/pn-cost-dao';

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
  });
});