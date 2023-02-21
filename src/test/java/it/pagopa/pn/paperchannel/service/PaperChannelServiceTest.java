package it.pagopa.pn.paperchannel.service;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.dao.ExcelDAO;
import it.pagopa.pn.paperchannel.dao.model.DeliveriesData;
import it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.FileDownloadDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.rest.v1.dto.*;
import it.pagopa.pn.paperchannel.s3.S3Bucket;
import it.pagopa.pn.paperchannel.service.impl.PaperChannelServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

@Slf4j
class PaperChannelServiceTest extends BaseTest {
    private static final String TENDER_CODE_WITHOUT_DRIVER = "TENDER_WITHOUT_DRIVER";
    private static final String TENDER_CODE_WITH_DRIVER = "TENDER_CODE_WITH_DRIVER";
    private static final String DRIVER_CODE_WITHOUT_DRIVER = "DRIVER_WITHOUT_DRIVER";
    private static final String DRIVER_CODE_1_OK = "CFGRTD";
    @Mock
    private CostDAO costDAO;
    @Mock
    private DeliveryDriverDAO deliveryDriverDAO;
    @Mock
    private TenderDAO tenderDAO;
    @Mock
    private ExcelDAO<DeliveriesData> excelDAO;
    @Mock
    private FileDownloadDAO fileDownloadDAO;
    @Mock
    private S3Bucket s3Bucket;

    private CostDTO costWithoutCapAndZone;
    private CostDTO costNational;
    private CostDTO costNationalDuplicate;
    private CostDTO costInternational;
    private List<PnCost> costs;
    private PnDeliveryDriver driverOK;

    @InjectMocks
    private PaperChannelServiceImpl paperChannelService;


    @BeforeEach
    public void setUp(){
        initModel();
        initMock();
    }


    @Test
    void createOrUpdateCostTestBadlyContent(){
        //TEST CASE BADLY CONTENT REQUEST
        StepVerifier.create(
                this.paperChannelService.createOrUpdateCost(
                        "TENDER", "Delivery", costWithoutCapAndZone
                )
        ).expectErrorMatches((ex) -> {
            Assertions.assertTrue(ex instanceof PnGenericException);
            Assertions.assertEquals(ExceptionTypeEnum.COST_BADLY_CONTENT, ((PnGenericException) ex).getExceptionType());
            return false;
        }).verify();

        //TEST CASE DRIVER NOT EXIST
        StepVerifier.create(
                this.paperChannelService.createOrUpdateCost(
                        TENDER_CODE_WITHOUT_DRIVER, DRIVER_CODE_WITHOUT_DRIVER, costNational
                )
        ).expectError(PnGenericException.class).verify();

        FSUResponseDTO result = new FSUResponseDTO();

        FSUResponseDTO response = this.paperChannelService.getDetailsFSU(
                TENDER_CODE_WITHOUT_DRIVER
        ).block();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(TENDER_CODE_WITHOUT_DRIVER, response.getFsu().getFiscalCode());

        //TEST CASE CREATE COST INTERNATIONAL
        PnCost firstCost = new PnCost();
        firstCost.setZone("ZONA_1");
        firstCost.setBasePrice(2.23F);
        firstCost.setPagePrice(2.23F);
        firstCost.setProductType(ProductTypeEnumDto._890.getValue());
        Mockito.when(this.costDAO.createOrUpdate(firstCost)).thenReturn(Mono.just(firstCost));
        this.paperChannelService.createOrUpdateCost(
                TENDER_CODE_WITH_DRIVER, DRIVER_CODE_1_OK, costInternational
        ).block();

        // TEST CASE OK
        Mockito.when(this.costDAO.createOrUpdate(Mockito.any())).thenReturn(Mono.just(firstCost));
        this.paperChannelService.createOrUpdateCost(
                TENDER_CODE_WITH_DRIVER, DRIVER_CODE_1_OK, costNational
        ).block();

        // TEST CASE CAP DUPLICATE
        StepVerifier.create(
                this.paperChannelService.createOrUpdateCost(
                        TENDER_CODE_WITH_DRIVER, DRIVER_CODE_1_OK, costNationalDuplicate
                )
        ).expectError(PnGenericException.class).verify();

    }

    private void initModel(){
        costWithoutCapAndZone = new CostDTO();
        costWithoutCapAndZone.setZone(null);
        costWithoutCapAndZone.setCap(null);
        costWithoutCapAndZone.setPrice(2.22F);
        costWithoutCapAndZone.setPriceAdditional(2.22F);
        driverOK = new PnDeliveryDriver();
        driverOK.setTenderCode(TENDER_CODE_WITH_DRIVER);
        driverOK.setUniqueCode(DRIVER_CODE_1_OK);

        costNational = new CostDTO();
        costNational.setProductType(ProductTypeEnumDto.AR);
        costNational.setZone(null);
        costNational.setCap(List.of("20123", "20100"));
        costNational.setPrice(2.22F);
        costNational.setPriceAdditional(2.22F);

        costInternational = new CostDTO();
        costInternational.setProductType(ProductTypeEnumDto._890);
        costInternational.setZone(null);
        costInternational.setZone(InternationalZoneEnum._1);
        costInternational.setPrice(2.22F);
        costInternational.setPriceAdditional(2.22F);

        PnCost pnCost1 = new PnCost();
        pnCost1.setCap(List.of("10000", "10300", "90000"));
        pnCost1.setProductType("AR");
        PnCost pnCost2 = new PnCost();
        pnCost2.setCap(List.of("20000", "20600", "67000"));
        pnCost2.setProductType("AR");
        PnCost pnCost3 = new PnCost();
        pnCost3.setCap(List.of("70000", "80300", "55000"));
        pnCost3.setProductType("AR");
        PnCost pnCost4 = new PnCost();
        pnCost4.setZone("ZONA_1");
        pnCost4.setProductType("AR");
        this.costs = List.of(pnCost1, pnCost2, pnCost3, pnCost4);

        costNationalDuplicate = new CostDTO();
        costNationalDuplicate.setProductType(ProductTypeEnumDto.AR);
        costNationalDuplicate.setZone(null);
        costNationalDuplicate.setCap(List.of("20000", "20100"));
        costNationalDuplicate.setPrice(2.22F);
        costNationalDuplicate.setPriceAdditional(2.22F);
    }

    private void initMock(){
        Mockito.when(this.deliveryDriverDAO.getDeliveryDriver(TENDER_CODE_WITHOUT_DRIVER, DRIVER_CODE_WITHOUT_DRIVER))
                .thenReturn(Mono.empty());

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriver(TENDER_CODE_WITH_DRIVER, DRIVER_CODE_1_OK))
                .thenReturn(Mono.just(driverOK));

        Mockito.when(
            this.costDAO.findAllFromTenderAndProductTypeAndExcludedUUID(
                    TENDER_CODE_WITH_DRIVER, ProductTypeEnumDto._890.getValue(), null
            )
        ).thenReturn(Flux.empty());

        Mockito.when(
                this.costDAO.findAllFromTenderAndProductTypeAndExcludedUUID(
                        TENDER_CODE_WITH_DRIVER, ProductTypeEnumDto.AR.getValue(), null
                )
        ).thenReturn( Flux.fromStream(this.costs.stream()));
    }

}
