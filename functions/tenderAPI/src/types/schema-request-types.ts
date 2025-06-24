import { z } from 'zod';

export enum OperationEnum {
  GET_UNIFIED_DELIVERY_DRIVERS = 'GET_UNIFIED_DELIVERY_DRIVERS',
  GET_TENDERS = 'GET_TENDERS',
  GET_TENDER_ACTIVE = 'GET_TENDER_ACTIVE',
  GET_COSTS = 'GET_COSTS',
  GET_COST = 'GET_COST',
  GET_DELIVERY_DRIVERS = 'GET_DELIVERY_DRIVERS',
  GET_GEOKEY = 'GET_GEOKEY',
}

export const BaseEventSchema = z.object({
  operation: z.enum([
    OperationEnum.GET_TENDERS,
    OperationEnum.GET_TENDER_ACTIVE,
    OperationEnum.GET_COSTS,
    OperationEnum.GET_COST,
    OperationEnum.GET_DELIVERY_DRIVERS,
    OperationEnum.GET_GEOKEY,
    OperationEnum.GET_UNIFIED_DELIVERY_DRIVERS,
  ]),
});

export const TenderActiveEventSchema = BaseEventSchema.extend({
  operation: z.literal(OperationEnum.GET_TENDER_ACTIVE),
});

export const TendersEventSchema = BaseEventSchema.extend({
  operation: z.literal(OperationEnum.GET_TENDERS),
  page: z.number().min(1),
  size: z.number().min(1),
  from: z.string().datetime().optional(),
  to: z.string().datetime().optional(),
});

export const CostsEventSchema = BaseEventSchema.extend({
  operation: z.literal(OperationEnum.GET_COSTS),
  tenderId: z.string(),
  product: z.string().optional(),
  lot: z.string().optional(),
  zone: z.string().optional(),
  deliveryDriverId: z.string().optional(),
});

export const CostEventSchema = BaseEventSchema.extend({
  operation: z.literal(OperationEnum.GET_COST),
  tenderId: z.string(),
  product: z.string(),
  geokey: z.string(),
});

export const GeokeyEventSchema = BaseEventSchema.extend({
  operation: z.literal(OperationEnum.GET_GEOKEY),
  tenderId: z.string(),
  product: z.string(),
  geokey: z.string(),
});

export const DeliveryDriversEventSchema = BaseEventSchema.extend({
  operation: z.literal(OperationEnum.GET_DELIVERY_DRIVERS)
});

const UnifiedDeliveryDriverRequest = z.object({
  geoKey: z.string(),
  product: z.string(),
});

export const UnifiedDeliveryDriversSchema = z.object({
  operation: z.literal(OperationEnum.GET_UNIFIED_DELIVERY_DRIVERS),
  tenderId: z.string(),
  requests: z.array(UnifiedDeliveryDriverRequest)
});

// types union
export const EventSchema = z.union([
  TendersEventSchema,
  TenderActiveEventSchema,
  CostsEventSchema,
  CostEventSchema,
  DeliveryDriversEventSchema,
  GeokeyEventSchema,
  UnifiedDeliveryDriversSchema,
]);

export type TendersEvent = z.infer<typeof TendersEventSchema>;
export type TenderActiveEvent = z.infer<typeof TenderActiveEventSchema>;
export type CostsEvent = z.infer<typeof CostsEventSchema>;
export type CostEvent = z.infer<typeof CostEventSchema>;
export type DeliveryDriversEvent = z.infer<typeof DeliveryDriversEventSchema>;
export type UnifiedDeliveryDriversEvent = z.infer<typeof UnifiedDeliveryDriversSchema>;
export type GeokeyEvent = z.infer<typeof GeokeyEventSchema>;
export type Event = z.infer<typeof EventSchema>;
export type UnifiedDeliveryDriverRequestType = z.infer<typeof UnifiedDeliveryDriverRequest>;
