package org.votingsystem.crypto.xml;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DateUtils {

    private static final int SECOND = 1000;
    private static final int MINUTE = 60 * SECOND;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;

    //https://developer.android.com/reference/java/text/SimpleDateFormat.html
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss 'GMT'XXX");
    private static final DateFormat isoDateFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    static {
        isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    private static final SimpleDateFormat[] ACCEPTED_TIMESTAMP_FORMATS = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
            new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z", Locale.US)
    };



    public static boolean inRange(Date initDate, Date lapsedDate, long timeRange) {
        return initDate.getTime() - lapsedDate.getTime() < timeRange;
    }

    public static int getDayOfMonthFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int getMonthFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH);
    }

    public static int getYearFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    public static Date getDate(String dateString) {
        try {
            if (dateString.endsWith("Z")) return isoDateFormat.parse(dateString);
            else return dateFormat.parse(dateString);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Date getDate(String dateString, String format) throws ParseException {
        try {
            DateFormat formatter = new SimpleDateFormat(format);
            return formatter.parse(dateString);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String getISODateStr(Date date) {
        return isoDateFormat.format(date);
    }

    public static String getDateStr(Date date) {
        return dateFormat.format(date);
    }

    public static String getDateStr(Date date, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static String getElapsedTimeHoursMinutesFromMilliseconds(long milliseconds) {
        String format = String.format("%%0%dd", 2);
        long elapsedTime = milliseconds / 1000;
        String minutes = String.format(format, (elapsedTime % 3600) / 60);
        String hours = String.format(format, elapsedTime / 3600);
        String time = hours + ":" + minutes;
        return time;
    }

    public static String getElapsedTimeHoursMinutesSecondsFromMilliseconds(long milliseconds) {
        String format = String.format("%%0%dd", 2);
        long elapsedTime = milliseconds / 1000;
        String seconds = String.format(format, elapsedTime % 60);
        String minutes = String.format(format, (elapsedTime % 3600) / 60);
        String hours = String.format(format, elapsedTime / 3600);
        String time = hours + ":" + minutes + ":" + seconds;
        return time;
    }

    public static Calendar addDays(int numDias) {
        Calendar today = Calendar.getInstance();
        today.add(Calendar.DATE, numDias);
        return today;
    }

    public static Calendar addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal;
    }

    public static String getDayWeekDateStr(Date date, String hourFormat) {
        if (hourFormat == null) hourFormat = "HH:mm";
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if (Calendar.getInstance().get(Calendar.YEAR) != calendar.get(Calendar.YEAR))
            return getDateStr(date, "dd MMM yyyy' '" + hourFormat);
        else return getDateStr(date, "EEE dd MMM' '" + hourFormat);
    }

    public static String getDayWeekDateSimpleStr(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if (Calendar.getInstance().get(Calendar.YEAR) != calendar.get(Calendar.YEAR))
            return getDateStr(date, "dd MMM yyyy");
        else return getDateStr(date, "EEE dd MMM");
    }

    public static Date getDayWeekDate(String dateStr) throws ParseException {
        try {
            return getDate(dateStr, "dd MMM yyyy' 'HH:mm");
        } catch (Exception ex) {
            Calendar resultCalendar = Calendar.getInstance();
            resultCalendar.setTime(getDate(dateStr, "EEE dd MMM' 'HH:mm"));
            resultCalendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
            return resultCalendar.getTime();
        }
    }

    public static String getElapsedTimeStr(Date end) {
        Float hours = (end.getTime() - Calendar.getInstance().getTime().getTime()) / (60 * 60 * 1000F);
        return Integer.valueOf(hours.intValue()).toString();
    }

    public static Calendar getMonday(Calendar calendar) {
        Calendar result = (Calendar) calendar.clone();
        result.add(Calendar.DAY_OF_YEAR, -7);
        result.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        result.set(Calendar.HOUR_OF_DAY, 24);
        result.set(Calendar.MINUTE, 0);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);
        return result;
    }

}