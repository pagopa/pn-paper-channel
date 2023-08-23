package it.pagopa.pn.paperchannel.service.tenders;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.InstanceCreator;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.paperchannel.mapper.CostMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.CostDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.DeliveryDriverDAO;
import it.pagopa.pn.paperchannel.middleware.db.dao.TenderDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnCost;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnDeliveryDriver;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.service.impl.PaperChannelServiceImpl;
import it.pagopa.pn.paperchannel.utils.Const;
import it.pagopa.pn.paperchannel.validator.CostValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CreateAndUpdateFromServiceTest {


    @InjectMocks
    private PaperChannelServiceImpl paperChannelService;
    @Mock
    private CostDAO costDAO;
    @Mock
    private DeliveryDriverDAO deliveryDriverDAO;
    @Mock
    private TenderDAO tenderDAO;

    @Spy
    private PnAuditLogBuilder pnAuditLogBuilder;
    private MockedStatic<CostMapper> costMapperMockedStatic;
    private MockedStatic<CostValidator> costValidatorMockedStatic;

    @AfterEach
    void after(){
        if (costMapperMockedStatic != null){
            costMapperMockedStatic.close();
        }
        if (costValidatorMockedStatic != null){
            costValidatorMockedStatic.close();
        }
    }

    @Test
    @DisplayName("whenChangeStatusTenderThatNotExistedThrowException")
    void updateStatusTenderThatNotExisted(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.CREATED);
        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.empty());
        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(TENDER_NOT_EXISTED, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenChangeStatusTenderInProgressOrEndedThrowException")
    void updateStatusTenderInProgressOrEnded(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.CREATED);
        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.ENDED.toString());

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));
        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(STATUS_NOT_VARIABLE, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();

        tender.setStatus(TenderDTO.StatusEnum.IN_PROGRESS.toString());

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));
        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(STATUS_NOT_VARIABLE, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenChangeStatusTenderAndExistedOtherTenderAlreadyConsolidatedForIntervalTimeThrowException")
    void updateStatusTenderAndExistedOtherTenderAlreadyConsolidated(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.VALIDATED);
        PnTender tender = InstanceCreator.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        Mockito.when(tenderDAO.getConsolidate(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(tender));

        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(CONSOLIDATE_ERROR, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenChangeStatusTenderWithoutFSUThrowException")
    void updateStatusTenderWithoutFSU(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.VALIDATED);

        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        Mockito.when(tenderDAO.getConsolidate(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFSU(Mockito.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(COST_DRIVER_OR_FSU_NOT_FOUND, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenChangeStatusTenderWithFSUAndWithoutCostThrowException")
    void updateStatusTenderWithFSUAndWithoutCost(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.VALIDATED);

        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        PnDeliveryDriver fsu = this.getListDrivers(1, true).get(0);

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        Mockito.when(tenderDAO.getConsolidate(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFSU(Mockito.any()))
                .thenReturn(Mono.just(fsu));

        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(new ArrayList<PnCost>().stream()));

        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(COST_DRIVER_OR_FSU_NOT_FOUND, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenChangeStatusTenderInvalidCostThrowException")
    void updateStatusTenderWithInvalidCost(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.VALIDATED);

        PnTender tender = this.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        PnDeliveryDriver fsu = this.getListDrivers(1, true).get(0);

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        Mockito.when(tenderDAO.getConsolidate(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFSU(Mockito.any()))
                .thenReturn(Mono.just(fsu));

        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getAllCosts("123", fsu.getTaxId(), false).stream()));

        StepVerifier.create(this.paperChannelService.updateStatusTender("123", status))
                .expectErrorMatches((e) -> {
                    assertTrue(e instanceof PnGenericException);
                    assertEquals(FSUCOST_VALIDATOR_NOTVALID, ((PnGenericException) e).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenChangeStatusTenderWithValidCostThenResponseOK")
    void updateStatusTenderWithValidCost(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.VALIDATED);

        PnTender tender = InstanceCreator.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.CREATED.toString());

        PnDeliveryDriver fsu = InstanceCreator.getDriver(true);

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        Mockito.when(tenderDAO.getConsolidate(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriverFSU(Mockito.any()))
                .thenReturn(Mono.just(fsu));

        Mockito.when(this.costDAO.findAllFromTenderCode(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(getAllCosts("123", fsu.getTaxId(), true).stream()));

        PnTender tenderUpdated = InstanceCreator.getListTender(1).get(0);
        tenderUpdated.setStatus(TenderDTO.StatusEnum.VALIDATED.toString());

        Mockito.when(this.tenderDAO.createOrUpdate(Mockito.any())).thenReturn(Mono.just(tenderUpdated));

        TenderCreateResponseDTO response = this.paperChannelService.updateStatusTender("123", status).block();
        assertNotNull(response);
    }

    @Test
    @DisplayName("whenChangeStatusTenderWithCreatedStatus")
    void updateStatusTenderWithCreatedStatus(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.CREATED);

        PnTender tender = InstanceCreator.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.VALIDATED.toString());
        tender.setStartDate(Instant.now().plus(10, ChronoUnit.DAYS));
        tender.setEndDate(Instant.now().plus(15, ChronoUnit.DAYS));
        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        PnTender tenderUpdated = InstanceCreator.getListTender(1).get(0);
        tenderUpdated.setStatus(TenderDTO.StatusEnum.CREATED.toString());
        tenderUpdated.setStartDate(Instant.now().plus(10, ChronoUnit.DAYS));
        tenderUpdated.setEndDate(Instant.now().plus(15, ChronoUnit.DAYS));
        Mockito.when(this.tenderDAO.createOrUpdate(Mockito.any())).thenReturn(Mono.just(tenderUpdated));

        TenderCreateResponseDTO response = this.paperChannelService.updateStatusTender("123", status).block();
        assertNotNull(response);
    }

    @Test
    @DisplayName("whenChangeStatusTenderWithSameStatus")
    void updateStatusTenderWithSameStatus(){
        Status status = new Status();
        status.setStatusCode(Status.StatusCodeEnum.VALIDATED);

        PnTender tender = InstanceCreator.getListTender(1).get(0);
        tender.setStatus(TenderDTO.StatusEnum.VALIDATED.toString());
        tender.setStartDate(Instant.now().plus(10, ChronoUnit.DAYS));
        tender.setEndDate(Instant.now().plus(15, ChronoUnit.DAYS));

        Mockito.when(tenderDAO.getTender(Mockito.any())).thenReturn(Mono.just(tender));

        Mockito.when(tenderDAO.createOrUpdate(Mockito.any())).thenReturn(Mono.just(tender));
        TenderCreateResponseDTO response = this.paperChannelService.updateStatusTender("123", status).block();
        assertNotNull(response);
    }

    @Test
    @DisplayName("whenCreateTenderWithEndDateBeforeStartDateThrowException")
    void createTenderWithBadlyFormatDateTest(){
        TenderCreateRequestDTO dto = getTenderDTO();
        Instant passed = Instant.now().minus(20, ChronoUnit.DAYS);
        dto.setEndDate(new Date(passed.toEpochMilli()));
        StepVerifier.create(this.paperChannelService.createOrUpdateTender(dto))
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("whenCreateTenderOkThenReturnResponse")
    void createTenderOK(){
        TenderCreateRequestDTO dto = getTenderDTO();

        Mockito.when(this.tenderDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(getListTender(1).get(0)));

        TenderCreateResponseDTO response = this.paperChannelService.createOrUpdateTender(dto).block();
        assertNotNull(response);
        assertEquals(TenderCreateResponseDTO.CodeEnum.NUMBER_0, response.getCode());
        assertTrue(response.getResult());
    }


    @Test
    @DisplayName("whenCreateDriverButTenderNotExistedThenThrowError")
    void createDriverWithNoTender(){
        DeliveryDriverDTO dto = getDriverDTO();

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.createOrUpdateDriver("1234", dto))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(TENDER_NOT_EXISTED, ((PnGenericException)ex).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenUpdateDriverWithDifferentRoleThrowError")
    void updateDriverNotFSU(){
        DeliveryDriverDTO dto = getDriverDTO();
        PnDeliveryDriver entity = InstanceCreator.getDriver(!dto.getFsu());

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(InstanceCreator.getListTender(1).get(0)));

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(entity));

        StepVerifier.create(paperChannelService.createOrUpdateDriver("1234", dto))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_DRIVER_HAVE_DIFFERENT_ROLE, ((PnGenericException) ex).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenUpdateDriverOkThenReturnResponse")
    void updateDriverOK(){
        DeliveryDriverDTO dto = getDriverDTO();
        PnDeliveryDriver entity = InstanceCreator.getDriver(dto.getFsu());

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(InstanceCreator.getListTender(1).get(0)));

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(entity));

        Mockito.when(this.deliveryDriverDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(getListDrivers(1, true).get(0)));

        this.paperChannelService.createOrUpdateDriver("1224", dto).block();
        Mockito.verify(this.deliveryDriverDAO, Mockito.times(1))
                .createOrUpdate(Mockito.any());

        Mockito.verify(this.tenderDAO, Mockito.times(1))
                .getTender(Mockito.any());

        Mockito.verify(this.deliveryDriverDAO, Mockito.times(1))
                .getDeliveryDriver(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("whenCreateDriverOkThenReturnResponse")
    void createDriverOK(){
        DeliveryDriverDTO dto = getDriverDTO();

        Mockito.when(this.tenderDAO.getTender(Mockito.any()))
                .thenReturn(Mono.just(InstanceCreator.getListTender(1).get(0)));

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Mockito.when(this.deliveryDriverDAO.createOrUpdate(Mockito.any()))
                .thenReturn(Mono.just(getListDrivers(1, true).get(0)));

        this.paperChannelService.createOrUpdateDriver("1224", dto).block();
        Mockito.verify(this.deliveryDriverDAO, Mockito.times(1))
                .createOrUpdate(Mockito.any());

        Mockito.verify(this.tenderDAO, Mockito.times(1))
                .getTender(Mockito.any());

        Mockito.verify(this.deliveryDriverDAO, Mockito.times(1))
                .getDeliveryDriver(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("whenCreateCostWithoutZoneOrCapThenThrowError")
    void createCostWithoutZoneOrCap(){
        CostDTO dto = getCostDTO();

        StepVerifier.create(this.paperChannelService.createOrUpdateCost("1234", "12333", dto))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(COST_BADLY_CONTENT, ((PnGenericException)ex).getExceptionType());
                    return true;
                }).verify();

        dto.setCap(new ArrayList<>());

        StepVerifier.create(this.paperChannelService.createOrUpdateCost("1234", "12333", dto))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(COST_BADLY_CONTENT, ((PnGenericException)ex).getExceptionType());
                    return true;
                }).verify();
    }

    @Test
    @DisplayName("whenCreateCostWithoutDriverThenThrowError")
    void createCostWithoutDriver(){
        CostDTO dto = getCostDTO();
        dto.setZone(InternationalZoneEnum._1);

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                        .thenReturn(Mono.empty());

        StepVerifier.create(this.paperChannelService.createOrUpdateCost("1234", "12333", dto))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(DELIVERY_DRIVER_NOT_EXISTED, ((PnGenericException)ex).getExceptionType());
                    return true;
                }).verify();

    }


    @Test
    @DisplayName("whenCreateCostWithDuplicateCapThenThrowError")
    void createCostWithDuplicateCap(){
        CostDTO dto = getCostDTO();
        dto.setCap(List.of("21045", "99999"));
        dto.setProductType(ProductTypeEnumDto.AR);

        PnDeliveryDriver driver = InstanceCreator.getDriver(true);

        Mockito.when(this.deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(driver));

        Mockito.when(costDAO.findAllFromTenderAndProductTypeAndExcludedUUID(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromStream(InstanceCreator.getAllNationalCost("1234", driver.taxId, true).stream()));

        costMapperMockedStatic = Mockito.mockStatic(CostMapper.class);
        costMapperMockedStatic.when(() -> {
            CostMapper.fromCostDTO(Mockito.any(), Mockito.any(), Mockito.any());
        }).thenReturn(InstanceCreator.getCost("1234", null, dto.getCap(), "AR"));

        costValidatorMockedStatic = Mockito.mockStatic(CostValidator.class);
        costValidatorMockedStatic.when(() -> {
            CostValidator.validateCosts(Mockito.any(), Mockito.any());
        }).thenThrow(new PnGenericException(COST_ALREADY_EXIST, COST_ALREADY_EXIST.getMessage()));


        StepVerifier.create(this.paperChannelService.createOrUpdateCost("1234", "12333", dto))
                .expectErrorMatches((ex) -> {
                    assertTrue(ex instanceof PnGenericException);
                    assertEquals(COST_ALREADY_EXIST, ((PnGenericException)ex).getExceptionType());
                    return true;
                }).verify();

    }

    @Test
    @DisplayName("whenCreateCostThenReturnResponse")
    void createCostOK(){
        CostDTO dto = getCostDTO();
        dto.setZone(InternationalZoneEnum._1);
        PnDeliveryDriver driver = InstanceCreator.getDriver(true);

                Mockito.when(this.deliveryDriverDAO.getDeliveryDriver(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(driver));

        costMapperMockedStatic = Mockito.mockStatic(CostMapper.class);
        costMapperMockedStatic.when(() -> {
            CostMapper.fromCostDTO(Mockito.any(), Mockito.any(), Mockito.any());
        }).thenReturn(InstanceCreator.getCost("1234", "ZONE_1", null, "AR"));


        Mockito.when(costDAO.findAllFromTenderAndProductTypeAndExcludedUUID(Mockito.any(), Mockito.any(), Mockito.any()))
                        .thenReturn(Flux.empty());

        Mockito.when(costDAO.createOrUpdate(Mockito.any()))
                        .thenReturn(Mono.just(getCost("ZONE_1", null, "AR")));

        StepVerifier.create(paperChannelService.createOrUpdateCost("1234", driver.getTaxId(), dto))
                .verifyComplete();

    }

    private List<PnCost> getAllCosts(String tenderCode, String driverCode, boolean fsu){
        List<PnCost> costs = new ArrayList<>();
        List<ProductTypeEnum> products = List.of(ProductTypeEnum.AR, ProductTypeEnum._890, ProductTypeEnum.RS);
        List<String> zones = List.of("ZONE_1", "ZONE_2", "ZONE_3");
        List<String> caps = new ArrayList<>(List.of("21222", "11111"));
        if (fsu) caps.add(Const.CAP_DEFAULT);
        for (ProductTypeEnum national: products){
            PnCost cost = getCost(null, caps, national.getValue());
            cost.setTenderCode(tenderCode);
            cost.setDeliveryDriverCode(driverCode);
            cost.setUuid(UUID.randomUUID().toString());
            cost.setFsu(fsu);
            costs.add(cost);
        }
        for (String zone : zones){
            PnCost cost = getCost(zone, null, "AR");
            cost.setTenderCode(tenderCode);
            cost.setDeliveryDriverCode(driverCode);
            cost.setUuid(UUID.randomUUID().toString());
            cost.setFsu(fsu);
            costs.add(cost);
        }
        for (String zone : zones){
            PnCost cost = getCost(zone, null, "RS");
            cost.setTenderCode(tenderCode);
            cost.setDeliveryDriverCode(driverCode);
            cost.setUuid(UUID.randomUUID().toString());
            cost.setFsu(fsu);
            costs.add(cost);
        }

        return costs;
    }

    private PnCost getCost(String zone, List<String> cap, String productType){
        PnCost cost = new PnCost();
        cost.setTenderCode("TENDER_1");
        cost.setFsu(true);
        cost.setZone(zone);
        cost.setCap(cap);
        cost.setUuid(UUID.randomUUID().toString());
        cost.setBasePrice(BigDecimal.valueOf(1.23F));
        cost.setPagePrice(BigDecimal.valueOf(1.23F));
        cost.setProductType(productType);
        return cost;
    }

    private List<PnDeliveryDriver> getListDrivers(int number, boolean fsu){
        List<PnDeliveryDriver> drivers = new ArrayList<>();
        for (int i = 0; i < number; i++){
            PnDeliveryDriver driver = new PnDeliveryDriver();
            driver.setTenderCode("TENDER_1");
            driver.setFsu(fsu);
            driver.setTaxId("12345678"+i);
            drivers.add(driver);
        }
        return drivers;
    }

    private List<PnTender> getListTender(int number) {
        List<PnTender> tenders = new ArrayList<>();
        for (int i=0; i <= number; i++){
            PnTender tender = new PnTender();
            tender.setTenderCode("Tender_"+i);
            tender.setStatus("CREATED");
            tender.setDescription("GARA_2023");

            tenders.add(tender);
        }
        return tenders;
    }

    private TenderCreateRequestDTO getTenderDTO(){
        TenderCreateRequestDTO dto = new TenderCreateRequestDTO();
        dto.setCode("1234444");
        dto.setStartDate(new Date());
        dto.setEndDate(new Date());
        return dto;
    }

    private DeliveryDriverDTO getDriverDTO(){
        DeliveryDriverDTO dto = new DeliveryDriverDTO();
        dto.setTaxId("12345678901");
        dto.setFsu(true);
        dto.setDenomination("GLS");
        dto.setBusinessName("GLS");
        return dto;
    }

    private CostDTO getCostDTO(){
        CostDTO dto = new CostDTO();
        dto.setPrice(BigDecimal.valueOf(1.23F));
        dto.setPriceAdditional(BigDecimal.valueOf(1.23F));
        dto.setDriverCode("1223444");
        dto.setTenderCode("1233344");
        return dto;
    }

}