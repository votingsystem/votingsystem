package org.votingsystem.android.xml;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    private static final DateFormat xmlDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ");

    public static Date getXmlDate(String dateString) {
        try {
            return xmlDateFormat.parse(dateString);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String getXmlDateStr(Date date) {
        try {
            return xmlDateFormat.format(date).replace("GMT", "");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
