package org.votingsystem.util;

import org.apache.log4j.Logger;

import java.io.File;
import java.text.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.MONDAY;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
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
    
    public static Date getYesterdayDate () {
    	Calendar cal = new GregorianCalendar(); 
    	int todayDay = cal.get(Calendar.DAY_OF_MONTH);
    	cal.set(Calendar.DAY_OF_MONTH, todayDay-1);
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

    public static String getDirStringFromDate (Date date) {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd_HH-mm-ss");
    	return formatter.format(date);
    }
    
    public static String getShortStringFromDate (Date date) {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
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

    public static Calendar getNextMonday(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 7);
        } else calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar;
    }

}