import { PaperChannelTender } from '../../../src/types/dynamo-types';
import { toPageMapper } from '../../../src/utils/mappers';
import { Page } from '../../../src/types/model-types';
import { tender } from '../config/model-mock';

describe('Mappers test', () => {
  const mockContent = [tender];

  test('toPageMapper_whenFirstPage_shouldReturnCorrectPages', () => {
    // Arrange
    const content: PaperChannelTender[] = mockContent;

    const totalElements = 100;
    const pageNumber = 1;
    const pageSize = 10;

    // Act
    const pageResult = toPageMapper(
      content,
      totalElements,
      pageNumber,
      pageSize
    );

    // Assert

    expect(pageResult).toEqual({
      content,
      totalElements,
      totalPages: 10,
      number: pageNumber,
      size: pageSize,
      isFirstPage: true,
      isLastPage: false,
    } as Page<PaperChannelTender>);
  });

  test('toPageMapper_whenMiddlePage_shouldReturnCorrectPages', () => {
    // Arrange
    const content: PaperChannelTender[] = mockContent;

    const totalElements = 100;
    const pageNumber = 2;
    const pageSize = 10;

    // Act
    const pageResult = toPageMapper(
      content,
      totalElements,
      pageNumber,
      pageSize
    );

    // Assert
    expect(pageResult).toEqual({
      content,
      totalElements,
      totalPages: 10,
      number: pageNumber,
      size: pageSize,
      isFirstPage: false,
      isLastPage: false,
    } as Page<PaperChannelTender>);
  });

  test('toPageMapper_whenEndPage_shouldReturnCorrectPages', () => {
    // Arrange
    const content: PaperChannelTender[] = mockContent;

    const totalElements = 100;
    const pageNumber = 10;
    const pageSize = 10;

    // Act
    const pageResult = toPageMapper(
      content,
      totalElements,
      pageNumber,
      pageSize
    );

    // Assert
    expect(pageResult).toEqual({
      content,
      totalElements,
      totalPages: 10,
      number: pageNumber,
      size: pageSize,
      isFirstPage: false,
      isLastPage: true,
    } as Page<PaperChannelTender>);
  });
});
