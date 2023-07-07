package it.pagopa.pn.paperchannel.utils;

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
    private static final ZoneId italianZoneId =  ZoneId.of("Europe/Rome");

    private DateUtils(){}

    public static String formatDate(Instant date)  {
        if (date == null) return null;
        return date.toString();
    }

    private static Instant parseOldStringToInstant(String date) {
        if (StringUtils.isBlank(date)) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime localDate = LocalDateTime.parse(date, formatter);
        ZonedDateTime time = localDate.atZone(italianZoneId);
        return time.toInstant();
    }

    public static Instant parseStringTOInstant(String date) {
        if (StringUtils.isBlank(date)) return null;
        if (!StringUtils.contains("Z", date)) return parseOldStringToInstant(date);
        return Instant.parse(date);
    }

    public static Long getTimeStampOfMills(LocalDateTime time){
        return time.toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    public static OffsetDateTime getOffsetDateTimeFromDate(Instant date) {
        return OffsetDateTime.ofInstant(date, ZoneOffset.UTC);
    }

    public static Instant getDatefromOffsetDateTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime.toInstant();
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
        return cal.getTime();
    }

}
