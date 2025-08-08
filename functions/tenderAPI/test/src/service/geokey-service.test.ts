import { getGeokeys, getGeokey } from '../../../src/services/geokey-service';
import { findAllGeokeys, findGeokey } from '../../../src/dao/pn-geokey-dao';
import { PaperChannelGeokey } from '../../../src/types/dynamo-types';

jest.mock('../../../src/dao/pn-geokey-dao');

const mockGeokey: PaperChannelGeokey = {
    tenderId: 'tender1',
    product: 'productA',
    geokey: 'geo1',
    tenderProductGeokey: 'tender1-productA-geo1',
    activationDate: '2024-01-01T00:00:00.000Z',
    lot: 'lot1',
    zone: 'zoneA',
    coverFlag: true,
    dismissed: false,
    createdAt: '2024-01-01T00:00:00.000Z',
};

describe('geokey-service', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('getGeokeys', () => {
        it.skip('should return an array of PaperChannelGeokey', async () => {
            (findAllGeokeys as jest.Mock).mockResolvedValue([mockGeokey]);
            const result = await getGeokeys('tender1', 'productA', 'geo1');
            expect(findAllGeokeys).toHaveBeenCalledWith('tender1', 'productA', 'geo1');
            expect(result).toEqual([mockGeokey]);
        });

        it.skip('should throw an error if findAllGeokeys fails', async () => {
            (findAllGeokeys as jest.Mock).mockRejectedValue(new Error('DB error'));
            await expect(getGeokeys('tender1', 'productA', 'geo1')).rejects.toThrow('DB error');
        });
    });

    describe('getGeokey', () => {
        it.skip('should return a PaperChannelGeokey object', async () => {
            (findGeokey as jest.Mock).mockResolvedValue(mockGeokey);
            const result = await getGeokey('tender1', 'productA', 'geo1');
            expect(findGeokey).toHaveBeenCalledWith('tender1', 'productA', 'geo1');
            expect(result).toEqual(mockGeokey);
        });

        it.skip('should return undefined if no geokey is found', async () => {
            (findGeokey as jest.Mock).mockResolvedValue(undefined);
            const result = await getGeokey('tender1', 'productA', 'geo1');
            expect(result).toBeUndefined();
        });

        it.skip('should throw an error if findGeokey fails', async () => {
            (findGeokey as jest.Mock).mockRejectedValue(new Error('DB error'));
            await expect(getGeokey('tender1', 'productA', 'geo1')).rejects.toThrow('DB error');
        });
    });
});