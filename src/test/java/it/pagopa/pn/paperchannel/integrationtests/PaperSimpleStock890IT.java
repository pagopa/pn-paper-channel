package it.pagopa.pn.paperchannel.integrationtests;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.StatusCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest(properties = {
    "pn.paper-channel.enable-simple-890-flow=true",
    "pn.paper-channel.complex-refinement-codes=RECAG007C"
})
class PaperSimpleStock890IT extends BasePaperStock890IT {

    /* RECAG005C CASES */

    @Test
    void test_simple_890_within_10days_stock_RECAG005C(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005A = buildStatusUpdateDto("RECAG005A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005B = buildStatusUpdateDto("RECAG005B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012", null, null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG005B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG005A, null);
        generateEvent(RECAG005B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG005C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_within_10days_stock_RECAG005C_without_RECAG011A(){

        // Given
        SingleStatusUpdateDto RECAG005A = buildStatusUpdateDto("RECAG005A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005B = buildStatusUpdateDto("RECAG005B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012", null, null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG005B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG005A, null);
        generateEvent(RECAG005B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG005C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_within_10days_stock_RECAG005C_with_RECAG011B(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG005A = buildStatusUpdateDto("RECAG005A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005B = buildStatusUpdateDto("RECAG005B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012", null, null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG005B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG005A, null);
        generateEvent(RECAG005B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG005C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_after_10days_stock_RECAG005C(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG005A = buildStatusUpdateDto("RECAG005A",null, Instant.parse("2024-01-15T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005B = buildStatusUpdateDto("RECAG005B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG005B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG005A, null);
        generateEvent(RECAG005B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG005C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }


    @Test
    void test_simple_890_after_10days_stock_RECAG005C_without_RECAG011A(){
        // Given
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG005A = buildStatusUpdateDto("RECAG005A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005B = buildStatusUpdateDto("RECAG005B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG005B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011B, null);
        generateEvent(RECAG005A, null);
        generateEvent(RECAG005B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG005C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_after_10days_stock_RECAG005C_without_RECAG011B(){
        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005A = buildStatusUpdateDto("RECAG005A",null, Instant.parse("2024-01-15T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG005B = buildStatusUpdateDto("RECAG005B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG005B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG005A, null);
        generateEvent(RECAG005B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG005C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_stock_RECAG012_RECAG011B_RECAG005C(){
        // Given
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",Arrays.asList("23L","CAD"), null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG005C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_stock_RECAG012_RECAG0011B_RECAG005C_singleAttachEvent(){
        // Given

        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG005C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG011BCad, null);
        generateEvent(RECAG005C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_stock_RECAG005C_out_of_order(){
        // Given

        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG005C, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG011BCad, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_stock_RECAG005C_missing_23L(){
        // Given

        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of();

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG005C, null);
        generateEvent(RECAG011BCad, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.FALSE);
    }

    @Test
    void test_simple_890_stock_RECAG005C_missing_RECAG012(){
        // Given

        SingleStatusUpdateDto RECAG005C = buildStatusUpdateDto("RECAG005C",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS
        );

        // When
        generateEvent(RECAG005C, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG011BCad, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.FALSE);
    }

    /* RECAG006C CASES */

    @Test
    void test_simple_890_within_10days_stock_RECAG006C(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006A = buildStatusUpdateDto("RECAG006A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006B = buildStatusUpdateDto("RECAG006B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012", null, null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG006B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG006A, null);
        generateEvent(RECAG006B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG006C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_within_10days_stock_RECAG006C_without_RECAG011A(){

        // Given
        SingleStatusUpdateDto RECAG006A = buildStatusUpdateDto("RECAG006A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006B = buildStatusUpdateDto("RECAG006B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012", null, null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG006B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG006A, null);
        generateEvent(RECAG006B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG006C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_within_10days_stock_RECAG006C_with_RECAG011B(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG006A = buildStatusUpdateDto("RECAG006A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006B = buildStatusUpdateDto("RECAG006B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012", null, null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG006B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG006A, null);
        generateEvent(RECAG006B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG006C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_after_10days_stock_RECAG006C(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG006A = buildStatusUpdateDto("RECAG006A",null, Instant.parse("2024-01-15T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006B = buildStatusUpdateDto("RECAG006B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG006B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG006A, null);
        generateEvent(RECAG006B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG006C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }


    @Test
    void test_simple_890_after_10days_stock_RECAG006C_without_RECAG011A(){
        // Given
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG006A = buildStatusUpdateDto("RECAG006A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006B = buildStatusUpdateDto("RECAG006B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG006B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011B, null);
        generateEvent(RECAG006A, null);
        generateEvent(RECAG006B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG006C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_after_10days_stock_RECAG006C_without_RECAG011B(){
        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006A = buildStatusUpdateDto("RECAG006A",null, Instant.parse("2024-01-15T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG006B = buildStatusUpdateDto("RECAG006B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG006B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG006A, null);
        generateEvent(RECAG006B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG006C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_stock_RECAG012_RECAG011B_RECAG006C(){
        // Given
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",Arrays.asList("23L","CAD"), null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG006C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_stock_RECAG012_RECAG0011B_RECAG006C_singleAttachEvent(){
        // Given

        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG006C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG011BCad, null);
        generateEvent(RECAG006C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_stock_RECAG006C_out_of_order(){
        // Given

        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG006C, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG011BCad, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_stock_RECAG006C_missing_23L(){
        // Given

        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of();

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG006C, null);
        generateEvent(RECAG011BCad, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.FALSE);
    }

    @Test
    void test_simple_890_stock_RECAG006C_missing_RECAG012(){
        // Given

        SingleStatusUpdateDto RECAG006C = buildStatusUpdateDto("RECAG006C",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS
        );

        // When
        generateEvent(RECAG006C, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG011BCad, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.FALSE);
    }

    /* RECAG007C CASES - managed with complex handler */

    @Test
    void test_simple_890_within_10days_stock_RECAG007C(){
        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007A = buildStatusUpdateDto("RECAG007A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007B = buildStatusUpdateDto("RECAG007B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG007B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG007A, null);
        generateEvent(RECAG007B, null);
        generateEvent(RECAG007C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_within_10days_stock_RECAG007C_without_RECAG011A(){

        // Given
        SingleStatusUpdateDto RECAG007A = buildStatusUpdateDto("RECAG007A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007B = buildStatusUpdateDto("RECAG007B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG007B", StatusCodeEnum.PROGRESS
        );

        // When
        generateEvent(RECAG007A, null);
        generateEvent(RECAG007B, null);

        // Expect exception because RECAG011A
        generateEvent(RECAG007C, PnGenericException.class);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.FALSE);
    }

    @Test
    void test_simple_890_within_10days_stock_RECAG007C_with_RECAG011B(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG007A = buildStatusUpdateDto("RECAG007A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007B = buildStatusUpdateDto("RECAG007B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG007B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG007A, null);
        generateEvent(RECAG007B, null);
        generateEvent(RECAG007C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_after_10days_stock_RECAG007C(){

        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG007A = buildStatusUpdateDto("RECAG007A",null, Instant.parse("2024-01-15T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007B = buildStatusUpdateDto("RECAG007B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG007B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG007A, null);
        generateEvent(RECAG007B, null);
        generateEvent(RECAG007C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }


    @Test
    void test_simple_890_after_10days_stock_RECAG007C_without_RECAG011A(){
        // Given
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG007A = buildStatusUpdateDto("RECAG007A",null, Instant.parse("2024-01-04T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007B = buildStatusUpdateDto("RECAG007B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG007B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011B, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG007A, null);
        generateEvent(RECAG007B, null);
        generateEvent(RECAG007C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_after_10days_stock_RECAG007C_without_RECAG011B(){
        // Given
        SingleStatusUpdateDto RECAG011A = buildStatusUpdateDto("RECAG011A",null, Instant.parse("2024-01-01T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007A = buildStatusUpdateDto("RECAG007A",null, Instant.parse("2024-01-15T00:00:00.000Z"));
        SingleStatusUpdateDto RECAG007B = buildStatusUpdateDto("RECAG007B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011A", StatusCodeEnum.PROGRESS,
            "RECAG007B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.PROGRESS,
            "PNAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011A, null);
        generateEvent(RECAG007A, null);
        generateEvent(RECAG007B, null);
        generateEvent(RECAG007C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_stock_RECAG012_RECAG011B_RECAG007C(){
        // Given
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",Arrays.asList("23L","CAD"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG007C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_stock_RECAG012_RECAG0011B_RECAG007C_singleAttachEvent(){
        // Given

        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);
        SingleStatusUpdateDto RECAG007C = buildStatusUpdateDto("RECAG007C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG007C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG011BCad, null);
        generateEvent(RECAG007C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    /* RECAG008C CASES */

    @Test
    void test_simple_890_stock_RECAG012_RECAG011B_RECAG008C(){
        // Given
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B = buildStatusUpdateDto("RECAG011B",Arrays.asList("23L","CAD"), null);
        SingleStatusUpdateDto RECAG008C = buildStatusUpdateDto("RECAG008C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG008C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B, null);
        generateEvent(RECAG008C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_stock_RECAG011B_RECAG012_RECAG008B_RECAG008C(){
        // Given

        SingleStatusUpdateDto RECAG011BArcad = buildStatusUpdateDto("RECAG011B", List.of("ARCAD"), null);
        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG008A = buildStatusUpdateDto("RECAG008A",null, null);
        SingleStatusUpdateDto RECAG008B = buildStatusUpdateDto("RECAG008B",List.of("Plico"), null);
        SingleStatusUpdateDto RECAG008C = buildStatusUpdateDto("RECAG008C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG008B", StatusCodeEnum.PROGRESS,
            "RECAG008C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG011BArcad, null);
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG008A, null);
        generateEvent(RECAG008B, null);
        generateEvent(RECAG008C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }

    @Test
    void test_simple_890_stock_RECAG012_RECAG0011B_RECAG008C_singleAttachEvent(){
        // Given

        SingleStatusUpdateDto RECAG012 = buildStatusUpdateDto("RECAG012",null, null);
        SingleStatusUpdateDto RECAG011B23L = buildStatusUpdateDto("RECAG011B", List.of("23L"), null);
        SingleStatusUpdateDto RECAG011BCad = buildStatusUpdateDto("RECAG011B", List.of("CAD"), null);
        SingleStatusUpdateDto RECAG008C = buildStatusUpdateDto("RECAG008C",null, null);

        Map<String, StatusCodeEnum> assertionLookupTable = Map.of(
            "RECAG011B", StatusCodeEnum.PROGRESS,
            "RECAG008C", StatusCodeEnum.PROGRESS,
            "RECAG012", StatusCodeEnum.OK
        );

        // When
        generateEvent(RECAG012, null);
        generateEvent(RECAG011B23L, null);
        generateEvent(RECAG011BCad, null);
        generateEvent(RECAG008C, null);

        // Then
        checkFlowCorrectness(assertionLookupTable, Boolean.TRUE);
    }
}
