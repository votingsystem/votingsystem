package org.votingsystem.cooin.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.springframework.dao.DataAccessException
import org.votingsystem.cooin.model.CooinAccount
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TagVS

import java.text.Normalizer

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class TagVSController {

    def systemService

    def index() {
        if("POST".equals(request.method)|| "OPTIONS".equals(request.method)) {
            def requestJSON = request.JSON
            if(!requestJSON.tag) {
                return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                        message(code: 'missingParamErrorMsg', args:['tag']))]
            } else  {
                TagVS tag
                TagVS.withTransaction { tag = TagVS.findWhere(name:requestJSON.tag) }
                if(!tag) {
                    String tagName = requestJSON.tag
                    tagName = Normalizer.normalize(requestJSON.tag, Normalizer.Form.NFD).replaceAll(
                            "\\p{InCombiningDiacriticalMarks}+", "");
                    tag = new TagVS(name:tagName).save()
                    new CooinAccount(currencyCode: Currency.getInstance('EUR').getCurrencyCode(), balance:BigDecimal.ZERO,
                            userVS:systemService.getSystemUser(), IBAN:systemService.getSystemUser().getIBAN(), tag:tag).save()
                }
                def result = [id:tag.id, name:tag.name]
                response.setHeader('Access-Control-Allow-Origin', "*")
                if (params.callback) render "${params.callback}(${result as JSON})"
                else render result as JSON
            }
        } else {
            if(!params.tag || params.tag.isEmpty()) {
                return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                        message(code: 'missingParamErrorMsg', args:['tag']))]
            }
            def listDB
            TagVS.withTransaction {
                listDB = TagVS.createCriteria().list(offset: 0) {
                    if(params.tag) { ilike('name', "%${params.tag}%") }
                }
            }
            List resultList = []
            listDB.each { it -> resultList.add(id:it.id, name:it.name) }
            Map result = [tagRecords:resultList, numTotalTags:listDB.totalCount]
            render result as JSON
        }
    }

    def list () {
        def listDB
        TagVS.withTransaction {
            listDB = TagVS.findAll()
        }
        List resultList = []
        listDB.each { it -> resultList.add(id:it.id, name:it.name) }
        Map result = [numTotalTags:listDB.size(), tagRecords:resultList]
        render result as JSON
    }

    def test () {
        def listDB
        TagVS.withTransaction {
            listDB = TagVS.createCriteria().list(offset: 0) { }
        }
        listDB.each { it ->
            new CooinAccount(currencyCode: Currency.getInstance('EUR').getCurrencyCode(), balance:BigDecimal.ZERO,
                    userVS:systemService.getSystemUser(), IBAN:systemService.getSystemUser().getIBAN(), tag:it).save()
        }
        render "OK"
    }

    /**
     * Servicio que busca los balances asociados a una etiqueta
     * @param tag Obligatorio. Texto de la etiqueta.
     * @param max Opcional (por defecto 20). Número máximo de documentos que devuelve la consulta (tamaño de la página).
     * @param offset Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
     * @httpMethod [GET]
     * @responseContentType [application/json]
     */
    def cooinAccountByBalanceTagVS () {
        if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: "serviceDevelopmentModeMsg"))]
        }
        log.debug "===============****¡¡¡¡¡ DEVELOPMENT Environment !!!!!****=================== "
        if (!params.tag) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR, message(code: 'searchMissingParamTag'))]
        } else {
            def listDB
            CooinAccount.withTransaction {
                listDB = CooinAccount.createCriteria().listDistinct() {
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

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

    def daoExceptionHandler(final DataAccessException exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}