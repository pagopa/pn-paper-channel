import { z } from 'zod';


const BaseEventSchema = z.object({
  operation: z.enum(['GET_TENDERS', 'GET_TENDER_ACTIVE', 'GET_COSTS', 'GET_COST', 'GET_DELIVERY_DRIVERS']),
});

const TenderActiveEventSchema = BaseEventSchema.extend({
  operation: z.literal("GET_TENDER_ACTIVE"),
});

const TendersEventSchema = BaseEventSchema.extend({
  operation: z.literal("GET_TENDERS"),
  page: z.number().min(1),
  size: z.number().min(1),
  from: z.date().optional(),
  to: z.date().optional()
});


const CostsEventSchema = BaseEventSchema.extend({
  operation: z.literal("GET_COSTS"),
  tenderId: z.string(),
  product: z.string().optional(),
  zone: z.string().optional(),
  deliveryDriverId: z.string().optional(),
});

const CostEventSchema = BaseEventSchema.extend({
  operation: z.literal("GET_COST"),
  tenderId: z.string(),
  product: z.string().optional(),
  geokey: z.string().optional(),
});

const DeliveryDriversEventSchema = BaseEventSchema.extend({
  operation: z.literal("GET_DELIVERY_DRIVERS"),
  from: z.date(),
  to: z.date().optional(),
});

// Unione dei tipi
const EventSchema = z.union([TendersEventSchema,TenderActiveEventSchema, CostsEventSchema, CostEventSchema, DeliveryDriversEventSchema]);

type TendersEvent = z.infer<typeof TendersEventSchema>;
type TenderActiveEvent = z.infer<typeof TenderActiveEventSchema>;
type CostsEvent = z.infer<typeof CostsEventSchema>;
type CostEvent = z.infer<typeof CostEventSchema>;
type DeliveryDriversEvent = z.infer<typeof DeliveryDriversEventSchema>;
type Event = z.infer<typeof EventSchema>;

export {
  EventSchema,
  TendersEventSchema,
  TenderActiveEventSchema,
  CostsEventSchema,
  CostEventSchema,
  DeliveryDriversEventSchema,
  TendersEvent,
  TenderActiveEvent,
  CostsEvent,
  CostEvent,
  DeliveryDriversEvent,
  Event
}