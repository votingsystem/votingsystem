package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.FieldValueEventVS
import org.votingsystem.model.SignatureVS
import org.votingsystem.model.EventVSClaim
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubSystemVS
import org.votingsystem.model.TypeVS


/**
 * @infoController Reclamaciones
 * @descController Servicios relacionados con la publicación de reclamaciones.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class EventVSClaimController {

    def eventVSClaimService
	def eventVSClaimSignatureCollectorService
    def eventVSService
	def signatureVSService
	
	/**
	 * @httpMethod [GET]
	 * @return La página principal de la aplicación web de reclamaciones.
	 */
	def main() {
		render(view:"main" , model:[selectedSubsystem:SubSystemVS.CLAIMS.toString()])
	}
	
	/**
	 * @httpMethod [GET]
	 * @param [id] Opcional. El identificador de la reclamación en la base de datos. Si no se pasa ningún 
	 *        id la consulta se hará entre todos las reclamaciones.
	 * @param [max] Opcional (por defecto 20). Número máximo de reclamaciones que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param [order] Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de inicio.
	 * @responseContentType [application/json]
	 * @return documento JSON con los manifiestos que cumplen con el criterio de búsqueda.
	 */
    def index () {
        def resultList
        if (params.long('id')) {
            EventVSClaim.withTransaction {
                resultList = EventVSClaim.createCriteria().list {
                    or {
                        eq("state", EventVS.State.ACTIVE)
                        eq("state", EventVS.State.AWAITING)
                        eq("state", EventVS.State.CANCELLED)
                        eq("state", EventVS.State.TERMINATED)
                    }
                    and { eq("id", params.long('id'))}
                }
            }
			if(resultList.isEmpty()) {
                return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                        message(code: 'eventVSNotFound', args:[params.id]))]
			} else {
                EventVSClaim eventVS = resultList.iterator().next()
				if(request.contentType?.contains(ContentTypeVS.JSON.getName())) {
					return [responseVS: new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.JSON,
                            data:eventVSService.getEventVSMap(eventVS))]
				} else {
					render(view:"eventVSClaim", model: [selectedSubsystem:SubSystemVS.CLAIMS.toString(),
						eventMap: eventVSService.getEventVSMap(eventVS)])
				}
			}
        } else {
            def eventsVSMap = new HashMap()
            eventsVSMap.eventVS = []
            params.sort = "dateBegin"
            EventVS.State eventVSState
            try {eventVSState = EventVS.State.valueOf(params.eventVSState)} catch(Exception ex) {}
            EventVSClaim.withTransaction {
                resultList = EventVSClaim.createCriteria().list(max: params.max, offset: params.offset,
                        sort:params.sort, order:params.order) {
                    if(eventVSState == EventVS.State.TERMINATED) {
                        or{
                            eq("state", EventVS.State.TERMINATED)
                            eq("state", EventVS.State.CANCELLED)
                        }
                    } else if(eventVSState) {
                        eq("state", eventVSState)
                    } else {
                        or{
                            eq("state", EventVS.State.ACTIVE)
                            eq("state", EventVS.State.AWAITING)
                            eq("state", EventVS.State.TERMINATED)
                            eq("state", EventVS.State.CANCELLED)
                        }
                    }
                }
                eventsVSMap.totalEventVS = resultList.totalCount
            }
            eventsVSMap.offset = params.long('offset')
            resultList.each {eventVSItem ->
                eventsVSMap.eventVS.add(eventVSService.getEventVSClaimMap(eventVSItem))
            }
            render eventsVSMap as JSON
        }
    }
    
	/**
	 * Servicio que proporciona el recibo con el que el sistema
	 * respondió a una solicitud de publicación de reclamación.
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSClaim/${id}/validated]
	 * @param [id] Obligatorio. El identificador de la reclamación en la base de datos.
	 * @return documento SMIME con el recibo.
	 */
    def validated () {
        def eventVS
		EventVS.withTransaction {
			eventVS = EventVSClaim.get(params.long('id'))
		}
        MessageSMIME messageSMIME
        if (eventVS?.id) {
            MessageSMIME.withTransaction {
                List results =MessageSMIME.createCriteria().list {
                    smimeParent{
                        eq("eventVS", eventVS)
                        eq("type", TypeVS.CLAIM_EVENT)
                    }
                }
                messageSMIME = results.iterator().next()
            }
            if (messageSMIME) {
                response.status = ResponseVS.SC_OK
                response.contentLength = messageSMIME.content.length
                //response.setContentType(ContentTypeVS.TEXT.getName())
                response.outputStream <<  messageSMIME.content
                response.outputStream.flush()
                return false
            }
        }
        if (!eventVS || !messageSMIME) {
            return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,message(code:'eventVSNotFound',args:[params.id]))]
        }
    }
    
	/**
	 * Servicio que proporciona una copia de la reclamación publicada.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSClaim/${id}/signed]
	 * @param [id] Obligatorio. El identificador de la reclamación en la base de datos.
	 * @return documento SMIME con la solicitud de publicación de la reclamación.
	 */
	def signed () {
		def eventVS
		EventVS.withTransaction { eventVS = EventVSClaim.get(params.long('id')) }
		if (eventVS) {
			MessageSMIME messageSMIME
			MessageSMIME.withTransaction {
				messageSMIME = MessageSMIME.findWhere(eventVS:eventVS, type:TypeVS.CLAIM_EVENT)
			}
			if (messageSMIME) {
                return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType:ContentTypeVS.TEXT_STREAM,
                        messageBytes: messageSMIME.content)]
			}
		} else return [responseVS: new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'eventVSNotFound', args:[params.id]))]
	}
    
	/**
	 * Servicio para publicar reclamaciones.
	 *
	 * @httpMethod [POST]
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. 
	 *                     documento en formato SMIME  en cuyo content se
	 *        encuentra la reclamación que se desea publicar en formato HTML.
	 * @responseContentType [application/x-pkcs7-signature] Obligatorio. Recibo firmado por el sistema.
	 * @return Recibo que consiste en el documento SMIME recibido con la signatureVS añadida del servidor.
	 */
    def save () {
		MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = eventVSClaimService.saveEvent(messageSMIMEReq, request.getLocale())
        if(ResponseVS.SC_OK == responseVS.statusCode) {
            response.setHeader('eventURL',
                    "${grailsApplication.config.grails.serverURL}/eventVSClaim/${responseVS.eventVS.id}")
            responseVS.setContentType(ContentTypeVS.SIGNED)
        }
        return [responseVS:responseVS]
    }
	
	/**
	 * Servicio que devuelve estadísticas asociadas a una reclamación.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSClaim/$id/statistics]
	 * @param [id] Obligatorio. Identificador en la base de datos de la reclamación que se desea consultar.
	 * @responseContentType [application/json]
	 * @return documento JSON con las estadísticas asociadas a la reclamación solicitada.
	 */
    def statistics () {
        EventVSClaim eventVSClaim
		if (!params.eventVS) {
			EventVSClaim.withTransaction {
				eventVSClaim = EventVSClaim.get(params.long('id'))
			}
		} else eventVSClaim = params.eventVS
        if (eventVSClaim) {
            def statisticsMap = eventVSClaimSignatureCollectorService.getStatisticsMap(eventVSClaim, request.getLocale())
			if(request.contentType?.contains(ContentTypeVS.JSON.getName())) {
				if (params.callback) render "${params.callback}(${statisticsMap as JSON})"
				else render statisticsMap as JSON
				return false
			} else {
				render(view:"statistics", model: [statisticsMap:statisticsMap])
				return
			}
        }
        return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND, message(code: 'eventVSNotFound', args:[params.id]))]
    }

	
	/**
	 * Servicio que devuelve información sobre la actividad de una acción de reclamación
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSClaim/$id/signaturesInfo]
	 * @param [id] Obligatorio. El identificador de la reclamación la base de datos.
	 * @responseContentType [application/json]
	 * @return documento JSON con información sobre las firmas recibidas por la reclamación solicitada.
	 */
	def signaturesInfo () {
		EventVSClaim eventVS = EventVSClaim.get(params.id)
		if (!eventVS) {
            return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'eventVSNotFound', args:[params.id]))]
		}
		def eventVSClaimInfoMap = new HashMap()
		List<SignatureVS> signatures
		SignatureVS.withTransaction {
			signatures = SignatureVS.findAllWhere(eventVS:eventVS, type:TypeVS.CLAIM_EVENT_SIGN)
		}
		log.debug("count: " + SignatureVS.findAllWhere(eventVS:eventVS, type:TypeVS.CLAIM_EVENT_SIGN).size())
		eventVSClaimInfoMap.numSignatures = signatures.size()
		eventVSClaimInfoMap.eventVSSubject = eventVS.subject
		eventVSClaimInfoMap.eventURL =
			"${grailsApplication.config.grails.serverURL}/eventVS/${eventVS.id}"
		eventVSClaimInfoMap.signatures = []
		signatures.each { firma ->
			def signatureMap = [id:firma.id, dateCreated:firma.dateCreated, userVS:firma.userVS.nif,
			firmaReclamacionURL:"${grailsApplication.config.grails.serverURL}/messageSMIME" +
				"/get?id=${firma.messageSMIME.id}",
			reciboFirmaReclamacionURL:"${grailsApplication.config.grails.serverURL}/messageSMIME" +
				"/receipt/${firma.messageSMIME?.id}"]
			def fieldValues = FieldValueEventVS.findAllWhere(firma:firma)
			signatureMap.fieldsEventVS = []
			fieldValues.each { fieldValue ->
				signatureMap.fieldsEventVS.add([campo:fieldValue.getFieldEventVS.content, value:fieldValue.value])
			}
			eventVSClaimInfoMap.signatures.add(signatureMap)
		}
		render eventVSClaimInfoMap as JSON
	}
}
