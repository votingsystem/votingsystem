package org.votingsystem.accesscontrol.controller

import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.*

/**
 * Servicios que gestiona solicitudes de copias de seguridad.
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class BackupVSController {

	def mailSenderService
    def eventVSElectionService

	/**
	 * Servicio que recibe solicitudes de copias de seguridad
	 *
	 * @httpMethod [GET, POST]
     * @serviceURL [/backupVS]
     * @param [id] Obligatorio. El identificador de la votación en la base de datos.
     * @param [email] Obligatorio. El email en el que el solicitante recibirá las instrucciones de descarga
	 * @return mensaje indicando el resultado de la solicitud.
	 */
	def index() {
        if (!params.eventId || params.email) {
            def eventVS
            EventVSElection.withTransaction {eventVS = EventVSElection.get(params.long("eventId"))}
            if(!eventVS)  return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                        message(code:'backupRequestEventIdErrorMsg', args:[params.eventId]))]
            if(!eventVS.backupAvailable) return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code:'eventWithoutBackup', args:[eventVS.subject]))]
            log.debug "backup request - eventId: ${params.eventId} - email: ${params.email}"
            final EventVS event = eventVS
            final String emailRequest = params.email
            runAsync {
                ResponseVS backupResponse = eventVSElectionService.generateBackup(event, null)
                if(ResponseVS.SC_OK == backupResponse?.statusCode) {
                    BackupRequestVS backupRequest = new BackupRequestVS(
                            filePath:backupResponse.message, type:TypeVS.VOTING_EVENT, email:emailRequest)
                    BackupRequestVS.withTransaction { backupRequest.save() }
                    mailSenderService.sendBackupMsg(backupRequest)
                } else log.error("Error generating Backup");
            }
            return [responseVS:new ResponseVS(ResponseVS.SC_OK, message(code:'backupRequestOKMsg',args:[params.email]))]
        } else {
            return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                    contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrors'))]
        }
	}

	
	/**
	 * (DISPONIBLE SOLO EN ENTORNOS DE DESARROLLO). Servicio que genera el archivo con los resultados de una votación.
	 *
	 * @httpMethod [GET]
     * @param [id] Obligatorio. El identificador de la votación en la base de datos.
	 */
	def devDownload() {
		if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: "serviceDevelopmentModeMsg"))]
		} else {
            EventVSElection event = null
            EventVSElection.withTransaction { event = EventVSElection.get(params.long('id')) }
            if(!event) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: "nullParamErrorMsg"))]
            } else {
                ResponseVS requestBackup = eventVSElectionService.generateBackup(event, null)
                if(ResponseVS.SC_OK == requestBackup?.statusCode) redirect(uri: requestBackup.message)
                else return [responseVS:requestBackup]
            }
        }
	}
	
	/**
	 * Servicio que proporciona la copia de seguridad a partir de la URL que se envía
	 * al solicitante en el mail de confirmación que recibe al enviar la solicitud.
	 * 
	 * @httpMethod [GET]
     * @serviceURL [/backupVS/download/$id]
	 * @param [id] Obligatorio. El identificador de la solicitud de copia de seguridad la base de datos.
	 * @return Archivo zip con la copia de seguridad.
	 */
	def download() {
		BackupRequestVS backupRequest
		BackupRequestVS.withTransaction { backupRequest = BackupRequestVS.get(params.long('id')) }
		if(!backupRequest) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: 'backupRequestNotFound', args:[params.id]))]
		}
		redirect(uri: backupRequest.filePath)
	}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}