package org.votingsystem.groovy.util

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class StringUtils {

    static final checkIds = { value ->
        if (value == null) return null; 
        def result = []
        value.split(",").each { sub ->
            if (sub.isNumber()) result << sub
        }
        return result
    }

}
