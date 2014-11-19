package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.*

/**
 * @infoController Reclamaciones
 * @descController Servicios relacionados con la publicación de reclamaciones.
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class EventVSClaimController {

    def eventVSClaimService
	def eventVSClaimSignatureCollectorService
    def eventVSService
	def signatureVSService

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
        if (params.long('id')) {
            def resultList
            EventVSClaim.withTransaction {
                resultList = EventVSClaim.createCriteria().list {
                    or {
                        eq("state", EventVS.State.ACTIVE)
                        eq("state", EventVS.State.PENDING)
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
                eventVS = eventVSService.checkEventVSDates(eventVS).eventVS
				if(request.contentType?.contains(ContentTypeVS.JSON.getName())) {
					return [responseVS: new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.JSON,
                            data:eventVSService.getEventVSMap(eventVS))]
				} else {
					render(view:"eventVSClaim", model: [eventMap: eventVSService.getEventVSMap(eventVS)])
				}
			}
        } else if(request.contentType?.contains("json")){
            def resultList
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
                            eq("state", EventVS.State.PENDING)
                            eq("state", EventVS.State.TERMINATED)
                            eq("state", EventVS.State.CANCELLED)
                        }
                    }
                }
                eventsVSMap.totalCount = resultList.totalCount
            }
            eventsVSMap.offset = params.long('offset')
            resultList.each {eventVSItem ->
                eventVSItem = eventVSService.checkEventVSDates(eventVSItem).eventVS
                eventsVSMap.eventVS.add(eventVSService.getEventVSClaimMap(eventVSItem))
            }
            render eventsVSMap as JSON
        } else render(view:"main")
    }

    def editor() {
        render(view:"editor")
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
	 * Claim publishing service.
	 *
	 * @httpMethod [POST]
	 * @requestContentType [application/pkcs7-signature] required. The content of the event to publish.
	 * @responseContentType [application/pkcs7-signature] required. The request signed by the system.
	 */
    def save () {
		MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        ResponseVS responseVS = eventVSClaimService.saveEvent(messageSMIME)
        if(ResponseVS.SC_OK == responseVS.statusCode) {
            response.setHeader('eventURL',
                    "${grailsApplication.config.grails.serverURL}/eventVSClaim/${responseVS.eventVS.id}")
        }
        return [responseVS:responseVS]
    }
	
	/**
	 * Servicio que devuelve estadísticas asociadas a una reclamación.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSClaim/$id/stats]
	 * @param [id] Obligatorio. Identificador en la base de datos de la reclamación que se desea consultar.
	 * @responseContentType [application/json]
	 * @return documento JSON con las estadísticas asociadas a la reclamación solicitada.
	 */
    def stats () {
        EventVSClaim eventVSClaim
		if (!params.eventVS) {
			EventVSClaim.withTransaction { eventVSClaim = EventVSClaim.get(params.long('id')) }
		} else eventVSClaim = params.eventVS
        if (eventVSClaim) {
            def statsMap = eventVSClaimSignatureCollectorService.getStatsMap(eventVSClaim)
			if(request.contentType?.contains(ContentTypeVS.JSON.getName())) {
				if (params.callback) render "${params.callback}(${statsMap as JSON})"
				else render statsMap as JSON
				return false
			} else {
				render(view:"stats", model: [statsMap:statsMap])
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
		signatures.each { signature ->
			def signatureMap = [id:signature.id, dateCreated:signature.dateCreated, userVS:signature.userVS.nif,
			firmaReclamacionURL:"${grailsApplication.config.grails.serverURL}/messageSMIME" +
				"/get?id=${signature.messageSMIME.id}",
			reciboFirmaReclamacionURL:"${grailsApplication.config.grails.serverURL}/messageSMIME" + "/${signature.messageSMIME?.id}"]
			def fieldValues = FieldValueEventVS.findAllWhere(signatureVS:signature)
			signatureMap.fieldsEventVS = []
			fieldValues.each { fieldValue ->
				signatureMap.fieldsEventVS.add([content:fieldValue.getFieldEventVS.content, value:fieldValue.value])
			}
			eventVSClaimInfoMap.signatures.add(signatureMap)
		}
		render eventVSClaimInfoMap as JSON
	}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
