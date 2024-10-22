import { NotFoundError } from '../../../src/types/error-types';
import { findActiveTender } from '../../../src/dao/pn-tender-dao';
import { getActiveTender } from '../../../src/services/tender-service';


jest.mock('../../../src/dao/pn-tender-dao');

describe('Tender Service Test', () => {

  describe('get active tender Service Test', () => {

    test('when no one tender active was found should throw NotFoundError', async () => {
      // Arrange
      (findActiveTender as jest.Mock).mockReturnValue(Promise.resolve(undefined));

      // Act && Assert
      await expect(() =>
        getActiveTender()
      ).rejects.toThrowError(
        new NotFoundError('Active tender not found')
      );
    });

  });

});