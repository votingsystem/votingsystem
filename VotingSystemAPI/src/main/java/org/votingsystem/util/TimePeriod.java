package org.votingsystem.util;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;


public class TimePeriod {

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

    public boolean isCurrentWeekPeriod() {
        TimePeriod currentWeekPeriod = DateUtils.getCurrentWeekPeriod();
        return (dateFrom.compareTo(currentWeekPeriod.getDateFrom()) >=0 &&
                dateTo.compareTo(currentWeekPeriod.getDateTo()) <= 0);
    }

    public boolean inRange(Date dateToCheck) {
        return dateToCheck.compareTo(dateFrom) >= 0 && dateToCheck.compareTo(dateTo) <= 0;
    }

    @Override public String toString() {
        return "Period from [" + DateUtils.getDateStr(dateFrom) + " - " + DateUtils.getDateStr(dateTo) + "]";
    }
}
