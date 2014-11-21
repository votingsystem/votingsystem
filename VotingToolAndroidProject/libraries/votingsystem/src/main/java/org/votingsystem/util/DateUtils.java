package org.votingsystem.util;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.lib.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
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

    public static Date getDateFromString (String dateString) throws ParseException {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd' 'HH:mm:ss");
        return formatter.parse(dateString);
    }

    public static Date getDateFromString (String dateString, String format) throws ParseException {
        DateFormat formatter = new SimpleDateFormat(format);
        return formatter.parse(dateString);
    }

    public static String getDateStr (Date date) {
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return formatter.format(date);
    }

    public static String getDateStr (Date date, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static String getElapsedTimeHoursMinutesFromMilliseconds(long milliseconds) {
        String format = String.format("%%0%dd", 2);
        long elapsedTime = milliseconds / 1000;
        String minutes = String.format(format, (elapsedTime % 3600) / 60);
        String hours = String.format(format, elapsedTime / 3600);
        String time =  hours + ":" + minutes;
        return time;
    }

    public static String getElapsedTimeHoursMinutesSecondsFromMilliseconds(long milliseconds) {
        String format = String.format("%%0%dd", 2);
        long elapsedTime = milliseconds / 1000;
        String seconds = String.format(format, elapsedTime % 60);
        String minutes = String.format(format, (elapsedTime % 3600) / 60);
        String hours = String.format(format, elapsedTime / 3600);
        String time =  hours + ":" + minutes + ":" + seconds;
        return time;
    }

    public static Calendar addDays(int numDias) {
        Calendar today = Calendar.getInstance();
        today.add(Calendar.DATE, numDias);
        return today;
    }

    public static Calendar getEventVSElectionDateBeginCalendar(
            int year, int monthOfYear,int dayOfMonth) {
        Calendar electionCalendar = Calendar.getInstance();
        electionCalendar.set(Calendar.YEAR, year);
        electionCalendar.set(Calendar.MONTH, monthOfYear);
        electionCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        electionCalendar.set(Calendar.HOUR_OF_DAY, 0);
        electionCalendar.set(Calendar.MINUTE, 0);
        electionCalendar.set(Calendar.SECOND, 0);
        electionCalendar.set(Calendar.MILLISECOND, 0);
        return electionCalendar;
    }

    public static Date addDays(Date date, int days){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
    }
    
    public static String getDayHourElapsedTime (Date date1, Date date2, Context context) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return getDayHourElapsedTime(cal1, cal2, context);
    }

    public static String getDayWeekDateStr (Date date) {
        Date lastYear = addDays(Calendar.getInstance().getTime(), -364);
        Date nextYear = addDays(Calendar.getInstance().getTime(), 364);
        if(date.before(lastYear) || date.after(nextYear)) return getDateStr(date, "dd MMM yyyy' 'HH:mm");
        else return getDateStr(date, "EEE dd MMM' 'HH:mm");
    }

    public static Date getDayWeekDate (String dateStr) throws ParseException {
        Date result = null;
        try {
            result = getDateFromString (dateStr, "dd MMM yyyy' 'HH:mm");
            return result;
        } catch (Exception ex) {
            result = getDateFromString (dateStr, "EEE dd MMM' 'HH:mm");
            return result;
        }
    }

    public static String getPath (Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("/yyyy/MM/dd/");
        return formatter.format(date);
    }

    public static Date getDateFromPath (String dateStr) {
        SimpleDateFormat formatter = new SimpleDateFormat("/yyyy/MM/dd/");
        try {
            return formatter.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();e.printStackTrace();
        }
        return null;
    }

    public static String getElapsedTimeStr(Date end) {
    	Float hours = (end.getTime() - Calendar.getInstance().getTime().getTime())/(60*60*1000F);
    	return Integer.valueOf(hours.intValue()).toString();
    }
    
    public static String getYearDayHourMinuteSecondElapsedTime (Calendar cal1, Calendar cal2,
            Context context) {
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

        StringBuilder result = new StringBuilder();
        if (elapsedYears > 0) result.append(elapsedYears + ", "
                + context.getString(R.string.years_lbl));
        if (elapsedDays > 0) result.append(elapsedDays + ", "
                + context.getString(R.string.days_lbl));
        if (elapsedHours > 0) result.append(elapsedHours + ", "
                + context.getString(R.string.hours_lbl));
        if (elapsedMinutes > 0) result.append(elapsedMinutes + ", "
                + context.getString(R.string.minutes_lbl));
        if (elapsedSeconds > 0) result.append(elapsedSeconds + ", "
                + context.getString(R.string.seconds_lbl));
        return result.toString();
    }

    public static String getYearDayHourMinuteSecondElapsedTime (Date date1, Date date2,
        Context context) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return getYearDayHourMinuteSecondElapsedTime(cal1, cal2, context);
    }
    
    public static String getDayHourElapsedTime (Calendar cal1, Calendar cal2, Context context) {
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

        StringBuilder result = new StringBuilder();
        if (elapsedDays > 0) result.append(elapsedDays + " " + context.getString(R.string.days_lbl));
        if (elapsedHours > 0) result.append(elapsedHours + ", " + context.getString(R.string.hours_lbl));
        return result.toString();
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

    public static TimePeriod getCurrentWeekPeriod() {
        return getWeekPeriod(Calendar.getInstance());
    }

    public static TimePeriod getWeekPeriod(Calendar selectedDate) {
        Calendar weekFromCalendar = getMonday(selectedDate);
        Calendar weekToCalendar = (Calendar) weekFromCalendar.clone();
        weekToCalendar.add(Calendar.DAY_OF_YEAR, 7);
        return new TimePeriod(weekFromCalendar.getTime(), weekToCalendar.getTime());
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

        public static TimePeriod parse(JSONObject jsonData) throws ParseException, JSONException {
            Date dateFrom = DateUtils.getDateFromString(jsonData.getString("dateFrom"), "dd MMM yyyy' 'HH:mm");
            Date dateTo = DateUtils.getDateFromString(jsonData.getString("dateTo"), "dd MMM yyyy' 'HH:mm");
            return new TimePeriod(dateFrom, dateTo);
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject jsonData = new JSONObject();
            jsonData.put("dateFrom", DateUtils.getDateStr(dateFrom, "dd MMM yyyy' 'HH:mm"));
            jsonData.put("dateTo", DateUtils.getDateStr(dateTo, "dd MMM yyyy' 'HH:mm"));
            return jsonData;
        }

        @Override public String toString() {
            return "Period from [" + getDateStr(dateFrom) + " - " + getDateStr(dateTo) + "]";
        }

    }
}