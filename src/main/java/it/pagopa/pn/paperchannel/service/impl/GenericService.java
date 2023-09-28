package it.pagopa.pn.paperchannel.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.paperchannel.middleware.db.dao.RequestDeliveryDAO;
import it.pagopa.pn.paperchannel.service.SqsSender;
import it.pagopa.pn.paperchannel.utils.PnLogAudit;

public class GenericService {
    protected final PnAuditLogBuilder auditLogBuilder;
    protected final SqsSender sqsSender;
    protected PnLogAudit pnLogAudit;
    protected RequestDeliveryDAO requestDeliveryDAO;

    public GenericService(PnAuditLogBuilder auditLogBuilder, SqsSender sqsSender, RequestDeliveryDAO requestDeliveryDAO) {
        this.auditLogBuilder = auditLogBuilder;
        this.sqsSender = sqsSender;
        this.pnLogAudit = new PnLogAudit(auditLogBuilder);
        this.requestDeliveryDAO = requestDeliveryDAO;
    }
}
