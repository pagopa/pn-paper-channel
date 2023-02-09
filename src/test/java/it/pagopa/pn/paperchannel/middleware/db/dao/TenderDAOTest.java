package it.pagopa.pn.paperchannel.middleware.db.dao;

import it.pagopa.pn.paperchannel.config.BaseTest;
import it.pagopa.pn.paperchannel.middleware.db.entities.PnTender;
import it.pagopa.pn.paperchannel.utils.Const;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class TenderDAOTest extends BaseTest {

    @Autowired
    private TenderDAO tenderDAO;
    private final PnTender t1 = new PnTender();
    private final PnTender t2 = new PnTender();
    private final PnTender active = new PnTender();
    private final PnTender draft = new PnTender();


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
        assertEquals(4, alls.size());
    }



    private void initialize(){
        t1.setTenderCode("GARA-2021");
        t1.setDate(Instant.parse("2021-01-01T00:20:56.630714800Z"));
        t1.setStartDate(Instant.parse("2021-01-01T00:20:56.630714800Z"));
        t1.setEndDate(Instant.parse("2021-12-31T23:20:56.630714800Z"));
        t1.setAuthor(Const.PN_PAPER_CHANNEL);
        t1.setDescription("Gara 2021");
        t1.setStatus("ENDED");
        this.tenderDAO.createOrUpdate(t1).block();

        t2.setTenderCode("GARA-2022");
        t2.setDate(Instant.parse("2022-01-01T00:20:56.630714800Z"));
        t2.setStartDate(Instant.parse("2022-01-01T00:20:56.630714800Z"));
        t2.setEndDate(Instant.parse("2022-12-31T23:20:56.630714800Z"));
        t2.setAuthor(Const.PN_PAPER_CHANNEL);
        t2.setDescription("Gara 2022");
        t2.setStatus("ENDED");
        this.tenderDAO.createOrUpdate(t2).block();

        active.setTenderCode("GARA-2023");
        active.setDate(Instant.parse("2023-01-01T00:20:56.630714800Z"));
        active.setStartDate(Instant.parse("2023-01-01T00:20:56.630714800Z"));
        active.setEndDate(Instant.parse("2023-12-31T23:20:56.630714800Z"));
        active.setAuthor(Const.PN_PAPER_CHANNEL);
        active.setDescription("Gara 2023");
        active.setStatus("IN_PROGRESS");
        this.tenderDAO.createOrUpdate(active).block();

        draft.setTenderCode("GARA-2023-draft");
        draft.setDate(Instant.parse("2023-01-01T00:20:56.630714800Z"));
        draft.setStartDate(Instant.parse("2023-01-01T00:20:56.630714800Z"));
        draft.setEndDate(Instant.parse("2023-12-31T23:20:56.630714800Z"));
        draft.setAuthor(Const.PN_PAPER_CHANNEL);
        draft.setDescription("Gara 2023");
        draft.setStatus("CREATED");
        this.tenderDAO.createOrUpdate(draft).block();
    }



}
