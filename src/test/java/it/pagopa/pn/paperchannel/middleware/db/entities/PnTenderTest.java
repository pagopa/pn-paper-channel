package it.pagopa.pn.paperchannel.middleware.db.entities;


import it.pagopa.pn.paperchannel.generated.openapi.server.v1.dto.TenderDTO;
import it.pagopa.pn.paperchannel.utils.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

class PnTenderTest {
    private PnTender pnTender;
    public String tenderCode;
    public Instant date;
    public String description;
    public String status;
    public String author;
    public Instant startDate;
    public Instant endDate;


    @BeforeEach
    void setUp(){
        this.initialize();
    }

    @Test
    void actualStatusTest() {
        pnTender = initTender();
        pnTender.setStartDate(getUpdatedDate(-1));
        pnTender.setEndDate(getUpdatedDate(1));
        String status = pnTender.getActualStatus();
        Assertions.assertEquals(status, TenderDTO.StatusEnum.IN_PROGRESS.getValue());

        pnTender = initTender();
        pnTender.setEndDate(getUpdatedDate(-1));
        status = pnTender.getActualStatus();
        Assertions.assertEquals(status, TenderDTO.StatusEnum.ENDED.getValue());

        pnTender = initTender();
        pnTender.setStatus(TenderDTO.StatusEnum.CREATED.toString());
        status = pnTender.getActualStatus();
        Assertions.assertEquals(status, TenderDTO.StatusEnum.CREATED.getValue());
    }

    @Test
    void toStringTest() {
        pnTender = initTender();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pnTender.getClass().getSimpleName());
        stringBuilder.append("(");
        stringBuilder.append("tenderCode=");
        stringBuilder.append(tenderCode);
        stringBuilder.append(", ");
        stringBuilder.append("date=");
        stringBuilder.append(date);
        stringBuilder.append(", ");
        stringBuilder.append("description=");
        stringBuilder.append(description);
        stringBuilder.append(", ");
        stringBuilder.append("status=");
        stringBuilder.append(status);
        stringBuilder.append(", ");
        stringBuilder.append("author=");
        stringBuilder.append(author);
        stringBuilder.append(", ");
        stringBuilder.append("startDate=");
        stringBuilder.append(startDate);
        stringBuilder.append(", ");
        stringBuilder.append("endDate=");
        stringBuilder.append(endDate);
        stringBuilder.append(")");

        String toTest = stringBuilder.toString();
        Assertions.assertEquals(toTest, pnTender.toString());
    }

    public Instant getUpdatedDate(int hours) {
        Date date = Date.from(Instant.now());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().toInstant();
    }

    private PnTender initTender() {
        PnTender pnTender = new PnTender();
        pnTender.setAuthor(author);
        pnTender.setDate(date);
        pnTender.setStatus(status);
        pnTender.setTenderCode(tenderCode);
        pnTender.setDescription(description);
        pnTender.setStartDate(startDate);
        pnTender.setEndDate(endDate);
        return pnTender;
    }

    private void initialize() {
        author = Const.PN_PAPER_CHANNEL;
        date = Instant.now();
        status = TenderDTO.StatusEnum.VALIDATED.toString();
        tenderCode = "FRGTHYJUKILO";
        description = "DESCRIPTION";
        startDate = Instant.now();
        endDate = Instant.now();
    }
}
