import {
  CostEventSchema,
  CostsEventSchema, DeliveryDriversEventSchema,
  Event,
  EventSchema,
  TenderActiveEventSchema,
  TendersEventSchema,
} from '../types/schema-request-types';

const validatorEvent = (event: unknown): Event => {
  const parsedEvent = EventSchema.safeParse(event);

  if (!parsedEvent.success) {
    //console.error("Invalid event data:", parsedEvent.error);
    throw new Error("Invalid event data");
  }

  if (TendersEventSchema.safeParse(parsedEvent.data).success) {
    return TendersEventSchema.parse(parsedEvent.data)
  }
  if (TenderActiveEventSchema.safeParse(parsedEvent.data).success){
    return TenderActiveEventSchema.parse(parsedEvent.data)
  }
   if (CostsEventSchema.safeParse(parsedEvent.data).success) {
    return CostsEventSchema.parse(parsedEvent.data);
    }
   if (CostEventSchema.safeParse(parsedEvent.data).success) {
    return CostEventSchema.parse(parsedEvent.data);
  }
  if (DeliveryDriversEventSchema.safeParse(parsedEvent.data).success) {
    return DeliveryDriversEventSchema.parse(parsedEvent.data);
  }

  throw new Error("Unknown event Type")
}

export {
  validatorEvent
};