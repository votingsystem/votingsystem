package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.VoteVS

import javax.xml.bind.annotation.adapters.HexBinaryAdapter

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class VoteVSController {

	def voteVSService
	def grailsApplication
        
    def index() { }
	
	/**
	 * Service that receives and checks the votes signed by the Control Center
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/voteVS]
	 * @requestContentType [application/pkcs7-signature] Required. The vote signed by the user anonymous certificate
     *                      and the Control Center
	 * @responseContentType [application/pkcs7-signature]
	 * @return  The vote signed by the user anonymous certificate, the Control Center and the Access Control
	 */
    def save() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
		else return [responseVS:voteVSService.validateVote(messageSMIME)]
	}
	
	/**
	 * Service that returns the data of the vote associated with the in passed as parameter
	 * @httpMethod [GET]
	 * @serviceURL [/voteVS/${id}]
	 * @param [id] Required. The VoteVS identifier in the database
	 * @responseContentType [application/json]
	 * @return the vote
	 */
	def get() {
		VoteVS voteVS
		Map  voteVSMap
		VoteVS.withTransaction {
			voteVS = VoteVS.get(params.long('id'))
			if(voteVS) voteVSMap = voteVSService.getVoteVSMap(voteVS)
		}
		if(!voteVS) return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'voteNotFound', args:[params.id]))]
		else render voteVSMap as JSON
	}


    /**
     * Service that returns the state and the value of the vote associated with the hash received as parameter
     * @httpMethod [GET]
     * @serviceURL [/voteVS/hash/${hashHex}]
     * @param [hashHex] Required. The VoteVS associated hash in hexadecimal
     * @responseContentType [application/json]
     * @return the state {OK, CANCELLED, ERROR} and the value of the vote
     */
    def getByHash() {
        VoteVS voteVS
        Map  voteVSMap
        if(params.hashHex) {
            VoteVS.withTransaction {
                voteVS = VoteVS.where {
                    certificateVS.hashCertVSBase64 == new String(new HexBinaryAdapter().unmarshal(params.hashHex))
                }.find()
                if(voteVS) voteVSMap = [state:voteVS.state.toString(), value:voteVS.optionSelected.content]
            }
        }
        if(!voteVS) return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'voteNotFound', args:[params.hashHex]))]
        else render voteVSMap as JSON
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
