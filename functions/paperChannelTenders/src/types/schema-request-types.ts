import { z } from 'zod';

export enum OperationEnum {
  GET_TENDERS = 'GET_TENDERS',
  GET_TENDER_ACTIVE = 'GET_TENDER_ACTIVE',
  GET_COSTS = 'GET_COSTS',
  GET_COST = 'GET_COST',
  GET_DELIVERY_DRIVERS = 'GET_DELIVERY_DRIVERS'
}

export const BaseEventSchema = z.object({
  operation: z.enum([OperationEnum.GET_TENDERS, OperationEnum.GET_TENDER_ACTIVE, OperationEnum.GET_COSTS, OperationEnum.GET_COST, OperationEnum.GET_DELIVERY_DRIVERS]),
});

export const TenderActiveEventSchema = BaseEventSchema.extend({
  operation: z.literal(OperationEnum.GET_TENDER_ACTIVE),
});

export const TendersEventSchema = BaseEventSchema.extend({
  operation: z.literal(OperationEnum.GET_TENDERS),
  page: z.number().min(1),
  size: z.number().min(1),
  from: z.date().optional(),
  to: z.date().optional()
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

export const DeliveryDriversEventSchema = BaseEventSchema.extend({
  operation: z.literal(OperationEnum.GET_DELIVERY_DRIVERS),
  from: z.date(),
  to: z.date().optional(),
});

// types union
export const EventSchema = z.union([TendersEventSchema,TenderActiveEventSchema, CostsEventSchema, CostEventSchema, DeliveryDriversEventSchema]);

export type TendersEvent = z.infer<typeof TendersEventSchema>;
export type TenderActiveEvent = z.infer<typeof TenderActiveEventSchema>;
export type CostsEvent = z.infer<typeof CostsEventSchema>;
export type CostEvent = z.infer<typeof CostEventSchema>;
export type DeliveryDriversEvent = z.infer<typeof DeliveryDriversEventSchema>;
export type Event = z.infer<typeof EventSchema>;
