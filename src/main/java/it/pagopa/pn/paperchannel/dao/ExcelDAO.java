package it.pagopa.pn.paperchannel.dao;

import java.io.File;
import java.io.FileInputStream;

public interface ExcelDAO<MODEL> {


    File createAndSave(MODEL data);
    MODEL readData(FileInputStream inputStream);


}
