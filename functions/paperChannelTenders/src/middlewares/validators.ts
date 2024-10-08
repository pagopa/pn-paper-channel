import {
  CostEventSchema,
  CostsEventSchema, DeliveryDriversEventSchema,
  Event,
  TenderActiveEventSchema,
  TendersEventSchema,
} from '../types/schema-request-types';



/**
 * Validates an incoming event against a set of predefined schemas.
 *
 * This function attempts to parse the provided event using a series of schemas.
 * It iterates through each schema, and if the event successfully matches one,
 * it returns the validated event data. If none of the schemas match, it throws
 * an error indicating that the event type is unknown.
 *
 * @param event - The event to validate, which can be of any type until validated.
 * @returns The validated event data of type Event if successful.
 *
 * @throws Will throw an error if the event does not match any of the predefined schemas.
 */
const validatorEvent = (event: unknown): Event => {

  const eventSchemas = [
    TendersEventSchema,
    TenderActiveEventSchema,
    CostsEventSchema,
    CostEventSchema,
    DeliveryDriversEventSchema,
  ];

  for (const schema of eventSchemas) {
    const result = schema.safeParse(event);
    if (result.success) {
      return result.data;
    }
  }

  throw new Error("Unknown event type");
}

export {
  validatorEvent
};