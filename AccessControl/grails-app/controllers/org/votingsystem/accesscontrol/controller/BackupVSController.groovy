package org.votingsystem.accesscontrol.controller

import com.itextpdf.text.pdf.AcroFields
import com.itextpdf.text.pdf.PdfReader
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.*
import org.votingsystem.util.ApplicationContextHolder

/**
 * @infoController Solicitud de copias de seguridad
 * @descController Servicios que gestiona solicitudes de copias de seguridad.
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class BackupVSController {

	def mailSenderService
    def eventVSManifestService
    def eventVSClaimService
    def eventVSElectionService

	/**
	 * Servicio que recibe solicitudes de copias de seguridad
	 *
	 * @httpMethod [POST]
     * @serviceURL [/backupVS]
     * @requestContentType [application/pdf,application/x-pkcs7-signature] Obligatorio. 
     *              El archivo PDF con los datos de la copia de seguridad.
	 * @return Si los datos son correctos el solicitante recibirá un
	 *         email con información para poder get la copia de seguridad.
	 */
	def index() {
        PDFDocumentVS pdfDocument = request.pdfDocument
        if (pdfDocument && pdfDocument.state == PDFDocumentVS.State.VALIDATED) {
            PdfReader reader = new PdfReader(pdfDocument.pdf);
            AcroFields form = reader.getAcroFields();
            String eventId = form.getField("eventId");
            def eventVS
            if(eventId) EventVS.withTransaction {eventVS = EventVS.get(new Long(eventId))}
            if(!eventVS)  return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                        message(code:'backupRequestEventIdErrorMsg', args:[eventId]))]
            if(!eventVS.backupAvailable) return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code:'eventWithoutBackup', args:[eventVS.subject]))]
            String subject = form.getField("subject");
            String email = form.getField("email");
            if(!email) return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code:'backupRequestEmailMissingErrorMsg'))]
            pdfDocument.state = PDFDocumentVS.State.BACKUP_REQUEST
            PDFDocumentVS.withTransaction {
                pdfDocument.eventVS = eventVS
                pdfDocument.save(flush:true)
            }
            log.debug "backup request - eventId: ${eventId} - subject: ${subject} - email: ${email}"
            ResponseVS backupGenResponseVS = null
            if(grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
                log.debug "Request from DEVELOPMENT environment generating sync response"
                backupGenResponseVS = requestBackup(eventVS)
                if(ResponseVS.SC_OK == backupGenResponseVS?.statusCode) {
                    BackupRequestVS backupRequest = new BackupRequestVS(filePath:backupGenResponseVS.message,
                            type:backupGenResponseVS.type, PDFDocumentVS:pdfDocument, email:email)
                    BackupRequestVS.withTransaction {backupRequest.save()}
                    mailSenderService.sendBackupMsg(backupRequest)
                    return [responseVS:new ResponseVS(ResponseVS.SC_OK, backupRequest.id.toString())]
                } else return [responseVS:backupGenResponseVS]
            } else {
                final EventVS event = eventVS
                final String emailRequest = email
                runAsync {
                    ResponseVS backupResponse = requestBackup(event)
                    if(ResponseVS.SC_OK == backupResponse?.statusCode) {
                        BackupRequestVS backupRequest = new BackupRequestVS(
                                filePath:backupResponse.message, type:backupResponse.type,
                                PDFDocumentVS:pdfDocument, email:emailRequest)
                        BackupRequestVS.withTransaction { backupRequest.save() }
                        mailSenderService.sendBackupMsg(backupRequest)
                    } else log.error("Error generating Backup");
                }
                return [responseVS:new ResponseVS(ResponseVS.SC_OK, message(code:'backupRequestOKMsg',args:[email]))]
            }
        } else {
            return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                    contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrorsHTML',
                    args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))]
        }
	}

	
	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE DESARROLLO). Que genera copias la copia de
	 * respaldo de un eventVS.
	 *
	 * @httpMethod [GET]
	 */
	def devDownload() {
		if(!grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: "serviceDevelopmentModeMsg"))]
		} else {
            EventVS event = null
            EventVS.withTransaction { event = EventVS.get(params.long('id')) }
            if(!event) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: "nullParamErrorMsg"))]
            } else {
                ResponseVS requestBackup = requestBackup(event)
                if(ResponseVS.SC_OK == requestBackup?.statusCode) {
                    redirect(uri: requestBackup.message)
                } else {
                    log.error("DEVELOPMENT - error generating backup");
                    return [responseVS:requestBackup]
                }
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
	 * Servicio que proporciona copias de las solicitudes de copias de seguridad recibidas.
	 *
	 * @httpMethod [GET]
     * @serviceURL [/backupVS/$id]
	 * @param [id] Obligatorio. El identificador de la solicitud de copia de seguridad la base de datos.
	 * @return El PDF en el que se solicita la copia de seguridad.
	 */
	def get() {
		BackupRequestVS backupRequest
        ResponseVS responseVS
		BackupRequestVS.withTransaction {
			backupRequest = BackupRequestVS.get(params.long('id'))
			if(backupRequest) {
				if(backupRequest.PDFDocumentVS) {//has PDF
                    responseVS = new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PDF,
                        messageBytes: backupRequest.PDFDocumentVS?.pdf)
				} else {//has SMIME
                    responseVS = new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.TEXT_STREAM,
                            messageBytes: backupRequest.messageSMIME?.content)
				}
			} 
		}
		if(!backupRequest) return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: 'backupRequestNotFound', args:[params.id]))]
		else return [responseVS:responseVS]
	}

    private ResponseVS requestBackup(EventVS eventVS) {
        ResponseVS backupGenResponseVS
        if(eventVS instanceof EventVSManifest) {
            backupGenResponseVS = eventVSManifestService.generateBackup((EventVSManifest)eventVS)
            log.debug("requestBackup - EventVSManifest")
        } else if(eventVS instanceof EventVSClaim) {
            log.debug("requestBackup - EventVSClaim")
            backupGenResponseVS = eventVSClaimService.generateBackup((EventVSClaim)eventVS)
        } else if(eventVS instanceof EventVSElection) {
            log.debug("requestBackup - EventVSElection")
            backupGenResponseVS = eventVSElectionService.generateBackup((EventVSElection)eventVS)
        } else  log.debug ("unknown eventVS class: ${eventVS.class}")
        return backupGenResponseVS
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}