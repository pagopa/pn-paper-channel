package it.pagopa.pn.paperchannel.dao;

import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;

import java.io.InputStream;

public interface ExcelDAO<M> {

    ExcelEngine create(M data);
    M readData(InputStream inputStream);

}
