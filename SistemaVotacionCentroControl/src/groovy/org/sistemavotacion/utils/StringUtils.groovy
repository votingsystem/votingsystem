package org.sistemavotacion.utils

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
* */
class StringUtils {

    static final checkIds = { value ->
        if (value == null) return null; 
        def result = []
        value.split(",").collect { sub ->
            if (sub.isNumber()) result << sub
        }
        return result
    }
        
    static final checkURL = { url ->
        if (!url) return null
        while (url.endsWith("/")){
                url = url.substring(0, url.length()-1)
        }
        return url
    }
}
