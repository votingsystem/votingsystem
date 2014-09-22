package org.votingsystem.groovy.util

class RequestUtils {

    // 1=ASCENDING, -1=DESCENDING
    //result GORM needs 'asc' for ascending or 'desc' for descending
    public static Map getSortParamsMap(Map params) {
        Map sortMap = [:]
        params.keySet().each {
            if(it.contains("sorts"))  {
                it.replaceAll(/\[(.*?)\]/) { fullMatch, sortParam ->
                    sortMap[sortParam] = (Integer.valueOf(params[it]) > 0) ? "asc":"desc"
                }
            }
        }
        return sortMap;
    }

    public static Calendar getCalendar(Map params) {
        Calendar calendar = Calendar.getInstance()
        if(params.year && params.month && params.day) {
            calendar.set(Calendar.YEAR, params.int('year'))
            calendar.set(Calendar.MONTH, params.int('month') - 1) //Zero based
            calendar.set(Calendar.DAY_OF_MONTH, params.int('day'))
        }
        calendar.getTime()
        return calendar
    }

}
