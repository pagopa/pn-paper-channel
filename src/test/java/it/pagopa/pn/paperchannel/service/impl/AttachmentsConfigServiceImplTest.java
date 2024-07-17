package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.api.dto.events.ConfigTypeEnum;
import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventItem;
import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventPayload;
import it.pagopa.pn.commons.rules.model.ListFilterChainResult;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.exception.PnInvalidChainRuleException;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentsConfigServiceImplTest {

    @Mock
    private PnAttachmentsConfigDAO pnAttachmentsConfigDAO;


    @Mock
    private PaperListChainEngine paperListChainEngine;


    @Mock
    private PnPaperChannelConfig pnPaperChannelConfig;

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

        var pnAttachmentsConfigs = AttachmentsConfigMapper.toPnAttachmentsConfig(configKey, configType, List.of(itemOne, itemTwo), "default##zip");

        when(pnAttachmentsConfigDAO.refreshConfig(pk, pnAttachmentsConfigs))
                .thenReturn(Mono.empty());
        when(pnPaperChannelConfig.getDefaultattachmentconfigcap()).thenReturn("default##zip");

        StepVerifier.create(attachmentsConfigService.refreshConfig(payload))
                .verifyComplete();

        verify(pnAttachmentsConfigDAO).refreshConfig(pk, pnAttachmentsConfigs);

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

        var pnAttachmentsConfigs = AttachmentsConfigMapper.toPnAttachmentsConfig(configKey, configType, List.of(itemOne, itemTwo), "default##zip");

        when(pnAttachmentsConfigDAO.refreshConfig(pk, pnAttachmentsConfigs))
                .thenReturn(Mono.error(new RuntimeException("Error DB")));
        when(pnPaperChannelConfig.getDefaultattachmentconfigcap()).thenReturn("default##zip");

        StepVerifier.create(attachmentsConfigService.refreshConfig(payload))
                .expectError(RuntimeException.class)
                .verify();

        verify(pnAttachmentsConfigDAO).refreshConfig(pk, pnAttachmentsConfigs);

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
        when(pnPaperChannelConfig.isEnabledocfilterruleengine()).thenReturn(true);

        // WHEN
        Mono<PnDeliveryRequest> mono = attachmentsConfigService.filterAttachmentsToSend(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), pnAddress);
        PnDeliveryRequest res = mono.block();

        // THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(2, res.getAttachments().size());
        Assertions.assertEquals(0, res.getRemovedAttachments().size());
    }


    @Test
    void filterAttachmentsToSend_rulenavigation() {
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
        pnAttachmentsConfig.setConfigKey("ZIP##DEFAULT");

        PnAttachmentsConfig pnAttachmentsConfig1 = new PnAttachmentsConfig();
        pnAttachmentsConfig1.setRules(null);
        pnAttachmentsConfig1.setConfigKey("ZIP#30000");
        pnAttachmentsConfig1.setParentReference("ZIP##DEFAULT");

        Mockito.when(pnAttachmentsConfigDAO.findConfigInInterval(Mockito.eq("ZIP##30000"), Mockito.any())).thenReturn(Mono.just(pnAttachmentsConfig1));
        Mockito.when(pnAttachmentsConfigDAO.findConfigInInterval(Mockito.eq("ZIP##DEFAULT"), Mockito.any())).thenReturn(Mono.just(pnAttachmentsConfig));
        Mockito.when(paperListChainEngine.filterItems(Mockito.any(), Mockito.anyList(), Mockito.anyList())).thenReturn(Flux.fromIterable(filterChainResults));
        when(pnPaperChannelConfig.isEnabledocfilterruleengine()).thenReturn(true);

        // WHEN
        Mono<PnDeliveryRequest> mono = attachmentsConfigService.filterAttachmentsToSend(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), pnAddress);
        PnDeliveryRequest res = mono.block();

        // THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(1, res.getAttachments().size());
        Assertions.assertEquals(1, res.getRemovedAttachments().size());
    }


    @Test
    void filterAttachmentsToSend_rulenavigation1() {
        // GIVEN
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING);


        PnAddress pnAddress = new PnAddress();
        pnAddress.setCap("30000");

        List<ListFilterChainResult<PnAttachmentInfo>> filterChainResults =new ArrayList<>();
        ListFilterChainResult<PnAttachmentInfo> filterChainResult = new ListFilterChainResult<PnAttachmentInfo>();
        filterChainResult.setItem(pnDeliveryRequest.getAttachments().get(0));
        filterChainResult.setSuccess(false);
        filterChainResult.setCode("KO");
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
        pnAttachmentsConfig.setConfigKey("ZIP##DEFAULT");

        List<PnAttachmentsRule> ruleParamsList1 = new ArrayList<>();
        PnAttachmentsRule rule1 = new PnAttachmentsRule();
        rule1.setRuleType(DocumentTagHandler.RULE_TYPE);
        PnRuleParams pnRuleParams1 = new PnRuleParams();
        pnRuleParams1.setTypeWithSuccessResult("DOCUMENT");
        rule1.setParams(pnRuleParams1);
        ruleParamsList1.add(rule1);
        PnAttachmentsConfig pnAttachmentsConfig1 = new PnAttachmentsConfig();
        pnAttachmentsConfig1.setRules(ruleParamsList1);
        pnAttachmentsConfig1.setConfigKey("ZIP#30000");
        pnAttachmentsConfig1.setParentReference("ZIP##DEFAULT");

        Mockito.when(pnAttachmentsConfigDAO.findConfigInInterval(Mockito.eq("ZIP##30000"), Mockito.any())).thenReturn(Mono.just(pnAttachmentsConfig1));
        Mockito.when(paperListChainEngine.filterItems(Mockito.any(), Mockito.anyList(), Mockito.anyList())).thenReturn(Flux.fromIterable(filterChainResults));
        when(pnPaperChannelConfig.isEnabledocfilterruleengine()).thenReturn(true);

        // WHEN
        Mono<PnDeliveryRequest> mono = attachmentsConfigService.filterAttachmentsToSend(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), pnAddress);
        PnDeliveryRequest res = mono.block();

        // THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(1, res.getAttachments().size());
        Assertions.assertEquals(1, res.getRemovedAttachments().size());
        Assertions.assertEquals(getPnAttachmentInfos().get(1).getFileKey(), res.getAttachments().get(0).getFileKey());  // ci deve essere il document
        Assertions.assertEquals(getPnAttachmentInfos().get(0).getFileKey(), res.getRemovedAttachments().get(0).getFileKey()); // non ci deve essere aar
        Assertions.assertEquals("OK", res.getAttachments().get(0).getFilterResultCode());
        Assertions.assertEquals("KO", res.getRemovedAttachments().get(0).getFilterResultCode());
    }

    @Test
    void filterAttachmentsToSend_missingcap() {
        // GIVEN
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING);


        PnAddress pnAddress = new PnAddress();
        pnAddress.setCap("30000");



        Mockito.when(pnAttachmentsConfigDAO.findConfigInInterval(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        when(pnPaperChannelConfig.isEnabledocfilterruleengine()).thenReturn(true);

        // WHEN
        Mono<PnDeliveryRequest> mono = attachmentsConfigService.filterAttachmentsToSend(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), pnAddress);
        PnDeliveryRequest res = mono.block();

        // THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(2, res.getAttachments().size());
        Assertions.assertEquals(0, res.getRemovedAttachments().size());
    }

    @Test
    void filterAttachmentsToSend_enginedisabled() {
        // GIVEN
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING);


        PnAddress pnAddress = new PnAddress();
        pnAddress.setCap("30000");

        List<ListFilterChainResult<PnAttachmentInfo>> filterChainResults =new ArrayList<>();
        ListFilterChainResult<PnAttachmentInfo> filterChainResult = new ListFilterChainResult<PnAttachmentInfo>();
        filterChainResult.setItem(pnDeliveryRequest.getAttachments().get(0));
        filterChainResult.setSuccess(false);
        filterChainResult.setCode("OK");
        filterChainResult.setDiagnostic("oook");
        filterChainResults.add(filterChainResult);
        filterChainResult = new ListFilterChainResult<PnAttachmentInfo>();
        filterChainResult.setItem(pnDeliveryRequest.getAttachments().get(1));
        filterChainResult.setSuccess(false);
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

        when(pnPaperChannelConfig.isEnabledocfilterruleengine()).thenReturn(false);

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
        when(pnPaperChannelConfig.isEnabledocfilterruleengine()).thenReturn(true);

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


    @Test
    void filterAttachmentsToSend_emptyattachmentstosend() {
        // GIVEN
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING);


        PnAddress pnAddress = new PnAddress();
        pnAddress.setCap("30000");

        List<ListFilterChainResult<PnAttachmentInfo>> filterChainResults =new ArrayList<>();
        ListFilterChainResult<PnAttachmentInfo> filterChainResult = new ListFilterChainResult<PnAttachmentInfo>();
        filterChainResult.setItem(pnDeliveryRequest.getAttachments().get(0));
        filterChainResult.setSuccess(false);
        filterChainResult.setCode("KO");
        filterChainResult.setDiagnostic("oook");
        filterChainResults.add(filterChainResult);
        filterChainResult = new ListFilterChainResult<PnAttachmentInfo>();
        filterChainResult.setItem(pnDeliveryRequest.getAttachments().get(1));
        filterChainResult.setSuccess(false);
        filterChainResult.setCode("KO");
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
        when(pnPaperChannelConfig.isEnabledocfilterruleengine()).thenReturn(true);

        // WHEN
        Mono<PnDeliveryRequest> mono = attachmentsConfigService.filterAttachmentsToSend(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), pnAddress);
        Assertions.assertThrows(PnInvalidChainRuleException.class, mono::block);
    }

    @Test
    void filterAttachmentsToSendSkippedBecauseAarWithRaddFalse() {
        // GIVEN
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING);
        pnDeliveryRequest.setAarWithRadd(false);


        PnAddress pnAddress = new PnAddress();
        pnAddress.setCap("30000");

        when(pnPaperChannelConfig.isEnabledocfilterruleengine()).thenReturn(true);

        // WHEN
        Mono<PnDeliveryRequest> mono = attachmentsConfigService.filterAttachmentsToSend(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), pnAddress);
        PnDeliveryRequest res = mono.block();

        // THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(2, res.getAttachments().size());
        Assertions.assertEquals(0, res.getRemovedAttachments().size());
        verify(pnAttachmentsConfigDAO, never()).findConfigInInterval(any(), any());
    }

    @Test
    void filterAttachmentsToSendSkippedBecauseAarWithRaddNull() {
        // GIVEN
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING);
        pnDeliveryRequest.setAarWithRadd(null);


        PnAddress pnAddress = new PnAddress();
        pnAddress.setCap("30000");

        when(pnPaperChannelConfig.isEnabledocfilterruleengine()).thenReturn(true);

        // WHEN
        Mono<PnDeliveryRequest> mono = attachmentsConfigService.filterAttachmentsToSend(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), pnAddress);
        PnDeliveryRequest res = mono.block();

        // THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(2, res.getAttachments().size());
        Assertions.assertEquals(0, res.getRemovedAttachments().size());
        verify(pnAttachmentsConfigDAO, never()).findConfigInInterval(any(), any());
    }

    @Test
    void filterAttachmentsToSendSkippedBecauseCountryIsNotNational() {
        // GIVEN
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING);


        PnAddress pnAddress = new PnAddress();
        pnAddress.setCap("30000");
        pnAddress.setCountry("SPAGNA");

        when(pnPaperChannelConfig.isEnabledocfilterruleengine()).thenReturn(true);

        // WHEN
        Mono<PnDeliveryRequest> mono = attachmentsConfigService.filterAttachmentsToSend(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), pnAddress);
        PnDeliveryRequest res = mono.block();

        // THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(2, res.getAttachments().size());
        Assertions.assertEquals(0, res.getRemovedAttachments().size());
        verify(pnAttachmentsConfigDAO, never()).findConfigInInterval(any(), any());
    }

    @Test
    void filterAttachmentsToSendOkWithCountryItalia() {
        // GIVEN
        PnDeliveryRequest pnDeliveryRequest = getDeliveryRequest(StatusDeliveryEnum.IN_PROCESSING);

        PnAddress pnAddress = new PnAddress();
        pnAddress.setCap("30000");
        pnAddress.setCountry("ITALIA");

        PnAttachmentsRule rule = new PnAttachmentsRule();
        rule.setRuleType(DocumentTagHandler.RULE_TYPE);
        final PnRuleParams params = new PnRuleParams();
        params.setTypeWithNextResult("AAR");
        rule.setParams(params);
        PnAttachmentsConfig pnAttachmentsConfig = new PnAttachmentsConfig();
        pnAttachmentsConfig.setConfigKey("ZIP##DEFAULT");
        pnAttachmentsConfig.setStartValidity(Instant.now().minus(1, ChronoUnit.DAYS));
        pnAttachmentsConfig.setRules(List.of(rule));

        List<ListFilterChainResult<PnAttachmentInfo>> filterChainResults =new ArrayList<>();
        ListFilterChainResult<PnAttachmentInfo> filterChainResult = new ListFilterChainResult<PnAttachmentInfo>();
        filterChainResult.setItem(pnDeliveryRequest.getAttachments().get(0));
        filterChainResult.setSuccess(true);
        filterChainResult.setCode("OK");
        filterChainResult.setDiagnostic("oook");
        filterChainResults.add(filterChainResult);
        filterChainResult = new ListFilterChainResult<>();
        filterChainResult.setItem(pnDeliveryRequest.getAttachments().get(1));
        filterChainResult.setSuccess(false);
        filterChainResult.setCode("KO");
        filterChainResult.setDiagnostic("ko");
        filterChainResults.add(filterChainResult);

        when(pnPaperChannelConfig.isEnabledocfilterruleengine()).thenReturn(true);
        when(pnAttachmentsConfigDAO.findConfigInInterval("ZIP##30000", pnDeliveryRequest.getNotificationSentAt()))
                .thenReturn(Mono.just(pnAttachmentsConfig));
        when(paperListChainEngine.filterItems(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), pnAttachmentsConfig.getRules()))
                .thenReturn(Flux.fromIterable(filterChainResults));

        // WHEN
        Mono<PnDeliveryRequest> mono = attachmentsConfigService.filterAttachmentsToSend(pnDeliveryRequest, pnDeliveryRequest.getAttachments(), pnAddress);
        PnDeliveryRequest res = mono.block();

        // THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(1, res.getAttachments().size());
        Assertions.assertEquals(1, res.getRemovedAttachments().size());
        verify(pnAttachmentsConfigDAO).findConfigInInterval("ZIP##30000", pnDeliveryRequest.getNotificationSentAt());
        verify(paperListChainEngine).filterItems(any(), any(), eq(pnAttachmentsConfig.getRules()));
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
        deliveryRequest.setAarWithRadd(true);
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
