package it.pagopa.pn.paperchannel.utils;

import io.swagger.v3.oas.models.security.SecurityScheme;
import it.pagopa.pn.paperchannel.exception.PnGenericException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.util.Pair;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.BADLY_FILTER_REQUEST;

@Slf4j
public class DateUtils {
    private static final String START_DATE = "2020-01-01T10:15:30";
    private static final ZoneId italianZoneId =  ZoneId.of("Europe/Rome");

    private DateUtils(){}

    public static String formatDate(Date date)  {
        if (date == null) return null;
        LocalDateTime dateTime =  LocalDateTime.ofInstant(date.toInstant(), italianZoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return dateTime.format(formatter);
    }

    public static Date parseDateString(String date) {
        if (StringUtils.isBlank(date)) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime localDate = LocalDateTime.parse(date, formatter);
        ZonedDateTime time = localDate.atZone(italianZoneId);
        return Date.from(time.toInstant());
    }

    public static Instant parseStringTOInstant(String date) {
        return Instant.parse(date);
    }

    public static Long getTimeStampOfMills(LocalDateTime time){
        return time.toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    public static OffsetDateTime getOffsetDateTimeFromDate(Date date) {
        return OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    public static Date getDatefromOffsetDateTime(OffsetDateTime offsetDateTime) {
        return Date.from(offsetDateTime.toInstant());
    }

    public static Pair<Instant, Instant> getStartAndEndTimestamp(Date startDate, Date endDate){
        Instant start = Instant.EPOCH;
        Instant end = Instant.now();

        if (startDate != null){
            start = startDate.toInstant();
        }

        if (endDate != null){
            end = endDate.toInstant();
        }


        if (start.isAfter(end)){
            throw new PnGenericException(BADLY_FILTER_REQUEST, BADLY_FILTER_REQUEST.getMessage());
        }

        return Pair.of(start, end);
    }

    public static Instant addedTime(Integer first, Integer second){
        Instant now = Instant.now();
        int tot = first * second;
        return now.plus(tot, ChronoUnit.MINUTES);
    }

    public static Date formatDateWithSpecificHour(Date date, int hour, int min, int sec){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, sec);
        cal.set(Calendar.MILLISECOND, 0);
        Date dateTime = cal.getTime();

        return dateTime;
    }

}
