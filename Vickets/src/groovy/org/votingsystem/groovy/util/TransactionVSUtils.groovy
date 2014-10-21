package org.votingsystem.groovy.util

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class TransactionVSUtils {

    public static Map setBigDecimalToPlainString(Map mapToTransform) {
        for(String currency: mapToTransform.keySet()) {
            for(String tag: mapToTransform[currency].keySet()) {
                if(mapToTransform[currency][tag] instanceof BigDecimal) mapToTransform[currency][tag] =
                        ((BigDecimal)mapToTransform[currency][tag]).toPlainString()
                else {
                    mapToTransform[currency][tag].total =  ((BigDecimal)mapToTransform[currency][tag].total).toPlainString()
                    mapToTransform[currency][tag].timeLimited =  ((BigDecimal)mapToTransform[currency][tag].timeLimited).toPlainString()
                }
            }
        }
        return mapToTransform
    }

}
