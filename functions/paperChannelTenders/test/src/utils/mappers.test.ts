import { PaperChannelTender } from '../../../src/types/dynamo-types';
import { toPageMapper } from '../../../src/utils/mappers';
import { Page } from '../../../src/types/model-types';

describe("Mappers test", () => {
  const mockContent = [{
    tenderId: '12344',
    activationDate: '2023-01-01',
    tenderName: 'Test tenders',
    vat: 0,
    nonDeductibleVat: 0,
    pagePrice: 0,
    basePriceAR: 0,
    basePriceRS: 0,
    basePrice890: 0,
    fee: 0,
    createdAt: '',
  }]

  test("toPageMapper_whenFirstPage_shouldReturnCorrectPages", () => {
    // Arrange
    const content: PaperChannelTender[] = mockContent;

    const totalElements = 100;
    const pageNumber = 1;
    const pageSize = 10;

    // Act
    const pageResult = toPageMapper(content, totalElements, pageNumber, pageSize);

    // Assert

    expect(pageResult).toEqual({
      content,
      totalElements,
      totalPages: 10,
      number: pageNumber,
      size: pageSize,
      isFirstPage: true,
      isLastPage: false,
    } as Page<PaperChannelTender>)

  })

  test("toPageMapper_whenMiddlePage_shouldReturnCorrectPages", () => {
    // Arrange
    const content: PaperChannelTender[] = mockContent;

    const totalElements = 100;
    const pageNumber = 2;
    const pageSize = 10;

    // Act
    const pageResult = toPageMapper(content, totalElements, pageNumber, pageSize);

    // Assert
    expect(pageResult).toEqual({
      content,
      totalElements,
      totalPages: 10,
      number: pageNumber,
      size: pageSize,
      isFirstPage: false,
      isLastPage: false,
    } as Page<PaperChannelTender>)

  })

  test("toPageMapper_whenEndPage_shouldReturnCorrectPages", () => {
    // Arrange
    const content: PaperChannelTender[] = mockContent;

    const totalElements = 100;
    const pageNumber = 10;
    const pageSize = 10;

    // Act
    const pageResult = toPageMapper(content, totalElements, pageNumber, pageSize);

    // Assert
    expect(pageResult).toEqual({
      content,
      totalElements,
      totalPages: 10,
      number: pageNumber,
      size: pageSize,
      isFirstPage: false,
      isLastPage: true,
    } as Page<PaperChannelTender>)

  })

})