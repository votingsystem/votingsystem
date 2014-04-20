package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.apache.lucene.search.Sort
import org.apache.lucene.search.SortField
import org.hibernate.search.FullTextQuery
import org.hibernate.search.FullTextSession
import org.hibernate.search.Search
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.vicket.TransactionVS
import org.votingsystem.search.SearchHelper
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.DateUtils
/**
 * @infoController Búsquedas
 * @descController Servicios de búsqueda sobre los datos generados por la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class SearchController {

    SearchHelper searchHelper;
    def sessionFactory
    def grailsApplication
    def transactionVSService

    /**
     * ==================================================\n
     * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE PRUEBAS).
     * ==================================================\n
     * Servicio que reindexa el motor de búsqueda
     * @httpMethod [GET]
     */
    def reindexTest () {
        if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "serviceDevelopmentModeMsg"))]
        }
        log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
        FullTextSession fullTextSession = Search.getFullTextSession(sessionFactory.currentSession);
        fullTextSession.createIndexer().startAndWait()
        return [responseVS : new ResponseVS(ResponseVS.SC_OK, "OK")]
    }


    def transactionVS() {
        def resultMap = [:]
        if (params.searchParam) {
            ResponseVS<List<TransactionVS>> responseVS = searchHelper.findResponseVSByFullText(TransactionVS.class,
                    ['type', 'amount', 'currency', 'dateCreated', 'subject']  as String[], params.searchParam,
                    params.int('offset'), params.int('max'));
            List<TransactionVS> transactionList = responseVS.getData()
            log.debug("responseVS.size: ${responseVS.size} - transactionList.size(): ${transactionList.size()}")
            def resultList = []
            transactionList.each {transactionItem ->
                resultList.add(transactionVSService.getTransactionMap(transactionItem))
            }
            resultMap = ["${message(code: 'transactionRecordsLbl')}":resultList, queryRecordCount: responseVS.size,
                            numTotalTransactions:responseVS.size ]
        }
        render resultMap as JSON
    }
}
