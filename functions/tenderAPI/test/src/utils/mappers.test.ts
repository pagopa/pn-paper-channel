import { toPageMapper } from '../../../src/utils/mappers';
import { Page } from '../../../src/types/model-types';

describe('Mappers test', () => {
  const mockContent = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17];

  test('toPageMapper_whenFirstPage_shouldReturnCorrectPages', () => {
    // Arrange
    const content: number[] = mockContent;
    const assertContent = mockContent.slice(0, 10);

    const totalElements = 17;
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
      content: assertContent,
      totalElements,
      totalPages: 2,
      number: pageNumber,
      size: pageSize,
      isFirstPage: true,
      isLastPage: false,
    } as Page<number>);
  });

  test('toPageMapper_whenMiddlePage_shouldReturnCorrectPages', () => {
    // Arrange
    const content: number[] = mockContent;
    const assertContent = mockContent.slice(5, 10)

    const totalElements = 17;
    const pageNumber = 2;
    const pageSize = 5;

    // Act
    const pageResult = toPageMapper(
      content,
      totalElements,
      pageNumber,
      pageSize
    );

    // Assert
    expect(pageResult).toEqual({
      content: assertContent,
      totalElements,
      totalPages: 4,
      number: pageNumber,
      size: pageSize,
      isFirstPage: false,
      isLastPage: false,
    } as Page<number>);
  });

  test('toPageMapper_whenEndPage_shouldReturnCorrectPages', () => {
    // Arrange
    const content: number[] = mockContent;
    const assertContent = mockContent.slice(10, 17)

    const totalElements = 17;
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
      content: assertContent,
      totalElements,
      totalPages: 2,
      number: pageNumber,
      size: pageSize,
      isFirstPage: false,
      isLastPage: true,
    } as Page<number>);
  });
});
