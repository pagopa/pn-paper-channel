package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.config.HttpConnector;
import it.pagopa.pn.paperchannel.config.PnPaperChannelConfig;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.model.DematZipInternalEvent;
import it.pagopa.pn.paperchannel.service.DematZipService;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.SafeStorageUtils;
import it.pagopa.pn.paperchannel.utils.ZipUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
public class DematZipServiceImpl extends GenericService implements DematZipService {

    private final PnPaperChannelConfig pnPaperChannelConfig;

    private final SafeStorageUtils safeStorageUtils;

    private final HttpConnector httpConnector;



    public DematZipServiceImpl(PnAuditLogBuilder auditLogBuilder, SqsSender sqsSender, RequestDeliveryDAO requestDeliveryDAO,
                               PnPaperChannelConfig pnPaperChannelConfig, SafeStorageUtils safeStorageUtils, HttpConnector httpConnector) {
        super(auditLogBuilder, sqsSender, requestDeliveryDAO);
        this.pnPaperChannelConfig = pnPaperChannelConfig;
        this.safeStorageUtils = safeStorageUtils;
        this.httpConnector = httpConnector;
    }

    @Override
    public Mono<Void> handle(DematZipInternalEvent dematZipInternalEvent) {
        String fileKey = safeStorageUtils.getFileKeyFromUri(dematZipInternalEvent.getAttachmentUri());
        safeStorageUtils.getFileRecursive(pnPaperChannelConfig.getAttemptSafeStorage(), fileKey, BigDecimal.ZERO)
                .flatMap(fileDownloadResponseDto -> httpConnector.downloadFileInByteArray(fileDownloadResponseDto.getDownload().getUrl()))
                .map(ZipUtils::extractPdfFromZip);
        return Mono.empty();
    }
}
