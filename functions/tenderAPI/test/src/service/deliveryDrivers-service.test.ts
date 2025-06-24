
import { findDeliveryDrivers, findDeliveryDriverByDriverId } from '../../../src/dao/pn-deliveryDriver-dao';
import * as costService from '../../../src/services/cost-service';
import { NotFoundError } from '../../../src/types/error-types';
import {
getAllDeliveryDrivers,
retrieveUnifiedDeliveryDriverForGivenRequests,
} from '../../../src/services/deliveryDrivers-service';

jest.mock('../../../src/dao/pn-deliveryDriver-dao');
jest.mock('../../../src/services/cost-service');

describe('deliveryDrivers-service', () => {
afterEach(() => {
    jest.clearAllMocks();
});

describe('getAllDeliveryDrivers', () => {
    it('should return all delivery drivers', async () => {
        const mockDrivers = [
            { id: 'driver1', name: 'Driver One' },
            { id: 'driver2', name: 'Driver Two' },
        ];
        (findDeliveryDrivers as jest.Mock).mockResolvedValue(mockDrivers);

        const result = await getAllDeliveryDrivers();

        expect(findDeliveryDrivers).toHaveBeenCalled();
        expect(result).toEqual(mockDrivers);
    });
});

describe('retrieveUnifiedDeliveryDriverForGivenRequests', () => {
    const tenderId = 'tender123';
    const body = [
        { product: 'productA', geoKey: 'geo1' },
        { product: 'productB', geoKey: 'geo2' },
    ];

    it('should return unified delivery drivers for valid requests', async () => {
        (costService.getCost as jest.Mock)
            .mockResolvedValueOnce({ deliveryDriverId: 'driverA' })
            .mockResolvedValueOnce({ deliveryDriverId: 'driverB' });

        (findDeliveryDriverByDriverId as jest.Mock)
            .mockResolvedValueOnce({ unifiedDeliveryDriver: 'unifiedA' })
            .mockResolvedValueOnce({ unifiedDeliveryDriver: 'unifiedB' });

        const result = await retrieveUnifiedDeliveryDriverForGivenRequests(body, tenderId);

        expect(costService.getCost).toHaveBeenCalledTimes(2);
        expect(findDeliveryDriverByDriverId).toHaveBeenCalledTimes(2);
        expect(result).toEqual([
            { geoKey: 'geo1', product: 'productA', unifiedDeliveryDriver: 'unifiedA' },
            { geoKey: 'geo2', product: 'productB', unifiedDeliveryDriver: 'unifiedB' },
        ]);
    });

    it('should throw NotFoundError if cost is not found', async () => {
        (costService.getCost as jest.Mock).mockResolvedValueOnce(undefined);

        await expect(
            retrieveUnifiedDeliveryDriverForGivenRequests(body, tenderId)
        ).rejects.toThrow(NotFoundError);

        if (body[0]) {
            expect(costService.getCost).toHaveBeenCalledWith(tenderId, body[0].product, body[0].geoKey);
        }
    });

    it('should throw NotFoundError if delivery driver is not found', async () => {
        (costService.getCost as jest.Mock).mockResolvedValueOnce({ deliveryDriverId: 'driverA' });
        (findDeliveryDriverByDriverId as jest.Mock).mockResolvedValueOnce(undefined);

        await expect(
            retrieveUnifiedDeliveryDriverForGivenRequests(body, tenderId)
        ).rejects.toThrow(NotFoundError);
    });

    it('should throw NotFoundError if unifiedDeliveryDriver is missing', async () => {
        (costService.getCost as jest.Mock).mockResolvedValueOnce({ deliveryDriverId: 'driverA' });
        (findDeliveryDriverByDriverId as jest.Mock).mockResolvedValueOnce({});

        await expect(
            retrieveUnifiedDeliveryDriverForGivenRequests(body, tenderId)
        ).rejects.toThrow(NotFoundError);
    });
});
});