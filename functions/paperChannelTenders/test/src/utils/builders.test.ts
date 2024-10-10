import {
  buildCostSortKey,
  buildGeokeyPartitionKey
} from '../../../src/utils/builders';

describe("Builders test", () => {

  test("buildGeokeyPartitionKey_shouldReturnCorrectPartitionKey", () => {
    // Arrange
    const tenderId = "1234";
    const product = "AR";
    const geokey = "86950";

    // Act
    const result = buildGeokeyPartitionKey(tenderId, product, geokey);

    expect(result).toEqual(tenderId+"#"+product+"#"+geokey);
  });

  test("buildCostSortKey_shouldReturnCorrectSortKey", () => {
    // Arrange
    const product = "AR";
    const lot = "LOT1";
    const zone = "EU"

    // Act
    const result = buildCostSortKey(product, lot, zone);

    expect(result).toEqual(product+"#"+lot+"#"+zone);
  });

});