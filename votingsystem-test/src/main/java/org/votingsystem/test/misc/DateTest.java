package org.votingsystem.test.misc;

import org.votingsystem.util.DateUtils;
import org.votingsystem.xml.XML;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DateTest {

    private static final Logger log = Logger.getLogger(DateTest.class.getName());

    public static void main(String[] args) throws Exception {

        LocalDate localDate = LocalDate.parse("2018 05 01", DateTimeFormatter.ofPattern("yyyy MM dd"));
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
        log.info("zonedDateTime: " + zonedDateTime);
        log.info("LocalDateTime: " + zonedDateTime.toLocalDateTime());

    }


    public static Date getUTCDate1(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC")).withZoneSameLocal(ZoneId.systemDefault()).toInstant());
    }

    public static Date getUTCDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC")).withZoneSameLocal(ZoneId.systemDefault()).toInstant());
    }


    public static void test1() throws Exception {
        LocalDate localDate = LocalDate.parse("2016-11-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        //ZonedDateTime zonedDateBegin = localDate.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
        ZonedDateTime zonedDateBegin = localDate.atStartOfDay(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault());
        LocalDateTime dateBegin = zonedDateBegin.toLocalDateTime();

        DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        log.info("FORMATTER: " + zonedDateBegin.format(FORMATTER));

        log.info("zonedDateBegin: " + zonedDateBegin.toString() + " - dateBegin: " + dateBegin);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        log.info("getDateStr: " + LocalDateTime.now().format(formatter));
        log.info("getDateStr: " + zonedDateBegin.toLocalDateTime().format(formatter));
    }

    public static void testGMT() throws Exception {
        String dateStr = DateUtils.getDateStr(LocalDateTime.now());
        log.info("dateStr: " + dateStr);
        LocalDateTime dateTime = DateUtils.getDate(dateStr);
        log.info("LocalDateTime: " + dateTime);
        String dateStr1 = "2016-12-17 19:41:00 GMT+01:00";
        log.info("dateStr1: " + dateStr1 + " - LocalDateTime: " + DateUtils.getDate(dateStr1));
        String dateStr2 = "2016-12-17 19:41:00 GMT+08:00";
        log.info("dateStr2: " + dateStr2 + " - LocalDateTime: " + DateUtils.getDate(dateStr2));
    }

    //https://developer.android.com/reference/java/text/SimpleDateFormat.html
    public static void testAndroid() throws Exception {
        //android doesn't support de 'X' date pattern
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ZZZZ");
        DateFormat xmlDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ");
        String dateStr1 = dateFormat.format(new Date());
        String dateStr2 = "2016-12-24 09:05:09.458 GMT-01:00";
        String xmlDate = "2016-12-24T11:45:18.797+08:00";

        log.info("dateStr1: " + dateStr1 + " - date: " + dateFormat.parse(dateStr1));
        Date date2 = dateFormat.parse(dateStr2);
        log.info("dateStr2: " + dateStr2 + " - date: " + date2 );
        log.info("xmlDate formatted: " + xmlDateFormat.format(new Date()));
        log.info("xmlDate: " + xmlDate + " - date: " + xmlDateFormat.parse(xmlDate) );
    }

    public static void serializeXML() throws Exception {
        TestDate testDate = new TestDate(ZonedDateTime.now());
        String testDateStr = XML.getMapper().writeValueAsString(testDate);
        log.info("testDateStr: " + testDateStr);
        testDate = XML.getMapper().readValue(testDateStr.getBytes(), TestDate.class);
        log.info("testDate: " + testDate);
        String androidXML = "<TestDate><date>2016-12-24T10:55:01.641+0100</date></TestDate>";
        testDate = XML.getMapper().readValue(androidXML.getBytes(), TestDate.class);
    }

    private static void testAmounts() {
        Duration duration = Duration.ofHours(2);
        System.out.println("duration: " + duration + " - multipliedBy: " + duration.multipliedBy(3));
        LocalDateTime dt = LocalDateTime.now();
        dt = dt.plus(duration);
        System.out.println("LocalDateTime: " + dt + " - plus(duration): " + dt);
        Period period = Period.ofMonths(6);
        System.out.println("period: " + period + " - plusDays(10): " + period.plusDays(10));
    }

    public static void testUTC0() {
        ZonedDateTime z1 = ZonedDateTime.now();
        ZonedDateTime z2 = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
        System.out.println("z1: " + z1 + " - z2: " + z2);
    }

    public static void testUTC1() {
        System.out.println("Default ZoneId: " + ZoneId.systemDefault());

        ZonedDateTime z1 = ZonedDateTime.now();
        System.out.println("now: " + z1 + " - zone: " + z1.getZone() + " - offset: " + z1.getOffset());

        ZonedDateTime z = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("UTC"));
        System.out.println(z + " - getZone: " + z.getZone() + " - getOffset: " + z.getOffset());
        LocalDateTime timePoint = LocalDateTime.now();
        ZonedDateTime zoned = ZonedDateTime.of(timePoint, ZoneId.of("Europe/Paris"));
        System.out.println(zoned);
    }

    public static void testUTC2() {
        LocalDateTime  ldt = LocalDateTime.of(2014, Month.JUNE,  21,   16,   30);

        ZoneId  usCentral = ZoneId.of("America/Chicago");
        ZonedDateTime zdt   = ZonedDateTime.of(ldt, usCentral);
        System.out.println("In US  Central Time Zone:"  + zdt);

        ZoneId  losAngeles = ZoneId.of("America/Los_Angeles");
        ZonedDateTime zdt2   = zdt.withZoneSameInstant(losAngeles);
        System.out.println("In  America/Los_Angeles Time Zone:"  + zdt2);

        ZoneId  utc = ZoneId.of("UTC");
        ZonedDateTime zdt3  = zdt.withZoneSameInstant(utc);
        System.out.println("In  utc:"  + zdt3);
    }

    public static void testUTC3() {
        ZonedDateTime nowSpain = ZonedDateTime.now();
        ZonedDateTime nowUTC = nowSpain.withZoneSameInstant(ZoneId.of("UTC"));
        System.out.println("nowSpain:"  + nowSpain + " - utc: " + nowUTC);
    }

    public static class TestDate {
        private ZonedDateTime date;
        TestDate(){}
        TestDate(ZonedDateTime date){
            this.date = date;
        }
        public ZonedDateTime getDate() {
            return date;
        }
        public void setDate(ZonedDateTime date) {
            this.date = date;
        }
    }

}
