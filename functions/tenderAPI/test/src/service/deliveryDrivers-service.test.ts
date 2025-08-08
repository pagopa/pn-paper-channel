
import { findDeliveryDrivers, findDeliveryDriversByDriverIds } from '../../../src/dao/pn-deliveryDriver-dao';
import * as costService from '../../../src/services/cost-service';
import { NotFoundError } from '../../../src/types/error-types';
import {
getAllDeliveryDrivers,
retrieveUnifiedDeliveryDrivers,
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

    describe('retrieveUnifiedDeliveryDrivers', () => {

        it('should retrieve unified delivery drivers correctly', async () => {
            const deliveryDriverGeoKeyProductTupleMap = {
            'driver1': ['geo1#productA', 'geo2#productB'],
            'driver2': ['geo3#productC']
            };
            const mockDrivers = [
                {
                    deliveryDriverId: 'driver1',
                    unifiedDeliveryDriver: 'unified1'
                },
                {
                    deliveryDriverId: 'driver2',
                    unifiedDeliveryDriver: 'unified2'
                }
            ];
            (findDeliveryDriversByDriverIds as jest.Mock).mockResolvedValue(mockDrivers);

            const result = await retrieveUnifiedDeliveryDrivers(deliveryDriverGeoKeyProductTupleMap);

            expect(findDeliveryDriversByDriverIds).toHaveBeenCalledWith(['driver1', 'driver2']);
            expect(result).toEqual([
                {
                    geoKey: 'geo1',
                    product: 'productA',
                    unifiedDeliveryDriver: 'unified1'
                },
                {
                    geoKey: 'geo2',
                    product: 'productB',
                    unifiedDeliveryDriver: 'unified1'
                },
                {
                    geoKey: 'geo3',
                    product: 'productC',
                    unifiedDeliveryDriver: 'unified2'
                }
            ]);
        });

        it('should throw NotFoundError if no delivery drivers found', async () => {
            const deliveryDriverGeoKeyProductTupleMap = {
                'driver1': ['geo1#productA']
            };
            (findDeliveryDriversByDriverIds as jest.Mock).mockResolvedValue([]);

            await expect(
                retrieveUnifiedDeliveryDrivers(deliveryDriverGeoKeyProductTupleMap)
            ).rejects.toThrow(NotFoundError);
        });

        it('should throw NotFoundError if unifiedDeliveryDriver is missing', async () => {
            const deliveryDriverGeoKeyProductTupleMap = {
                'driver1': ['geo1#productA']
            };
            const mockDrivers = [
                {
                    deliveryDriverId: 'driver1',
                    unifiedDeliveryDriver: undefined
                }
            ];
            (findDeliveryDriversByDriverIds as jest.Mock).mockResolvedValue(mockDrivers);

            await expect(
                retrieveUnifiedDeliveryDrivers(deliveryDriverGeoKeyProductTupleMap)
            ).rejects.toThrow(NotFoundError);
        }); });
});