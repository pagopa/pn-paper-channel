package it.pagopa.pn.paperchannel.exception;

import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.PaperEvent;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;


class RestExceptionHandlerTest {
    @Spy
    private RestExceptionHandler restExceptionHandler;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void handleResponseEntityExceptionTest(){
        PnGenericException pnGenericException = new PnGenericException(MAPPER_ERROR, MAPPER_ERROR.getMessage());
        restExceptionHandler.handleResponseEntityException(pnGenericException)
                .map(responseEntity -> {
                    Assertions.assertEquals(MAPPER_ERROR.getTitle(), responseEntity.getBody().getTitle());
                    Assertions.assertEquals(MAPPER_ERROR.getMessage(), responseEntity.getBody().getDetail());
                    return Mono.empty();
                })
                .block();
    }

    @Test
    void handlePnPaperEventExceptionTest(){
        String requestId = "MOCK-SUCC-WKHU-202209-P-1_send_digital_domicile0_source_PLATFORM_attempt_1";
        String statusCode = StatusDeliveryEnum.IN_PROCESSING.getCode();
        String statusDetail = StatusDeliveryEnum.IN_PROCESSING.getDescription();
        Date statusDate = Date.from(Instant.now());

        PaperEvent paperEvent = new PaperEvent();
        paperEvent.setRequestId(requestId);
        paperEvent.setStatusDateTime(statusDate);
        paperEvent.setStatusDetail(statusDetail);
        paperEvent.setStatusCode(statusCode);

        PnPaperEventException pnPaperEventException = new PnPaperEventException(paperEvent.getRequestId());
        restExceptionHandler.handlePnPaperEventException(pnPaperEventException)
                .map(responseEntity -> {
                        Assertions.assertFalse(responseEntity.hasBody());
                    return Mono.empty();
                })
                .block();
    }

    @Test
    void handlePnInputValidatorExceptionTest(){
        List<String> errors = new ArrayList<>();
        errors.add("error");
        PnInputValidatorException pnInputValidatorException = new PnInputValidatorException(DIFFERENT_DATA_RESULT, DIFFERENT_DATA_RESULT.getMessage(), HttpStatus.CONFLICT, errors);

        restExceptionHandler.handlePnInputValidatorException(pnInputValidatorException)
                .map(responseEntity -> {
                    Assertions.assertEquals(DIFFERENT_DATA_RESULT.getTitle(), responseEntity.getBody().getTitle());
                    Assertions.assertEquals(DIFFERENT_DATA_RESULT.getMessage(), responseEntity.getBody().getDetail());
                    Assertions.assertEquals(HttpStatus.CONFLICT.value(), responseEntity.getBody().getStatus());

                    Assertions.assertEquals("PN_DIFFERENT_DATA", responseEntity.getBody().getErrors().get(0).getCode());
                    Assertions.assertEquals("Trovato valore differente a db", responseEntity.getBody().getErrors().get(0).getDetail());
                    return Mono.empty();
                })
                .block();
    }

    @Test
    void handlePnInputValidatorExcelExceptionTest(){
        List<PnExcelValidatorException.ErrorCell> errors = new ArrayList<>();
        errors.add(new PnExcelValidatorException.ErrorCell(0, 1, "CAP", INVALID_CAP_FSU.getMessage()));
        PnExcelValidatorException pnExcelValidatorException = new PnExcelValidatorException(INVALID_CAP_FSU, errors);

        restExceptionHandler.handlePnInputValidatorException(pnExcelValidatorException)
                .map(responseEntity -> {
                    Assertions.assertEquals(INVALID_CAP_FSU.getTitle(), responseEntity.getBody().getTitle());
                    Assertions.assertEquals(INVALID_CAP_FSU.getMessage(), responseEntity.getBody().getDetail());
                    Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getStatusCode().value());

                    Assertions.assertEquals("0", responseEntity.getBody().getErrors().get(0).getCode());
                    Assertions.assertEquals("1", responseEntity.getBody().getErrors().get(0).getDetail());
                    Assertions.assertEquals(INVALID_CAP_FSU.getMessage(), responseEntity.getBody().getErrors().get(0).getElement());
                    return Mono.empty();
                })
                .block();
    }

    private void initialize() {
        restExceptionHandler = new RestExceptionHandler();
    }
}
