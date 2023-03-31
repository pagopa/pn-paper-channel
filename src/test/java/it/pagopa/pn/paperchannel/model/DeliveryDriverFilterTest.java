package it.pagopa.pn.paperchannel.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

class DeliveryDriverFilterTest {

    private Integer page;
    private Integer size;
    private Boolean status;
    private Date startDate;
    private Date endDate;

    @BeforeEach
    void setUp(){
        this.initialize();
    }
    @Test
    void setGetTest() {
        DeliveryDriverFilter deliveryDriverFilter = new DeliveryDriverFilter(page, size, status, startDate, endDate);
        Assertions.assertNotNull(deliveryDriverFilter);
        Assertions.assertEquals(page, deliveryDriverFilter.getPage());
        Assertions.assertEquals(size, deliveryDriverFilter.getSize());
        Assertions.assertEquals(status, deliveryDriverFilter.getStatus());
        Assertions.assertEquals(startDate, deliveryDriverFilter.getStartDate());
        Assertions.assertEquals(endDate, deliveryDriverFilter.getEndDate());

        Integer page = 10;
        Integer size = 1;
        Boolean status = false;
        Date startDate = Date.from(getUpdatedDate(10));
        Date endDate = Date.from(getUpdatedDate(20));

        deliveryDriverFilter.setPage(page);
        deliveryDriverFilter.setSize(size);
        deliveryDriverFilter.setStatus(status);
        deliveryDriverFilter.setStartDate(startDate);
        deliveryDriverFilter.setEndDate(endDate);

        Assertions.assertEquals(page, deliveryDriverFilter.getPage());
        Assertions.assertEquals(size, deliveryDriverFilter.getSize());
        Assertions.assertEquals(status, deliveryDriverFilter.getStatus());
        Assertions.assertEquals(startDate, deliveryDriverFilter.getStartDate());
        Assertions.assertEquals(endDate, deliveryDriverFilter.getEndDate());
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

    private void initialize() {
        page = 1;
        size = 10;
        status = true;
        startDate = Date.from(Instant.now());
        endDate = Date.from(getUpdatedDate(2));
    }
}
