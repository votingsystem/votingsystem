package org.votingsystem.util;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class DateUtils {


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
     * Método que devuelve un Date a partir de un String con formato "yyyy/MM/dd'T'HH:mm:ss"
     *
     * @param dateString fecha en formato String
     * @return Date fecha en formato Date
     * @throws import java.text.ParseException;
     */
    public static Date getDateFromString (String dateString) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd' 'HH:mm:ss");
    	return formatter.parse(dateString);
    }

    /**
     * Método que devuelve un String con formato "yyyy/MM/dd HH:mm:ss a partir de un Date"
     *
     * @param Date fecha en formato Date
     * @return dateString fecha en formato String
     * @throws import java.text.ParseException;
     */
    public static String getStringFromDate (Date date) {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    	return formatter.format(date);
    }

    /**
     * Método que devuelve un String con formato HH:mm:ss:SSS a partir de un Date"
     *
     * @param Date fecha en formato Date
     * @return dateString fecha en formato String
     * @throws import java.text.ParseException;
     */
    public static String getTimeFromDate (Date date) throws ParseException {
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

    public static final String getFechaYYYYMMDD () {
        Date date = new Date();
        String resultado = null;
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        resultado = formatter.format(date);
        return resultado;
    }

    public static Calendar xDiasAntes(int numDias) {
        Calendar today = Calendar.getInstance();
        today.add(Calendar.DATE, - numDias);
        return (today);
    }
    
    public static String getYearDayHourMinuteSecondElapsedTime (Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return getYearDayHourMinuteSecondElapsedTime(cal1, cal2);
    }
    
    public static String getDayHourElapsedTime (Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return getDayHourElapsedTime(cal1, cal2);
    }
    
    public static String getSpanishStringFromDate (Date date) {
    	SimpleDateFormat formatter = new SimpleDateFormat("dd MMM 'de' yyyy 'a las' HH:mm"); 
    	return formatter.format(date);
    }
    
    public static String getShortSpanishStringFromDate (Date date) {
    	SimpleDateFormat formatter = new SimpleDateFormat("HH:mm 'del' dd/MMM/yyyy"); 
    	return formatter.format(date);
    }
    
    public static String getElpasedTimeHoursFromNow(Date end) {
    	Float hours = (end.getTime() -DateUtils.getTodayDate().getTime())/(60*60*1000F);
    	return Integer.valueOf(hours.intValue()).toString();
    }
    
    public static String getYearDayHourMinuteSecondElapsedTime (Calendar cal1, Calendar cal2) {
            
        long l1 = cal1.getTimeInMillis();
        long l2 = cal2.getTimeInMillis();
        long diff = l2 - l1;

        long secondInMillis = 1000;
        long minuteInMillis = secondInMillis * 60;
        long hourInMillis = minuteInMillis * 60;
        long dayInMillis = hourInMillis * 24;
        long yearInMillis = dayInMillis * 365;

        long elapsedYears = diff / yearInMillis;
        diff = diff % yearInMillis;
        long elapsedDays = diff / dayInMillis;
        diff = diff % dayInMillis;
        long elapsedHours = diff / hourInMillis;
        diff = diff % hourInMillis;
        long elapsedMinutes = diff / minuteInMillis;
        diff = diff % minuteInMillis;
        long elapsedSeconds = diff / secondInMillis;

        StringBuilder duracion = new StringBuilder();
        if (elapsedYears > 0) duracion.append(elapsedYears + ", años");
        if (elapsedDays > 0) duracion.append(elapsedDays + ", días");
        if (elapsedHours > 0) duracion.append(elapsedHours + ", horas");
        if (elapsedMinutes > 0) duracion.append(elapsedMinutes + ", minutos");
        if (elapsedSeconds > 0) duracion.append(elapsedSeconds + ", segundos");
        return duracion.toString();
    }
    
    public static String getDayHourElapsedTime (Calendar cal1, Calendar cal2) {
            
        long l1 = cal1.getTimeInMillis();
        long l2 = cal2.getTimeInMillis();
        long diff = l2 - l1;

        long secondInMillis = 1000;
        long minuteInMillis = secondInMillis * 60;
        long hourInMillis = minuteInMillis * 60;
        long dayInMillis = hourInMillis * 24;
        long yearInMillis = dayInMillis * 365;

        long elapsedDays = diff / dayInMillis;
        diff = diff % dayInMillis;
        long elapsedHours = diff / hourInMillis;
        diff = diff % hourInMillis;

        StringBuilder duracion = new StringBuilder();
        if (elapsedDays > 0) duracion.append(elapsedDays + " días");
        if (elapsedHours > 0) duracion.append(elapsedHours + ", horas");
        return duracion.toString();
    }

    public static String getSpanishFormattedStringFromDate (Date date) {
        DateFormat formatter = new SimpleDateFormat("dd MMM yyyy' 'HH:mm:ss");
        return formatter.format(date);
    }

}