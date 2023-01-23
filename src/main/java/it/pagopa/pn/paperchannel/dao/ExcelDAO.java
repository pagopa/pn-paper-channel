package it.pagopa.pn.paperchannel.dao;

import java.io.FileInputStream;

public interface ExcelDAO<MODEL> {


    void createAndSave(MODEL data);

    MODEL readData(FileInputStream inputStream);


}
