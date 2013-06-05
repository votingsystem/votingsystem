package org.sistemavotacion.util;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class DateUtils {

	private static Logger logger = LoggerFactory.getLogger(DateUtils.class);
	
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

    public static Date getTodayDate () {
        return new Date(System.currentTimeMillis());
    }
    
    public static Date getYesterdayDate () {
    	Calendar cal = new GregorianCalendar(); 
    	int todayDay = cal.get(Calendar.DAY_OF_MONTH);
    	cal.set(Calendar.DAY_OF_MONTH, todayDay-1);
        return cal.getTime();
    }
    
    public static Date getTodayRoundedDate () {
	    Calendar calendar = Calendar.getInstance(); // today
	    Calendar gregorianCalendar = new GregorianCalendar(); 
	    calendar.clear();
	    calendar.set(gregorianCalendar.get(Calendar.YEAR),
	    		gregorianCalendar.get(Calendar.MONTH),
	    		gregorianCalendar.get(Calendar.DAY_OF_MONTH));
        return calendar.getTime();
    }

    public static Date getNextDayRoundedDate (int nextDayFromToday) {
	    Calendar calendar = Calendar.getInstance(); // today
	    Calendar gregorianCalendar = new GregorianCalendar(); 
	    calendar.clear();
	    calendar.set(gregorianCalendar.get(Calendar.YEAR),
	    		gregorianCalendar.get(Calendar.MONTH),
	    		gregorianCalendar.get(Calendar.DAY_OF_MONTH) + nextDayFromToday);
        return calendar.getTime();
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
            File.separator + format.format(calendar.get(Calendar.DAY_OF_MONTH)) +
            File.separator;
    }

    /**
     * Método que devuelve un Date a partir de un String con formato "yyyy-MM-dd'T'HH:mm:ss"
     *
     * @param dateString fecha en formato String
     * @return Date fecha en formato Date
     * @throws import java.text.ParseException;
     */
    public static Date getDateFromString (String dateString) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	return formatter.parse(dateString);
    }
    
    public static Date getDateFromShortString (String dateString) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    	return formatter.parse(dateString);
    }

    /**
     * Método que devuelve un String con formato "yyyy-MM-dd'T'HH:mm:ss a partir de un Date"
     *
     * @param Date fecha en formato Date
     * @return dateString fecha en formato String
     * @throws import java.text.ParseException;
     */
    public static String getStringFromDate (Date date) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
    	return formatter.format(date);
    }
        
    public static String getSpanishFormattedStringFromDate (Date date) {
        DateFormat formatter = new SimpleDateFormat("dd MMM yyyy' 'HH:mm:ss");
    	return formatter.format(date);
    }
    
    public static String getDirStringFromDate (Date date) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    	return formatter.format(date);
    }
    
    public static String getShortStringFromDate (Date date) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    	return formatter.format(date);
    }

    /**
     * Método que devuelve un String con formato HH:mm:ss:SSS a partir de un Date"
     *
     * @param Date fecha en formato Date
     * @return dateString fecha en formato String
     * @throws import java.text.ParseException;
     */
    public static String getTimeFromDate (Date date) {
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
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

    public static final String getFechaYYYYMMDD () {
        Date date = new Date();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    public static Calendar xDiasAntes(int numDias) {
        Calendar today = Calendar.getInstance();
        today.add(Calendar.DATE, - numDias);
        return (today);
    }

}