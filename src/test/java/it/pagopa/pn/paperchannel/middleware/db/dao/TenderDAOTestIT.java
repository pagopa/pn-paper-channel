package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.TenderDTO;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.utils.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TenderDAOTestIT extends BaseTest {

    @Autowired
    private TenderDAO tenderDAO;
    private final PnTender t1 = new PnTender();
    private final PnTender t2 = new PnTender();
    private final PnTender active = new PnTender();
    private final PnTender validate = new PnTender();
    private final PnTender validate1 = new PnTender();


    @BeforeEach
    public void setUp(){
        initialize();
    }

    @Test
    void findTenderActiveTest(){
        PnTender finded = this.tenderDAO.findActiveTender().block();
        assertNotNull(finded);
        assertEquals(active.getTenderCode(), finded.getTenderCode());
        assertEquals(active.getAuthor(), finded.getAuthor());
        assertEquals(active.getStatus(), finded.getStatus());
        assertEquals(active.getDescription(), finded.getDescription());
    }

    @Test
    void findTenderDetailTest(){
        PnTender tender = this.tenderDAO.getTender(t1.getTenderCode()).block();
        assertNotNull(tender);
        assertEquals(t1.getTenderCode(), tender.getTenderCode());
        assertEquals(t1.getDate(), tender.getDate());
        assertEquals(t1.getAuthor(), tender.getAuthor());
        assertEquals(t1.getStatus(), tender.getStatus());
        assertEquals(t1.getDescription(), tender.getDescription());
    }

    @Test
    void findAllTenders(){
        List<PnTender> alls = this.tenderDAO.getTenders().block();
        assertNotNull(alls);
        assertEquals(5, alls.size());
    }

    @Test
    void deleteTenderTest(){
        PnTender tender = this.tenderDAO.deleteTender(t1.getTenderCode()).block();
        assertNotNull(tender);
    }

    @Test
    void getTenderStatusValidateConsolidateTest(){
        Instant startDate = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant endDate = Instant.now().minus(2, ChronoUnit.DAYS);
        PnTender tender = this.tenderDAO.getConsolidate(startDate, endDate).block();
        assertNotNull(tender);
    }
    @Test
    void getTenderStatusValidateConsolidateTest2(){
        Instant startDate = Instant.parse("2023-01-14T00:20:56.630714800Z");
        Instant endDate = Instant.parse("2023-01-21T00:20:56.630714800Z");
        PnTender tender = this.tenderDAO.getConsolidate(startDate, endDate).block();
        assertNotNull(tender);
    }

    @Test
    void getTenderStatusValidateConsolidateTest3(){
        Instant startDate = Instant.parse("2023-01-08T00:20:56.630714800Z");
        Instant endDate = Instant.parse("2023-01-17T00:20:56.630714800Z");
        PnTender tender = this.tenderDAO.getConsolidate(startDate, endDate).block();
        assertNotNull(tender);
    }

    @Test
    void getTenderStatusValidateConsolidateTest4(){
        Instant startDate = Instant.parse("2023-01-08T00:20:56.630714800Z");
        Instant endDate = Instant.parse("2023-01-21T00:20:56.630714800Z");
        PnTender tender = this.tenderDAO.getConsolidate(startDate, endDate).block();
        assertNotNull(tender);
    }

    @Test
    void getTenderStatusValidateNotConsolidateTest(){
        Instant startDate = Instant.parse("2024-01-10T00:20:56.630714800Z");
        Instant endDate = Instant.parse("2024-01-28T00:20:56.630714800Z");
        PnTender tender = this.tenderDAO.getConsolidate(startDate, endDate).block();
        assertNull(tender);
    }


    private void initialize(){
        t1.setTenderCode("GARA-2021");
        t1.setDate(Instant.parse("2021-01-01T00:20:56.630714800Z"));
        t1.setStartDate(Instant.parse("2021-01-01T00:20:56.630714800Z"));
        t1.setEndDate(Instant.parse("2021-12-31T23:20:56.630714800Z"));
        t1.setAuthor(Const.PN_PAPER_CHANNEL);
        t1.setDescription("Gara 2021");
        t1.setStatus(TenderDTO.StatusEnum.ENDED.getValue());
        this.tenderDAO.createOrUpdate(t1).block();

        t2.setTenderCode("GARA-2022");
        t2.setDate(Instant.parse("2022-01-01T00:20:56.630714800Z"));
        t2.setStartDate(Instant.parse("2022-01-01T00:20:56.630714800Z"));
        t2.setEndDate(Instant.parse("2022-12-31T23:20:56.630714800Z"));
        t2.setAuthor(Const.PN_PAPER_CHANNEL);
        t2.setDescription("Gara 2022");
        t2.setStatus(TenderDTO.StatusEnum.ENDED.getValue());
        this.tenderDAO.createOrUpdate(t2).block();

        active.setTenderCode("GARA-2023");
        active.setDate(Instant.parse("2023-01-01T00:20:56.630714800Z"));
        active.setStartDate(Instant.now().minus(10, ChronoUnit.DAYS));
        active.setEndDate(Instant.now().plus(3, ChronoUnit.DAYS));
        active.setAuthor(Const.PN_PAPER_CHANNEL);
        active.setDescription("Gara 2023");
        active.setStatus(TenderDTO.StatusEnum.VALIDATED.getValue());
        this.tenderDAO.createOrUpdate(active).block();

        validate.setTenderCode("GARA-2023-validated");
        validate.setDate(Instant.parse("2023-01-01T00:20:56.630714800Z"));
        validate.setStartDate(Instant.parse("2023-01-10T00:20:56.630714800Z"));
        validate.setEndDate(Instant.parse("2023-01-15T23:20:56.630714800Z"));
        validate.setAuthor(Const.PN_PAPER_CHANNEL);
        validate.setDescription("Gara validated 2023");
        validate.setStatus(TenderDTO.StatusEnum.VALIDATED.getValue());
        this.tenderDAO.createOrUpdate(validate).block();

        validate1.setTenderCode("GARA-2023-febbraio");
        validate1.setDate(Instant.parse("2023-02-15T00:20:56.630714800Z"));
        validate1.setStartDate(Instant.parse("2023-01-16T00:20:56.630714800Z"));
        validate1.setEndDate(Instant.parse("2023-01-20T23:20:56.630714800Z"));
        validate1.setAuthor(Const.PN_PAPER_CHANNEL);
        validate1.setDescription("Gara gennaio 2023");
        validate1.setStatus(TenderDTO.StatusEnum.VALIDATED.getValue());
        this.tenderDAO.createOrUpdate(validate1).block();

    }



}
