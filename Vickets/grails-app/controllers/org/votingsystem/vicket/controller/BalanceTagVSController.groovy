package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.vicket.model.BalanceTagVS
import org.votingsystem.vicket.model.UserVSAccount

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class BalanceTagVSController {

    def index() {
        if("POST".equals(request.method)) {
            def requestJSON = request.JSON
            if(!requestJSON.tag) {
                return [responseVS : new ResponseVS(ResponseVS.SC_ERROR, message(code: 'missingParamErrorMsg', args:['tag']))]
            } else  {
                BalanceTagVS tag
                BalanceTagVS.withTransaction { tag = BalanceTagVS.findWhere(name:requestJSON.tag.toLowerCase()) }
                if(!tag) tag = new BalanceTagVS(name:requestJSON.tag.toLowerCase()).save()
                def result = [id:tag.id, name:tag.name]
                render  result as JSON
            }
        } else {
            def listDB
            BalanceTagVS.withTransaction {
                listDB = BalanceTagVS.createCriteria().list(offset: 0) {
                    if(params.tag) { ilike('name', "%${params.tag}%") }
                }
            }
            List resultList = []
            listDB.each { it -> resultList.add(id:it.id, name:it.name) }
            Map result = [tagRecords:resultList, numTotalTags:listDB.totalCount]
            render result as JSON
        }
    }

    /**
     * Servicio que busca los balances asociados a una etiqueta
     * @param tag Obligatorio. Texto de la etiqueta.
     * @param max Opcional (por defecto 20). Número máximo de documentos que devuelve la consulta (tamaño de la página).
     * @param offset Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
     * @httpMethod [GET]
     * @responseContentType [application/json]
     */
    def userVSAccountByBalanceTagVS () {
        if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "serviceDevelopmentModeMsg"))]
        }
        log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
        if (!params.tag) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR, message(code: 'searchMissingParamTag'))]
        } else {
            def listDB
            UserVSAccount.withTransaction {
                listDB = UserVSAccount.createCriteria().listDistinct() {
                    tagVSSet {
                        eq('name', params.tag)
                    }
                }
            }
            def resultList = []
            listDB.each {account ->
                resultList.add([id: account.id, userVS:account.userVS.nif, amount:account.amount])
            }
            def resultMap = [accounts:resultList]
            render resultMap as JSON
        }
    }

}