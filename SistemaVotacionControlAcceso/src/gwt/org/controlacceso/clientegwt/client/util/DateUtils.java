package org.controlacceso.clientegwt.client.util;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
import java.util.Date;
import com.google.gwt.i18n.client.DateTimeFormat;

public class DateUtils {

    /**
     * Método que devuelve un Date a partir de un String con formato "yyyy-MM-dd HH:mm:ss"
     *
     * @param dateString fecha en formato String
     * @return Date fecha en formato Date
     * @throws import java.text.ParseException;
     */
    public static Date getDateFromString (String dateString) {
    	DateTimeFormat formatter = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss");
    	return formatter.parse(dateString);
    }
    
    /**
     * Método que devuelve un Date a partir de un String con formato "dd-MM-yyyy"
     *
     * @param dateString fecha en formato String
     * @return Date fecha en formato Date
     * @throws import java.text.ParseException;
     */
    public static Date getDateFromSimpleString (String dateString) {
    	DateTimeFormat formatter = DateTimeFormat.getFormat("yyyy-MM-dd");
    	return formatter.parse(dateString);
    }    

    public static Date getTodayDate () {
        return new Date(System.currentTimeMillis());
    }
    
    /**
     * Método que devuelve un String con formato "yyyy-MM-dd HH:mm:ss a partir de un Date"
     *
     * @param Date fecha en formato Date
     * @return dateString fecha en formato String
     * @throws import java.text.ParseException;
     */
    public static String getStringFromDate (Date date) {
    	DateTimeFormat formatter = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss");
    	return formatter.format(date);
    }
    
    public static String getSpanishStringFromDate (Date date) {
    	DateTimeFormat formatter = DateTimeFormat.getFormat("dd MMM 'de' yyyy 'a las' HH:mm");
    	return formatter.format(date);
    }
    
    /**
     * Método que devuelve un String con formato "dd-MM-yyyy"
     *
     * @param Date fecha en formato Date
     * @return dateString fecha en formato String
     * @throws import java.text.ParseException;
     */
    public static String getSimpleStringFromDate (Date date) {
    	DateTimeFormat formatter = DateTimeFormat.getFormat("yyyy-MM-dd");
    	return formatter.format(date);
    }    
    
    public static String getElpasedTimeHours(Date start, Date end) {
    	Float hours = (end.getTime() - start.getTime())/(60*60*1000F);
    	return new Integer(hours.intValue()).toString();
    }
    
    public static String getElpasedTimeHoursFromNow(Date end) {
    	Float hours = (end.getTime() -DateUtils.getTodayDate().getTime())/(60*60*1000F);
    	return new Integer(hours.intValue()).toString();
    }
}