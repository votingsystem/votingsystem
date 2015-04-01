package org.votingsystem.util;

import org.votingsystem.throwable.ExceptionVS;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class DateUtils {

    private static Logger log = Logger.getLogger(DateUtils.class.getSimpleName());

    private static final DateFormat urlDateFormatter = new SimpleDateFormat("yyyyMMdd_HHmm");
    private static final DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    static {
        isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static Calendar getCalendar(int year, int month, int day){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);//Zero based
        calendar.set(Calendar.DAY_OF_MONTH, day);
        return calendar;
    }

    public static Calendar addDays(Date date, int days){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal;
    }

    public static Calendar addDays(int numDias) {
        Calendar today = Calendar.getInstance();
        today.add(Calendar.DATE, numDias);
        return today;
    }

    public static int getDayOfMonthFromDate (Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int getMonthFromDate (Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH);
    }

    public static int getYearFromDate (Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    public static Date getDateFromString (String dateString) throws ParseException {
        if(dateString.endsWith("Z")) return isoDateFormat.parse(dateString);
        else {
            DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            return formatter.parse(dateString);
        }
    }

    public static Date getDateFromString (String dateString, Locale locale) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", locale);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.parse(dateString);
    }

    public static Date getDateFromURL (String dateString) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmm");
        return formatter.parse(dateString);
    }

    public static Date getDateFromString (String dateString, String format) throws ParseException {
        DateFormat formatter = new SimpleDateFormat(format);
        return formatter.parse(dateString);
    }

    public static String getISODateStr (Date date) {
        return isoDateFormat.format(date);
    }

    public static String getDateStr (Date date) {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd' 'HH:mm:ss");
        return formatter.format(date);
    }

    public static String getDateStr (Date date, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static String getDirPath (Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("/yyyy/MMM/dd/");
        return formatter.format(date);
    }

    public static String getDirSufix (Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MMM_dd");
        return formatter.format(date);
    }

    public static String getTimePeriodPath (TimePeriod timePeriod) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MMM_dd");
        return "/" + formatter.format(timePeriod.getDateFrom()) + "__" +  formatter.format(timePeriod.getDateTo());
    }

    public static Date getDateFromPath (String dateStr) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("/yyyy/MMM/dd/");
        return formatter.parse(dateStr);
    }

    public static String getURLPath (Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("/yyyy/MM/dd/");
        return formatter.format(date);
    }

    /**
     *  elapsed time in hours/minutes/seconds
     * @return String
     */
    public static String getElapsedTimeHoursMinutesFromMilliseconds(long milliseconds) {
        String format = String.format("%%0%dd", 2);
        long elapsedTime = milliseconds / 1000;
        String seconds = String.format(format, elapsedTime % 60);
        String minutes = String.format(format, (elapsedTime % 3600) / 60);
        String hours = String.format(format, elapsedTime / 3600);
        String time =  hours + ":" + minutes + ":" + seconds;
        return time;
    }
    
    /**
     *  elapsed time in hours/minutes/seconds/milliseconds
     * @return String
     */
    public static String getElapsedTimeHoursMinutesMillis(long milliseconds) {
        String format = String.format("%%0%dd", 2);
        String millisecondsFormat = String.format("%%0%dd", 3);
        long elapsedTime = milliseconds / 1000;
        String milliSeconds = String.format(millisecondsFormat, milliseconds % 1000);
        String seconds = String.format(format, elapsedTime % 60);
        String minutes = String.format(format, (elapsedTime % 3600) / 60);
        String hours = String.format(format, elapsedTime / 3600);
        String time =  hours + ":" + minutes + ":" + seconds + ":" + milliSeconds;
        return time;
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

    public static Calendar getNexMonday(Calendar calendar) {
        Calendar result = (Calendar) calendar.clone();
        result.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        result.set(Calendar.HOUR_OF_DAY, 24);
        result.set(Calendar.MINUTE, 0);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);
        return result;
    }

    public static Calendar getCalendar(Date date) {
        Calendar result = Calendar.getInstance();
        result.setTime(date);
        return result;
    }

    public static Calendar resetDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static Calendar resetCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static TimePeriod getCurrentWeekPeriod() {
        return getWeekPeriod(Calendar.getInstance());
    }

    public static TimePeriod getYearPeriod(Calendar selectedDate) {
        Calendar dayFrom = (Calendar) selectedDate.clone();
        dayFrom.set(Calendar.DAY_OF_YEAR, 1);
        dayFrom.set(Calendar.HOUR_OF_DAY, 0);
        dayFrom.set(Calendar.MINUTE, 0);
        dayFrom.set(Calendar.SECOND, 0);
        dayFrom.set(Calendar.MILLISECOND, 0);
        Calendar dayTo = (Calendar) dayFrom.clone();
        dayTo.add(Calendar.DAY_OF_YEAR, 365);
        return new TimePeriod(dayFrom.getTime(), dayTo.getTime());
    }

    public static TimePeriod getMonthPeriod(Calendar selectedDate) {
        Calendar dayFrom = (Calendar) selectedDate.clone();
        dayFrom.set(Calendar.DAY_OF_MONTH, 1);
        dayFrom.set(Calendar.HOUR_OF_DAY, 0);
        dayFrom.set(Calendar.MINUTE, 0);
        dayFrom.set(Calendar.SECOND, 0);
        dayFrom.set(Calendar.MILLISECOND, 0);
        Calendar dayTo = (Calendar) dayFrom.clone();
        dayTo.add(Calendar.DAY_OF_MONTH, dayFrom.getActualMaximum(Calendar.DAY_OF_MONTH));
        return new TimePeriod(dayFrom.getTime(), dayTo.getTime());
    }

    public static TimePeriod getWeekPeriod(Calendar selectedDate) {
        selectedDate.getTime();//to make sure set values are updated
        Calendar weekFromCalendar = getMonday(selectedDate);
        Calendar weekToCalendar = (Calendar) weekFromCalendar.clone();
        weekToCalendar.add(Calendar.DAY_OF_YEAR, 7);
        return new TimePeriod(weekFromCalendar.getTime(), weekToCalendar.getTime());
    }

    public static TimePeriod getWeekPeriod(Date selectedDate) {
        Calendar selectedDateCalendar = Calendar.getInstance();
        selectedDateCalendar.setTime(selectedDate);
        return getWeekPeriod(selectedDateCalendar);
    }

    public static TimePeriod addHours(Calendar baseCal, Integer numHours){
        Calendar cal = (Calendar) baseCal.clone();
        cal.add(Calendar.HOUR, numHours);
        Date dateFrom = null;
        Date dateTo = null;
        if(baseCal.getTime().after(cal.getTime())) {
            dateFrom = cal.getTime();
            dateTo = baseCal.getTime();
        } else {
            dateFrom = baseCal.getTime();
            dateTo = cal.getTime();
        }
        return new TimePeriod(dateFrom, dateTo);
    }

    public static TimePeriod getDayPeriod(Calendar selectedDate) {
        Calendar dayFrom = (Calendar) selectedDate.clone();
        dayFrom.set(Calendar.HOUR_OF_DAY, 0);
        dayFrom.set(Calendar.MINUTE, 0);
        dayFrom.set(Calendar.SECOND, 0);
        dayFrom.set(Calendar.MILLISECOND, 0);
        Calendar dayTo = (Calendar) dayFrom.clone();
        dayTo.add(Calendar.HOUR_OF_DAY, 24);
        return new TimePeriod(dayFrom.getTime(), dayTo.getTime());
    }

    public static TimePeriod getHourPeriod(Calendar selectedDate) {
        Calendar dayFrom = (Calendar) selectedDate.clone();
        dayFrom.set(Calendar.MINUTE, 0);
        dayFrom.set(Calendar.SECOND, 0);
        dayFrom.set(Calendar.MILLISECOND, 0);
        Calendar dayTo = (Calendar) dayFrom.clone();
        dayTo.add(Calendar.HOUR_OF_DAY, 1);
        return new TimePeriod(dayFrom.getTime(), dayTo.getTime());
    }

    public static TimePeriod getMinutePeriod(Calendar selectedDate) {
        Calendar dayFrom = (Calendar) selectedDate.clone();
        dayFrom.set(Calendar.SECOND, 0);
        dayFrom.set(Calendar.MILLISECOND, 0);
        Calendar dayTo = (Calendar) dayFrom.clone();
        dayTo.add(Calendar.SECOND, 60);
        return new TimePeriod(dayFrom.getTime(), dayTo.getTime());
    }

    public static TimePeriod getSecondPeriod(Calendar selectedDate) {
        Calendar dayFrom = (Calendar) selectedDate.clone();
        dayFrom.set(Calendar.MILLISECOND, 0);
        Calendar dayTo = (Calendar) dayFrom.clone();
        dayTo.add(Calendar.MILLISECOND, 1000);
        return new TimePeriod(dayFrom.getTime(), dayTo.getTime());
    }

    public static TimePeriod getLapsePeriod(Date selectedDate, TimePeriod.Lapse timePeriodLapse) throws ExceptionVS {
        Calendar selectedDateCalendar = Calendar.getInstance();
        selectedDateCalendar.setTime(selectedDate);
        switch(timePeriodLapse) {
            case YEAR: return getYearPeriod(selectedDateCalendar);
            case MONTH: return getMonthPeriod(selectedDateCalendar);
            case WEEK: return getWeekPeriod(selectedDateCalendar);
            case DAY: return getDayPeriod(selectedDateCalendar);
            case HOUR: return getHourPeriod(selectedDateCalendar);
            case MINUTE: return getMinutePeriod(selectedDateCalendar);
            case SECOND: return getSecondPeriod(selectedDateCalendar);
        }
        throw new ExceptionVS("Unsupported Lapse period: '" + timePeriodLapse + "'");
    }

    public static Calendar getDayFromPreviousWeek(Calendar requestDate) {
        Calendar result = ((Calendar)requestDate.clone());
        result.set(Calendar.DAY_OF_YEAR, (requestDate.get(Calendar.DAY_OF_YEAR) - 7));
        return result;
    }

    public static TimePeriod getURLTimePeriod(String dateFromStr, String dateToStr) throws ParseException {
        Date dateFrom = urlDateFormatter.parse(dateFromStr);
        Date dateTo = urlDateFormatter.parse(dateToStr);
        return new TimePeriod(dateFrom, dateTo);
    }

    public static Date getURLDate(String dateStr) throws ParseException {
        return urlDateFormatter.parse(dateStr);
    }

    public static class TimePeriod {

        public enum Lapse {YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND}

        private Date dateFrom;
        private Date dateTo;

        public TimePeriod(Date dateFrom, Date dateTo) {
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
        }

        public Date getDateFrom() {
            return dateFrom;
        }

        public Date getDateTo() {
            return dateTo;
        }

        public static TimePeriod parse(Map dataMap) throws ParseException {
            Date dateFrom = DateUtils.getDateFromString((String) dataMap.get("dateFrom"));
            Date dateTo = DateUtils.getDateFromString((String) dataMap.get("dateTo"));
            return new TimePeriod(dateFrom, dateTo);
        }

        public Map toMap() {
            Map dataMap = new HashMap<>();
            dataMap.put("dateFrom", dateFrom);
            dataMap.put("dateTo", dateTo);
            return dataMap;
        }

        public Map getMap() {
            return getMap(null);
        }

        public Map getMap(String dateFormat) {
            Map dataMap = new HashMap();
            dataMap.put("dateFrom", dateFormat == null ? dateFrom : DateUtils.getDateStr(dateFrom, dateFormat));
            dataMap.put("dateTo", dateFormat == null ? dateTo : DateUtils.getDateStr(dateTo, dateFormat));
            return dataMap;
        }

        public boolean isCurrentWeekPeriod() {
            TimePeriod currentWeekPeriod = getCurrentWeekPeriod();
            return (dateFrom.compareTo(currentWeekPeriod.getDateFrom()) >=0 &&
                    dateTo.compareTo(currentWeekPeriod.getDateTo()) <= 0);
        }

        public boolean inRange(Date dateToCheck) {
            return dateToCheck.compareTo(dateFrom) >= 0 && dateToCheck.compareTo(dateTo) <= 0;
        }

        @Override public String toString() {
            return "Period from [" + getDateStr(dateFrom) + " - " + getDateStr(dateTo) + "]";
        }
    }

    public static String getDayWeekDateStr (Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if(Calendar.getInstance().get(Calendar.YEAR) != calendar.get(Calendar.YEAR))
            return getDateStr(date, "dd MMM yyyy' 'HH:mm");
        else return getDateStr(date, "EEE dd MMM' 'HH:mm");
    }

    public static Date getDayWeekDate (String dateStr) throws ParseException {
        try {
            return getDateFromString (dateStr, "dd MMM yyyy' 'HH:mm");
        } catch (Exception ex) {
            Calendar resultCalendar = Calendar.getInstance();
            resultCalendar.setTime(getDateFromString (dateStr, "EEE dd MMM' 'HH:mm"));
            resultCalendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
            return resultCalendar.getTime();
        }
    }

}