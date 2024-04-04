package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.ConfigTypeEnum;
import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventItem;
import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventPayload;
import it.pagopa.pn.commons.rules.model.FilterChainResult;
import it.pagopa.pn.commons.rules.model.ListFilterChainResult;
import it.pagopa.pn.paperchannel.mapper.AttachmentsConfigMapper;
import it.pagopa.pn.paperchannel.middleware.db.dao.PnAttachmentsConfigDAO;
import it.pagopa.pn.paperchannel.middleware.db.entities.*;
import it.pagopa.pn.paperchannel.model.Address;
import it.pagopa.pn.paperchannel.model.StatusDeliveryEnum;
import it.pagopa.pn.paperchannel.rule.handler.DocumentTagHandler;
import it.pagopa.pn.paperchannel.rule.handler.PaperListChainEngine;
import it.pagopa.pn.paperchannel.utils.AttachmentsConfigUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentsConfigServiceImplTest {

    @Mock
    private PnAttachmentsConfigDAO pnAttachmentsConfigDAO;


    @Mock
    private PaperListChainEngine paperListChainEngine;


    @InjectMocks
    private AttachmentsConfigServiceImpl attachmentsConfigService;


    @Test
    void refreshConfigOk() {
        String configKey = "80100";
        String configType = ConfigTypeEnum.ZIPCODE.name();
        final String pk = AttachmentsConfigUtils.buildPartitionKey(configKey, configType);

        PnAttachmentsConfigEventItem itemOne = PnAttachmentsConfigEventItem.builder()
                .startValidity(Instant.parse("2024-01-10T00:00:00.000Z"))
                .endValidity(Instant.parse("2024-02-10T00:00:00.000Z"))
                .build();

        PnAttachmentsConfigEventItem itemTwo = PnAttachmentsConfigEventItem.builder()
                .startValidity(Instant.parse("2024-01-10T00:00:00.000Z"))
                .endValidity(Instant.parse("2024-02-10T00:00:00.000Z"))
                .build();

        PnAttachmentsConfigEventPayload payload = PnAttachmentsConfigEventPayload.builder()
                .configKey(configKey)
                .configType(ConfigTypeEnum.ZIPCODE.name())
                .configs(List.of(itemOne, itemTwo))
                .build();

        var pnAttachmentsConfigs = AttachmentsConfigMapper.toPnAttachmentsConfig(configKey, configType, List.of(itemOne, itemTwo));

        when(pnAttachmentsConfigDAO.putItemInTransaction(pk, pnAttachmentsConfigs))
                .thenReturn(Mono.empty());

        StepVerifier.create(attachmentsConfigService.refreshConfig(payload))
                .verifyComplete();

        verify(pnAttachmentsConfigDAO).putItemInTransaction(pk, pnAttachmentsConfigs);

    }

    @Test
    void refreshConfigKOForDAO() {
        String configKey = "80100";
        String configType = ConfigTypeEnum.ZIPCODE.name();
        final String pk = AttachmentsConfigUtils.buildPartitionKey(configKey, configType);

        PnAttachmentsConfigEventItem itemOne = PnAttachmentsConfigEventItem.builder()
                .startValidity(Instant.parse("2024-01-10T00:00:00.000Z"))
                .endValidity(Instant.parse("2024-02-10T00:00:00.000Z"))
                .build();

        PnAttachmentsConfigEventItem itemTwo = PnAttachmentsConfigEventItem.builder()
                .startValidity(Instant.parse("2024-01-10T00:00:00.000Z"))
                .endValidity(Instant.parse("2024-02-10T00:00:00.000Z"))
                .build();

        PnAttachmentsConfigEventPayload payload = PnAttachmentsConfigEventPayload.builder()
                .configKey(configKey)
                .configType(ConfigTypeEnum.ZIPCODE.name())
                .configs(List.of(itemOne, itemTwo))
                .build();

        var pnAttachmentsConfigs = AttachmentsConfigMapper.toPnAttachmentsConfig(configKey, configType, List.of(itemOne, itemTwo));

        when(pnAttachmentsConfigDAO.putItemInTransaction(pk, pnAttachmentsConfigs))
                .thenReturn(Mono.error(new RuntimeException("Error DB")));

        StepVerifier.create(attachmentsConfigService.refreshConfig(payload))
                .expectError(RuntimeException.class)
                .verify();

        verify(pnAttachmentsConfigDAO).putItemInTransaction(pk, pnAttachmentsConfigs);

    }


    @Test
    void filterAttachmentsToSend() {
        // GIVEN
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING);


        PnAddress pnAddress = new PnAddress();
        pnAddress.setCap("30000");

        List<ListFilterChainResult<PnAttachmentInfo>> filterChainResults =new ArrayList<>();
        ListFilterChainResult<PnAttachmentInfo> filterChainResult = new ListFilterChainResult<PnAttachmentInfo>();
        filterChainResult.setItem(pnDeliveryRequest.getAttachments().get(0));
        filterChainResult.setSuccess(true);
        filterChainResult.setCode("OK");
        filterChainResult.setDiagnostic("oook");
        filterChainResults.add(filterChainResult);
        filterChainResult = new ListFilterChainResult<PnAttachmentInfo>();
        filterChainResult.setItem(pnDeliveryRequest.getAttachments().get(1));
        filterChainResult.setSuccess(true);
        filterChainResult.setCode("OK");
        filterChainResult.setDiagnostic("oook");
        filterChainResults.add(filterChainResult);

        List<PnAttachmentsRule> ruleParamsList = new ArrayList<>();
        PnAttachmentsRule rule = new PnAttachmentsRule();
        rule.setRuleType(DocumentTagHandler.RULE_TYPE);
        PnRuleParams pnRuleParams = new PnRuleParams();
        pnRuleParams.setTypeWithSuccessResult("AAR");
        rule.setParams(pnRuleParams);
        ruleParamsList.add(rule);
        PnAttachmentsConfig pnAttachmentsConfig = new PnAttachmentsConfig();
        pnAttachmentsConfig.setRules(ruleParamsList);

        Mockito.when(pnAttachmentsConfigDAO.findConfigInInterval(Mockito.any(), Mockito.any())).thenReturn(Mono.just(pnAttachmentsConfig));
        Mockito.when(paperListChainEngine.filterItems(Mockito.any(), Mockito.anyList(), Mockito.anyList())).thenReturn(Flux.fromIterable(filterChainResults));

        // WHEN
        Mono<PnDeliveryRequest> mono = attachmentsConfigService.filterAttachmentsToSend(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), pnAddress);
        PnDeliveryRequest res = mono.block();

        // THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(2, res.getAttachments().size());
        Assertions.assertEquals(0, res.getRemovedAttachments().size());
    }


    @Test
    void filterAttachmentsToSend_withremoved() {
        // GIVEN
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING);


        PnAddress pnAddress = new PnAddress();
        pnAddress.setCap("30000");

        List<ListFilterChainResult<PnAttachmentInfo>> filterChainResults =new ArrayList<>();
        ListFilterChainResult<PnAttachmentInfo> filterChainResult = new ListFilterChainResult<PnAttachmentInfo>();
        filterChainResult.setItem(pnDeliveryRequest.getAttachments().get(0));
        filterChainResult.setSuccess(true);
        filterChainResult.setCode("OK");
        filterChainResult.setDiagnostic("oook");
        filterChainResults.add(filterChainResult);
        filterChainResult = new ListFilterChainResult<PnAttachmentInfo>();
        filterChainResult.setItem(pnDeliveryRequest.getAttachments().get(1));
        filterChainResult.setSuccess(false);
        filterChainResult.setCode("KO");
        filterChainResult.setDiagnostic("koooooo");
        filterChainResults.add(filterChainResult);

        List<PnAttachmentsRule> ruleParamsList = new ArrayList<>();
        PnAttachmentsRule rule = new PnAttachmentsRule();
        rule.setRuleType(DocumentTagHandler.RULE_TYPE);
        PnRuleParams pnRuleParams = new PnRuleParams();
        pnRuleParams.setTypeWithSuccessResult("AAR");
        rule.setParams(pnRuleParams);
        ruleParamsList.add(rule);
        PnAttachmentsConfig pnAttachmentsConfig = new PnAttachmentsConfig();
        pnAttachmentsConfig.setRules(ruleParamsList);

        Mockito.when(pnAttachmentsConfigDAO.findConfigInInterval(Mockito.any(), Mockito.any())).thenReturn(Mono.just(pnAttachmentsConfig));
        Mockito.when(paperListChainEngine.filterItems(Mockito.any(), Mockito.anyList(), Mockito.anyList())).thenReturn(Flux.fromIterable(filterChainResults));

        // WHEN
        Mono<PnDeliveryRequest> mono = attachmentsConfigService.filterAttachmentsToSend(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), pnAddress);
        PnDeliveryRequest res = mono.block();

        // THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(1, res.getAttachments().size());
        Assertions.assertEquals(getPnAttachmentInfos().get(0).getFileKey(), res.getAttachments().get(0).getFileKey());
        Assertions.assertEquals(1, res.getRemovedAttachments().size());
        Assertions.assertEquals(getPnAttachmentInfos().get(1).getFileKey(), res.getRemovedAttachments().get(0).getFileKey());
    }

    private PnDeliveryRequest getDeliveryRequest(StatusDeliveryEnum status){
        PnDeliveryRequest deliveryRequest= new PnDeliveryRequest();
        List<PnAttachmentInfo> attachmentUrls = getPnAttachmentInfos();

        Address address = new Address();
        address.setAddress("via roma");
        address.setAddressRow2("via lazio");
        address.setCap("00061");
        address.setCity("roma");
        address.setCity2("viterbo");
        address.setCountry("italia");
        address.setPr("PR");
        address.setFullName("Ettore Fieramosca");
        address.setNameRow2("Ettore");
        address.setFromNationalRegistry(true);

        deliveryRequest.setAddressHash(address.convertToHash());
        deliveryRequest.setRequestId("12345");
        deliveryRequest.setFiscalCode("ABCD123AB501");
        deliveryRequest.setReceiverType("PF");
        deliveryRequest.setIun("iun");
        deliveryRequest.setCorrelationId("");
        deliveryRequest.setStatusCode(status.getCode());
        deliveryRequest.setStatusDescription(status.getDescription());
        deliveryRequest.setStatusDetail(status.getDetail());
        deliveryRequest.setStatusDate("2023-07-07T08:43:00.764Z");
        deliveryRequest.setProposalProductType("AR");
        deliveryRequest.setPrintType("PT");
        deliveryRequest.setStartDate("");
        deliveryRequest.setProductType("RN_AR");
        deliveryRequest.setAttachments(attachmentUrls);
        deliveryRequest.setNotificationSentAt(Instant.EPOCH.plusMillis(57));
        return deliveryRequest;
    }

    @NotNull
    private static List<PnAttachmentInfo> getPnAttachmentInfos() {
        List<PnAttachmentInfo> attachmentUrls = new ArrayList<>();
        PnAttachmentInfo pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("2023-07-07T08:43:00.764Z");
        pnAttachmentInfo.setFileKey("http://localhost:8080");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("http://localhost:8080");
        pnAttachmentInfo.setDocTag("AAR");
        attachmentUrls.add(pnAttachmentInfo);
        pnAttachmentInfo = new PnAttachmentInfo();
        pnAttachmentInfo.setDate("2023-07-07T08:43:00.764Z");
        pnAttachmentInfo.setFileKey("http://localhost:8080/1");
        pnAttachmentInfo.setId("");
        pnAttachmentInfo.setNumberOfPage(3);
        pnAttachmentInfo.setDocumentType("");
        pnAttachmentInfo.setUrl("http://localhost:8080/1");
        pnAttachmentInfo.setDocTag("DOCUMENT");
        attachmentUrls.add(pnAttachmentInfo);
        return attachmentUrls;
    }

}
