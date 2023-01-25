package it.pagopa.pn.paperchannel.dao;

import it.pagopa.pn.paperchannel.dao.common.ExcelEngine;

import java.io.FileInputStream;

public interface ExcelDAO<MODEL> {

    ExcelEngine create(MODEL data);
    MODEL readData(FileInputStream inputStream);

}
