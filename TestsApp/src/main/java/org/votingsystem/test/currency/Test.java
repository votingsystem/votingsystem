package org.votingsystem.test.currency;

import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

public class Test {

    static Logger log;

    public static void main(String[] args) throws Exception {
        log = TestUtils.init(Test.class);
        System.exit(0);
    }

    public static void isoDate() throws Exception {
        Logger log = TestUtils.init(Test.class);
        Calendar calendar = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = calendar.getTime();
        log.info("date:" + date.toString());
        String dateStr = dateFormat.format(date);
        log.info("ISO formatted date:" + dateStr);
        Date parsedDate = DateUtils.getDateFromString(dateStr);
        log.info("parsed date:" + parsedDate.toString());
        Calendar parsedCalendar = javax.xml.bind.DatatypeConverter.parseDateTime(dateStr);
        log.info("parsedCalendar: " + parsedCalendar.getTime());
        System.exit(0);
    }
}




