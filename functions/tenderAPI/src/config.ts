/**
 * Retrieves the value of an environment variable, returning a default value if it is not set.
 *
 * @param env - The name of the environment variable to retrieve.
 * @param defaultValue - An optional default value to return if the environment variable is not set.
 *
 * @returns The value of the environment variable or the default value if it is not defined.
 */
const getEnvironmentVariable = (
  env: string,
  defaultValue: string | undefined = undefined
) => {
  const value = process.env[env];
  if (value === undefined) {
    return defaultValue;
  }
  return value;
};

export const UNCOVERED_FLAG = 'UNCOVERED';

export const PN_TENDER_TABLE_NAME = getEnvironmentVariable(
  'AWS_DYNAMODBPAPERCHANNELTENDERTABLE',
  'pn-PaperChannelTender'
);
export const PN_COST_TABLE_NAME = getEnvironmentVariable(
  'AWS_DYNAMODBPAPERCHANNELCOSTTABLE',
  'pn-PaperChannelCost'
);
export const PN_GEOKEY_TABLE_NAME = getEnvironmentVariable(
  'AWS_DYNAMODBPAPERCHANNELGEOKEYTABLE',
  'pn-PaperChannelGeokey'
);

export const PN_DELIVERY_DRIVER_TABLE_NAME = getEnvironmentVariable(
  'AWS_DYNAMODBPAPERCHANNELDELIVERYDRIVERTABLE',
  'pn-PaperChannelDeliveryDriver'
);