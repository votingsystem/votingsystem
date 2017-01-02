package org.votingsystem.util;

import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.throwable.ExceptionBase;

import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;


/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class DateUtils {

    public static String APPLICATION_DATE_PATTERN = "yyyy-MM-dd' 'HH:mm:ss OOOO";

    public static String getISO_OFFSET_DATE_TIME(LocalDateTime date) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.of(date, ZoneId.systemDefault()));
    }

    public static ZonedDateTime getDateFromISO_OFFSET_DATE_TIME(String dateStr) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault());
    }

    /**
     * Date format pattern used to parse HTTP date headers in ANSI C asctime() format.
     */
    public static String getASCTIME (LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy");
        return formatter.format(date);
    }

    /**
     * Date format pattern used to parse HTTP date headers in RFC 1036 format.
     */
    public static String getRFC1036 (ZonedDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz");
        return formatter.format(date);
    }

    /**
     * Date format pattern used to parse HTTP date headers in RFC FC1123 format.
     */
    public static String getRFC1123 (ZonedDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz");
        return formatter.format(date);
    }

    public static LocalDateTime getDateFromURL (String dateStr) throws ParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        return LocalDateTime.parse(dateStr, formatter);
    }

    public static LocalDateTime getDate (String dateStr, String pattern) throws ParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(dateStr, formatter);
    }

    /**
     * The trailing spaces are a hack to solve a bug:
     * http://stackoverflow.com/questions/37287103/why-does-gmt8-fail-to-parse-with-pattern-o-despite-being-copied-straight-ou
     *
     * @param dateStr
     * @return
     * @throws ParseException
     */
    public static LocalDateTime getDate (String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(APPLICATION_DATE_PATTERN + " ");
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateStr + " ", formatter);
            return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception ex) {
            return ZonedDateTime.parse(dateStr).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }
    }

    public static ZonedDateTime getZonedDateTime(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(APPLICATION_DATE_PATTERN + " ");
            return ZonedDateTime.parse(dateStr + " ", formatter);
        } catch (Exception ex) {
            return ZonedDateTime.parse(dateStr).withZoneSameInstant(ZoneId.systemDefault());
        }
    }

    public static String getDateStr(LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(APPLICATION_DATE_PATTERN);
        return formatter.format(ZonedDateTime.of(date, ZoneId.systemDefault()));
    }

    public static String getDateStr(ZonedDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(APPLICATION_DATE_PATTERN);
        return formatter.format(date);
    }

    public static String getShortDateStr (LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
                .withZone( ZoneId.systemDefault());
        return formatter.format(date);
    }

    public static String getDateStr (LocalDateTime date, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.format(date);
    }

    public static String getDirPath (LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("/yyyy/MMM/dd/");
        return formatter.format(date);
    }

    public static String getDirSufix (LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MMM_dd");
        return formatter.format(date);
    }

    public static String getTimePeriodPath (Interval timePeriod) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MMM_dd");
        return "/" + formatter.format(timePeriod.getDateFrom()) + "__" +  formatter.format(timePeriod.getDateTo());
    }

    public static LocalDateTime getDateFromPath (String dateStr) throws ParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("/yyyy/MMM/dd/");
        return LocalDateTime.parse(dateStr, formatter);
    }

    public static String getURLPath (LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("/yyyy/MM/dd/");
        return formatter.format(date);
    }

    public static boolean checkIfTimeLimited(LocalDateTime notBefore, LocalDateTime notAfter) {
        Long days = notBefore.until(notAfter, ChronoUnit.DAYS);
        return days <= 7;//one week
    }

    /**
     *  elapsed time in hours/minutes/seconds
     * @return String
     */
    public static String getElapsedHoursMinutesSeconds(LocalDateTime dateBegin, LocalDateTime dateFinish) {
        StringBuilder result = new StringBuilder();
        LocalDateTime tempDateTime = LocalDateTime.from(dateBegin);
        Long years = tempDateTime.until(dateFinish, ChronoUnit.YEARS);
        if(years > 0) result.append(years + " years ");
        tempDateTime = tempDateTime.plusYears(years);

        Long months = tempDateTime.until(dateFinish, ChronoUnit.MONTHS);
        tempDateTime = tempDateTime.plusMonths( months );
        if(months > 0) result.append(months + " months ");

        Long days = tempDateTime.until( dateFinish, ChronoUnit.DAYS);
        tempDateTime = tempDateTime.plusDays(days);
        if(days > 0) result.append(days + " days ");

        Long hours = tempDateTime.until(dateFinish, ChronoUnit.HOURS);
        tempDateTime = tempDateTime.plusHours(hours);
        Long minutes = tempDateTime.until(dateFinish, ChronoUnit.MINUTES);
        tempDateTime = tempDateTime.plusMinutes( minutes );
        Long seconds = tempDateTime.until(dateFinish, ChronoUnit.SECONDS);
        return result.append(String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" +
                String.format("%02d", seconds)).toString();
    }

    public static LocalDateTime getMonday(LocalDateTime selectedDate) {
        return selectedDate.withNano(0).withSecond(0).withMinute(0).withHour(0)
                .with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
    }

    public static LocalDateTime getNexMonday(LocalDateTime selectedDate) {
        return selectedDate.withNano(0).withSecond(0).withMinute(0).withHour(0)
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    public static Interval getYearPeriod(LocalDateTime selectedDate) {
        LocalDateTime dayFrom = selectedDate.withNano(0).withSecond(0).withMinute(0).withHour(0).withDayOfYear(1);
        return new Interval(dayFrom, selectedDate);
    }

    public static Interval getMonthPeriod(LocalDateTime selectedDate) {
        LocalDateTime dayFrom = selectedDate.withNano(0).withSecond(0).withMinute(0).withHour(0).withDayOfMonth(1);
        return new Interval(dayFrom, selectedDate);
    }

    public static Interval getWeekPeriod(LocalDateTime selectedDate) {
        LocalDateTime previousMonday = selectedDate.withNano(0).withSecond(0).withMinute(0).withHour(0)
                .with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        return new Interval(previousMonday, selectedDate);
    }

    public static Interval getDayPeriod(LocalDateTime selectedDate) {
        LocalDateTime from = selectedDate.withNano(0).withSecond(0).withMinute(0).withHour(0);
        LocalDateTime to = selectedDate.plus(1, ChronoUnit.DAYS).withNano(0).withSecond(0).withMinute(0).withHour(0);
        return new Interval(from, to);
    }

    public static Interval getHourPeriod(LocalDateTime selectedDate) {
        LocalDateTime from = selectedDate.withNano(0).withSecond(0).withMinute(0);
        LocalDateTime to = selectedDate.plus(1, ChronoUnit.HOURS).withNano(0).withSecond(0).withMinute(0);
        return new Interval(from, to);
    }

    public static Interval getMinutePeriod(LocalDateTime selectedDate) {
        LocalDateTime from = selectedDate.withNano(0).withSecond(0);
        LocalDateTime to = selectedDate.plus(1, ChronoUnit.MINUTES).withNano(0).withSecond(0);
        return new Interval(from, to);
    }

    public static Interval getSecondPeriod(LocalDateTime selectedDate) {
        LocalDateTime from = selectedDate.withNano(0);
        LocalDateTime to = selectedDate.plus(1, ChronoUnit.SECONDS).withNano(0);
        return new Interval(from, to);
    }

    public static Interval getLapsePeriod(LocalDateTime selectedDate, Interval.Lapse timePeriodLapse) throws ExceptionBase {
        switch(timePeriodLapse) {
            case YEAR: return getYearPeriod(selectedDate);
            case MONTH: return getMonthPeriod(selectedDate);
            case WEEK: return getWeekPeriod(selectedDate);
            case DAY: return getDayPeriod(selectedDate);
            case HOUR: return getHourPeriod(selectedDate);
            case MINUTE: return getMinutePeriod(selectedDate);
            case SECOND: return getSecondPeriod(selectedDate);
        }
        throw new ExceptionBase("Unsupported Lapse period: '" + timePeriodLapse + "'");
    }

    public static LocalDateTime getDayFromPreviousWeek(LocalDateTime selectedDate) {
        return selectedDate.plus(-7, ChronoUnit.DAYS);
    }

    public static Interval getURLTimePeriod(String dateFromStr, String dateToStr) throws ParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        LocalDateTime dateFrom = LocalDateTime.parse(dateFromStr, formatter);
        LocalDateTime dateTo = LocalDateTime.parse(dateToStr, formatter);
        return new Interval(dateFrom, dateTo);
    }

    public static LocalDateTime getURLPart(String dateStr) throws ParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        return LocalDateTime.parse(dateStr, formatter);
    }

    public static String getDayWeekDateStr (LocalDateTime date, String hourFormat) {
        if(date.getYear() == LocalDateTime.now().getYear())
            return getDateStr(date, "dd MMM yyyy' '" + hourFormat);
        else return getDateStr(date, "EEE dd MMM' '" + hourFormat);
    }

    public static LocalDateTime getDayWeekDate (String dateStr) throws ParseException {
        try {
            return getDate (dateStr, "dd MMM yyyy' 'HH:mm");
        } catch (Exception ex) {
            return getDate (dateStr, "EEE dd MMM' 'HH:mm");
        }
    }

    public static LocalDateTime getLocalDateFromUTCDate(Date utcDate) {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(utcDate.toInstant(),
                ZoneId.systemDefault()).withZoneSameLocal(ZoneId.of("UTC"));
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static Date getUTCDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC")).withZoneSameLocal(ZoneId.systemDefault()).toInstant());
    }

    public static Date getUTCDateNow() {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * **genTime** is the time at which the time-stamp token has been created by the TSA.  It is expressed as UTC time
     * (Coordinated Universal Time) to reduce confusion with the local time zone use.
     * @return
     */
    public static LocalDateTime getLocalDate(TimeStampToken timeStampToken) {
        return getLocalDateFromUTCDate(timeStampToken.getTimeStampInfo().getGenTime());
    }

    /**
     * Certificate time comparison is done in the UTC time zone. The certificate expiry
     date is converted to UTC and a comparison made to the millisecond.
     * @param certificateDate
     * @return
     */
    public static LocalDateTime getCertificateLocalDate(Date certificateDate) {
        return getLocalDateFromUTCDate(certificateDate);
    }

}