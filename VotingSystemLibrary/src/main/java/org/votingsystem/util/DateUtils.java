package org.votingsystem.util;

import org.apache.log4j.Logger;

import java.io.File;
import java.text.*;
import java.util.*;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class DateUtils {

    private static Logger logger = Logger.getLogger(DateUtils.class);

    public static Date addDays(Date date, int days){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
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
    
    public static Date getDatePlus (int numDays) {
    	Calendar cal = new GregorianCalendar(); 
    	int todayDay = cal.get(Calendar.DAY_OF_YEAR);
    	cal.set(Calendar.DAY_OF_YEAR, todayDay + numDays);
        return cal.getTime();
    }

    /**
     * Método que devuelve una ruta del sistema de ficheros con formato
     * /aaaa/mm/dd en función de la fecha que se le pase como argumento.
     *
     * @param Date
     * @return String ruta del sitema de fichros
     */
    public static String getFilesystemPathFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        NumberFormat format = new DecimalFormat("00");
        return File.separator + format.format(calendar.get(Calendar.YEAR)) +
            File.separator + format.format((calendar.get(Calendar.MONTH) + 1)) +
            File.separator + format.format(calendar.get(Calendar.DAY_OF_MONTH)) + File.separator;
    }

    /**
     * Método que devuelve un Date a partir de un String con formato "yyyy/MM/dd'T'HH:mm:ss"
     *
     * @param dateString fecha en formato String
     * @return Date fecha en formato Date
     * @throws import java.text.ParseException;
     */
    public static Date getDateFromString (String dateString) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    	return formatter.parse(dateString);
    }
    
    public static Date getDateFromShortString (String yyyy_MM_dd) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
    	return formatter.parse(yyyy_MM_dd);
    }

    /**
     * Método que devuelve un String con formato "yyyy/MM/dd'T'HH:mm:ss a partir de un Date"
     *
     * @param Date fecha en formato Date
     * @return dateString fecha en formato String
     * @throws import java.text.ParseException;
     */
    public static String getStringFromDate (Date date) {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd' 'HH:mm:ss");
    	return formatter.format(date);
    }

    public static String getDate_Es (Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy");
        return formatter.format(date);
    }

    public static String getLongDate_Es (Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy HH:mm");
        return formatter.format(date);
    }

    public static Date getDateFromLongDateStr_Es (String dateStr) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy HH:mm");
        return formatter.parse(dateStr);
    }
    
    public static String getShortStringFromDate (Date date) {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
    	return formatter.format(date);
    }

    public static String getDirPath (Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("/yyyy/MMM/dd/");
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
    public static String getElapsedTimeHoursMinutesMillisFromMilliseconds(long milliseconds) {
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
        result.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        result.set(Calendar.HOUR_OF_DAY, 0);
        result.set(Calendar.MINUTE, 0);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);
        return result;
    }

    public static TimePeriod getCurrentWeekPeriod() {
        return getWeekPeriod(Calendar.getInstance());
    }

    public static DateUtils.TimePeriod getYearPeriod(Calendar selectedDate) {
        Calendar dayFrom = (Calendar) selectedDate.clone();
        dayFrom.set(Calendar.DAY_OF_YEAR, 1);
        dayFrom.set(Calendar.HOUR_OF_DAY, 0);
        dayFrom.set(Calendar.MINUTE, 0);
        dayFrom.set(Calendar.SECOND, 0);
        dayFrom.set(Calendar.MILLISECOND, 0);
        Calendar dayTo = (Calendar) dayFrom.clone();
        dayTo.add(Calendar.DAY_OF_YEAR, 365);
        return new DateUtils.TimePeriod(dayFrom.getTime(), dayTo.getTime());
    }

    public static DateUtils.TimePeriod getMonthPeriod(Calendar selectedDate) {
        Calendar dayFrom = (Calendar) selectedDate.clone();
        dayFrom.set(Calendar.DAY_OF_MONTH, 1);
        dayFrom.set(Calendar.HOUR_OF_DAY, 0);
        dayFrom.set(Calendar.MINUTE, 0);
        dayFrom.set(Calendar.SECOND, 0);
        dayFrom.set(Calendar.MILLISECOND, 0);
        Calendar dayTo = (Calendar) dayFrom.clone();
        dayTo.add(Calendar.DAY_OF_MONTH, dayFrom.getActualMaximum(Calendar.DAY_OF_MONTH));
        return new DateUtils.TimePeriod(dayFrom.getTime(), dayTo.getTime());
    }

    public static TimePeriod getWeekPeriod(Calendar selectedDate) {
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

    public static DateUtils.TimePeriod getDayPeriod(Calendar selectedDate) {
        Calendar dayFrom = (Calendar) selectedDate.clone();
        dayFrom.set(Calendar.HOUR_OF_DAY, 0);
        dayFrom.set(Calendar.MINUTE, 0);
        dayFrom.set(Calendar.SECOND, 0);
        dayFrom.set(Calendar.MILLISECOND, 0);
        Calendar dayTo = (Calendar) dayFrom.clone();
        dayTo.add(Calendar.HOUR_OF_DAY, 24);
        return new DateUtils.TimePeriod(dayFrom.getTime(), dayTo.getTime());
    }

    public static DateUtils.TimePeriod getHourPeriod(Calendar selectedDate) {
        Calendar dayFrom = (Calendar) selectedDate.clone();
        dayFrom.set(Calendar.MINUTE, 0);
        dayFrom.set(Calendar.SECOND, 0);
        dayFrom.set(Calendar.MILLISECOND, 0);
        Calendar dayTo = (Calendar) dayFrom.clone();
        dayTo.add(Calendar.HOUR_OF_DAY, 1);
        return new DateUtils.TimePeriod(dayFrom.getTime(), dayTo.getTime());
    }

    public static DateUtils.TimePeriod getMinutePeriod(Calendar selectedDate) {
        Calendar dayFrom = (Calendar) selectedDate.clone();
        dayFrom.set(Calendar.SECOND, 0);
        dayFrom.set(Calendar.MILLISECOND, 0);
        Calendar dayTo = (Calendar) dayFrom.clone();
        dayTo.add(Calendar.SECOND, 60);
        return new DateUtils.TimePeriod(dayFrom.getTime(), dayTo.getTime());
    }

    public static DateUtils.TimePeriod getSecondPeriod(Calendar selectedDate) {
        Calendar dayFrom = (Calendar) selectedDate.clone();
        dayFrom.set(Calendar.MILLISECOND, 0);
        Calendar dayTo = (Calendar) dayFrom.clone();
        dayTo.add(Calendar.MILLISECOND, 1000);
        return new DateUtils.TimePeriod(dayFrom.getTime(), dayTo.getTime());
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

        @Override public String toString() {
            return "Period from [" + getStringFromDate(dateFrom) + " - " + getStringFromDate(dateTo) + "]";
        }
    }
}