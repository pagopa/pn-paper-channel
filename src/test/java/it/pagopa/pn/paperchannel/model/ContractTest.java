package it.pagopa.pn.paperchannel.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContractTest {
    private Double price;
    private Double pricePerPage;


    @BeforeEach
    void setUp(){
        this.initialize();
    }
    @Test
    void setGetTest() {
        Contract contract = initContract();
        Assertions.assertNotNull(contract);
        Assertions.assertEquals(price, contract.getPrice());
        Assertions.assertEquals(pricePerPage, contract.getPricePerPage());

        Double price = 1.09;
        Double pricePerPage = 0.1;

        contract.setPrice(price);
        contract.setPricePerPage(pricePerPage);

        Assertions.assertEquals(price, contract.getPrice());
        Assertions.assertEquals(pricePerPage, contract.getPricePerPage());
    }

    private Contract initContract() {
        Contract contract = new Contract();
        contract.setPrice(price);
        contract.setPricePerPage(pricePerPage);
        return contract;
    }

    private void initialize() {
        price = 1.75;
        pricePerPage = 0.5;
    }
}
