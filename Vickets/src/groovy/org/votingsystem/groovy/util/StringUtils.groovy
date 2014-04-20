package org.votingsystem.groovy.util

class StringUtils {

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

}
