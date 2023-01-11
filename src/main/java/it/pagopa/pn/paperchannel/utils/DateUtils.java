package it.pagopa.pn.paperchannel.utils;

import it.pagopa.pn.paperchannel.exception.PnGenericException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cglib.core.Local;
import org.springframework.data.util.Pair;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static it.pagopa.pn.paperchannel.exception.ExceptionTypeEnum.BADLY_FILTER_REQUEST;

@Slf4j
public class DateUtils {
    private static final Long START_TIMESTAMP = 1672527600L;
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

    public static Long getTimeStampOfMills(LocalDateTime time){
        return time.toInstant(ZoneOffset.UTC).getEpochSecond();
    }


    public static OffsetDateTime getOffsetDateTime(String date){
        return LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atOffset(ZoneOffset.UTC);
    }

    public static OffsetDateTime getOffsetDateTimeFromDate(Date date) {
        //return OffsetDateTime.ofInstant(date.toInstant(), italianZoneId);
        return OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    public static Pair<Long, Long> getStartAndEndTimestamp(String startDate, String endDate){
        Long start = START_TIMESTAMP;
        Long end = getTimeStampOfMills(LocalDateTime.now());
        if (StringUtils.isNotBlank(startDate)){
            start = getTimeStampOfMills(LocalDateTime.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (StringUtils.isNotBlank(endDate)){
            end = getTimeStampOfMills(LocalDateTime.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        if (start < end){
            throw new PnGenericException(BADLY_FILTER_REQUEST, BADLY_FILTER_REQUEST.getMessage());
        }
        return Pair.of(start, end);
    }

    /*

    public static String formatTime(ZonedDateTime datetime) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        return datetime.format(formatter.withZone(italianZoneId));
    }

    public static LocalDate getLocalDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return LocalDate.parse(date, formatter);
    }

    public static ZonedDateTime parseDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        LocalDate locdate = LocalDate.parse(date, formatter);

        return locdate.atStartOfDay(italianZoneId);
    }

    public static ZonedDateTime atStartOfDay(Instant instant) {
        LocalDate locdate = LocalDate.ofInstant(instant, italianZoneId);
        return locdate.atStartOfDay(italianZoneId);
    }

    public static ZonedDateTime parseTime(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        return formatter.parse(date, ZonedDateTime::from);
    }

    public static String formatDate(Instant instant) {
        if (instant == null)
            return null;

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        return LocalDate.ofInstant(instant, italianZoneId).format(formatter);
    }
    */
}
