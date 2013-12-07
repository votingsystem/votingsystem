package org.votingsystem.accesscontrol.controller

import com.itextpdf.text.pdf.AcroFields
import com.itextpdf.text.pdf.PdfReader
import org.votingsystem.model.BackupRequestVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EnvironmentVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSClaim
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.EventVSManifest
import org.votingsystem.model.PDFDocumentVS
import org.votingsystem.util.ApplicationContextHolder;
import org.votingsystem.model.ResponseVS
/**
 * @infoController Solicitud de copias de seguridad
 * @descController Servicios que gestiona solicitudes de copias de seguridad.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class BackupVSController {
	
	def eventVSManifestService
	def eventVSClaimService
	def eventVSElectionService
	def mailSenderService

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
        PDFDocumentVS pdfDocument = params.pdfDocument
        if (pdfDocument && pdfDocument.state == PDFDocumentVS.State.VALIDATED) {
            PdfReader reader = new PdfReader(pdfDocument.pdf);
            AcroFields form = reader.getAcroFields();
            String eventId = form.getField("eventId");
            String msg = null
            def eventVS
            if(eventId) {
                EventVS.withTransaction {eventVS = EventVS.get(new Long(eventId))}
            } else msg = message(code: 'backupRequestEventWithoutIdErrorMsg')

            if(!eventVS)
                msg = message(code:'backupRequestEventIdErrorMsg', [eventId])
            if(!eventVS.backupAvailable)
                msg = message(code:'eventWithoutBackup', args:[eventVS.subject])
            String subject = form.getField("subject");
            String email = form.getField("email");
            if(!email)
                msg =  message(code:'backupRequestEmailMissingErrorMsg')

            if(msg) {
                pdfDocument.state = PDFDocumentVS.State.BACKUP_REQUEST_ERROR
            } else pdfDocument.state = PDFDocumentVS.State.BACKUP_REQUEST

            PDFDocumentVS.withTransaction {
                pdfDocument.eventVS = eventVS
                pdfDocument.save(flush:true)
            }
            if(msg) {
                params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,message:msg)
                return false
            }

            log.debug "backup request - eventId: ${eventId} - subject: ${subject} - email: ${email}"
            ResponseVS backupGenResponseVS = null
            if(EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
                log.debug "Request from DEVELOPMENT environment generating sync response"
                backupGenResponseVS = requestBackup(eventVS, request.locale)
                if(ResponseVS.SC_OK == backupGenResponseVS?.statusCode) {
                    BackupRequestVS backupRequest = new BackupRequestVS( filePath:backupGenResponseVS.message,
                            type:backupGenResponseVS.type, PDFDocumentVS:pdfDocument, email:email)
                    BackupRequestVS.withTransaction {backupRequest.save()}
                    params.responseVS = new ResponseVS(ResponseVS.SC_OK, backupRequest.id.toString())
                    return false
                } else {
                    log.error("DEVELOPMENT - error generating backup");
                    params.responseVS = backupGenResponseVS
                    return false
                }
            } else {
                final EventVS event = eventVS
                final Locale locale = request.locale
                final String emailRequest = email
                runAsync {
                    ResponseVS backupResponse = requestBackup(event, locale)
                    if(ResponseVS.SC_OK == backupResponse?.statusCode) {
                        BackupRequestVS backupRequest = new BackupRequestVS(
                                filePath:backupResponse.message, type:backupResponse.type,
                                PDFDocumentVS:pdfDocument, email:emailRequest)
                        BackupRequestVS.withTransaction { backupRequest.save() }
                        mailSenderService.sendBackupMsg(backupRequest, locale)
                    } else log.error("Error generating Backup");
                }
                params.responseVS= new ResponseVS(ResponseVS.SC_OK, message(code:'backupRequestOKMsg',args:[email]))
            }
        } else {
            params.responseVS= new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: 'requestWithErrorsHTML', args:[
                            "${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))
        }
	}
	
	
	private ResponseVS requestBackup(EventVS eventVS, Locale locale) {
		log.debug ("requestBackup")
		ResponseVS backupGenResponseVS
		if(eventVS instanceof EventVSManifest) {
			backupGenResponseVS = eventVSManifestService.generateBackup((EventVSManifest)eventVS, locale)
			log.debug("---> EventVSManifest")
		} else if(eventVS instanceof EventVSClaim) {
			log.debug("---> EventVSClaim")
			backupGenResponseVS = eventVSClaimService.generateBackup((EventVSClaim)eventVS,locale)
		} else if(eventVS instanceof EventVSElection) {
			log.debug("---> EventVSElection")
			backupGenResponseVS = eventVSElectionService.generateBackup((EventVSElection)eventVS, locale)
		}
		return backupGenResponseVS
	}
	
	/**
	 * (SERVICIO DISPONIBLE SOLO EN ENTORNOS DE DESARROLLO). Que genera copias la copia de
	 * respaldo de un eventVS.
	 *
	 * @httpMethod [GET]
	 */
	def devDownload() {
		if(!EnvironmentVS.DEVELOPMENT.equals(ApplicationContextHolder.getEnvironment())) {
            params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: "serviceDevelopmentModeMsg"))
		} else {
            EventVS event = null
            EventVS.withTransaction { event = EventVS.get(params.long('id')) }
            if(!event) {
                params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: "nullParamErrorMsg"))
            } else {
                ResponseVS requestBackup = requestBackup(event, request.locale)
                if(ResponseVS.SC_OK == requestBackup?.statusCode) {
                    redirect(uri: requestBackup.message)
                } else {
                    log.error("DEVELOPMENT - error generating backup");
                    params.responseVS = requestBackup
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
		BackupRequestVS solicitud
		BackupRequestVS.withTransaction {
			solicitud = BackupRequestVS.get(params.long('id'))
		}
		if(!solicitud) {
            params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: 'backupRequestNotFound', args:[params.id]))
			return
		}
		redirect(uri: solicitud.filePath)
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
		BackupRequestVS solicitud
		byte[] solicitudBytes
        ResponseVS responseVS
		BackupRequestVS.withTransaction {
			solicitud = BackupRequestVS.get(params.long('id'))
			if(solicitud) {
				if(solicitud.PDFDocumentVS) {//has PDF
                    responseVS = new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PDF,
                        messageBytes: solicitud.PDFDocumentVS?.pdf)
				} else {//has SMIME
                    responseVS = new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.TEXT_STREAM,
                            messageBytes: solicitud.messageSMIME?.content)
				}
			} 
		}
		if(!solicitud) {
            params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: 'backupRequestNotFound', args:[params.id]))
		} else params.responseVS = responseVS
	}

}