package org.votingsystem.controlcenter.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.AccessControlVS
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.util.ApplicationContextHolder

/**
 * @infoController Información de la aplicación
 * @descController Servicios que ofrecen datos sobre la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class ServerInfoController {

    def signatureVSService

    /**
     * @httpMethod [GET]
     * @serviceURL [/serverInfo]
     * @responseContentType [application/json]
     * @return Documento JSON con datos de la aplicación
     */
    def index() {
        HashMap serverInfo = new HashMap()
        serverInfo.name = grailsApplication.config.vs.serverName
        serverInfo.serverType = ActorVS.Type.CONTROL_CENTER.toString()
        serverInfo.serverURL = "${grailsApplication.config.grails.serverURL}"
        serverInfo.urlTimeStampServer="${grailsApplication.config.vs.urlTimeStampServer}"
        serverInfo.urlBlog = grailsApplication.config.vs.blogURL
        serverInfo.state = ActorVS.State.OK.toString()
        serverInfo.certChainURL = "${createLink(controller: 'certificateVS', action:'certChain', absolute:true)}"
        serverInfo.certChainPEM = signatureVSService.getServerCertChain().text
        def accessControl = AccessControlVS.getAll()
        serverInfo.accessControl = []
        accessControl?.each {
            def accessControlMap = [name:it.name, serverURL:it.serverURL, state:it.state.toString()]
            serverInfo.accessControl.add(accessControlMap)
        }
        serverInfo.environmentMode =  grails.util.Environment.current.toString()
        response.setHeader('Access-Control-Allow-Origin', "*")

        if (params.callback) render "${params.callback}(${serverInfo as JSON})"
        else render serverInfo as JSON
    }

    /**
     * @httpMethod [GET]
     * @return La lista de servicios de la aplicación
     */
    def serviceList () { }

    /**
     * @httpMethod [GET]
     * @return Datos de las versiones de algunos componentes de la aplicación
     */
    def appData () { }

    /**
     * @httpMethod [GET]
     * @return Información general de la aplicación
     */
    def info () { }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
