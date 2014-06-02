package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.vicket.util.ApplicationContextHolder

import java.security.cert.X509Certificate

/**
 * @infoController Información de la aplicación
 * @descController Servicios que ofrecen datos sobre la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class ServerInfoController {

	def timeStampService
    def signatureVSService
    
	/**
	 * @httpMethod [GET]
	 * @responseContentType [application/json]
	 * @return documento JSON con datos de la aplicación
	 */
	def index() {
        HashMap serverInfo = new HashMap()
        X509Certificate serverCert = signatureVSService.getServerCert()
        byte[] serverCertPEMBytes = CertUtil.getPEMEncoded (serverCert)
        serverInfo.certChainPEM = new String(serverCertPEMBytes)
        serverInfo.name = grailsApplication.config.VotingSystem.serverName
        serverInfo.serverType = ActorVS.Type.VICKETS.toString()
        serverInfo.serverURL = "${grailsApplication.config.grails.serverURL}"
		serverInfo.state = ActorVS.State.RUNNING.toString()
        serverInfo.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        byte [] signingCertPEMBytes = timeStampService.getSigningCertPEMBytes()
        if(signingCertPEMBytes) serverInfo.timeStampCertPEM = new String(signingCertPEMBytes)
        serverInfo.environmentMode = ApplicationContextHolder.getEnvironment().toString()
		response.setHeader('Access-Control-Allow-Origin', "*")
		if (params.callback) render "${params.callback}(${serverInfo as JSON})"
        else render serverInfo as JSON
	}

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }

}
