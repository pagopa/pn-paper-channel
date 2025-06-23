package it.pagopa.pn.paperchannel.integrationtests;

import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.PaperProgressStatusEventDto;
import it.pagopa.pn.paperchannel.generated.openapi.msclient.pnextchannel.v1.dto.SingleStatusUpdateDto;
import it.pagopa.pn.paperchannel.middleware.db.dao.AddressDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.PaperRequestErrorDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnAddress;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryRequest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnRequestError;
import it.pagopa.pn.paperchannel.service.PaperResultAsyncService;
import it.pagopa.pn.paperchannel.utils.AddressTypeEnum;
import it.pagopa.pn.paperchannel.utils.DateUtils;
import it.pagopa.pn.paperchannel.utils.ExternalChannelCodeEnum;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class})
@ActiveProfiles("test")
class PaperNotRetryableTest extends BaseTest.WithOutLocalStackTest {

    @Autowired
    private PaperResultAsyncService paperResultAsyncService;

    @MockBean
    private RequestDeliveryDAO requestDeliveryDAO;

    @MockBean
    private AddressDAO mockAddressDAO;

    @MockBean
    private PaperRequestErrorDAO paperRequestErrorDAO;


    @ParameterizedTest
    @ValueSource(strings = {"P008", "P010", "P011", "P012", "P013", "P014"})
    void whenECReturnStatusP010ThenPushSendEventAndCreatePaperError(String statusCode) {

        PnDeliveryRequest pnDeliveryRequest = CommonUtils.createPnDeliveryRequest();

        PaperProgressStatusEventDto analogMail = CommonUtils.createSimpleAnalogMail();
        analogMail.setStatusCode(statusCode);
        analogMail.setProductType("AR");

        SingleStatusUpdateDto extChannelMessage = new SingleStatusUpdateDto();
        extChannelMessage.setAnalogMail(analogMail);


        PnDeliveryRequest afterSetForUpdate = CommonUtils.createPnDeliveryRequest();
        // KO
        afterSetForUpdate.setStatusDetail(ExternalChannelCodeEnum.getStatusCode(extChannelMessage.getAnalogMail().getStatusCode()));
        // P010
        afterSetForUpdate.setStatusCode(analogMail.getStatusCode());
        // AR - P010
        afterSetForUpdate.setStatusDescription(extChannelMessage.getAnalogMail().getProductType()
                .concat(" - ").concat(pnDeliveryRequest.getStatusCode()).concat(" - ").concat(extChannelMessage.getAnalogMail().getStatusDescription()));
        afterSetForUpdate.setStatusDate(DateUtils.formatDate(extChannelMessage.getAnalogMail().getStatusDateTime().toInstant()));

        when(requestDeliveryDAO.getByRequestId(anyString())).thenReturn(Mono.just(pnDeliveryRequest));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class))).thenReturn(Mono.just(afterSetForUpdate));
        when(requestDeliveryDAO.updateData(any(PnDeliveryRequest.class), anyBoolean())).thenReturn(Mono.just(afterSetForUpdate));
        when(paperRequestErrorDAO.created(any(PnRequestError.class))).thenReturn(Mono.just(new PnRequestError()));

        PnAddress pnAddress = new PnAddress();
        pnAddress.setTypology(AddressTypeEnum.RECEIVER_ADDRESS.name());
        pnAddress.setCity("Milan");
        pnAddress.setCap("");

        when(mockAddressDAO.findAllByRequestId(anyString())).thenReturn(Mono.just(List.of(pnAddress)));

        assertDoesNotThrow(() -> paperResultAsyncService.resultAsyncBackground(extChannelMessage, 0).block());
        verify(paperRequestErrorDAO).created(any(PnRequestError.class));


    }




}
