package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.*
import org.votingsystem.util.DateUtils

import javax.xml.bind.annotation.adapters.HexBinaryAdapter

/**
 * @infoController Votaciones
 * @descController Servicios relacionados con la publicación de votaciones.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class EventVSElectionController {

    def eventVSElectionService
    def eventVSService

    /**
     * @httpMethod [GET]
     * @return La página principal de la aplicación web de votación.
     */
    def elections() {
        render(view:"elections")
    }

    def editor() {
        render(view:"editor")
    }

    /**
	 * @httpMethod [GET]
     * @serviceURL [/eventVSElection/$id]
	 * @param [id] Opcional. El identificador de la votación en la base de datos. Si no se pasa ningún id
	 *        la consulta se hará entre todos las votaciones.
	 * @param [max] Opcional (por defecto 20). Número máximo de votaciones que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param [order] Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de inicio.
	 * @responseContentType [application/json]
	 * @return documento JSON con las votaciones que cumplen con el criterio de búsqueda.
	 */
	def index() {
        if (params.long('id')) {
            def resultList
            EventVSElection.withTransaction {
                resultList = EventVSElection.createCriteria().list {
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
                EventVSElection eventVS = resultList.iterator().next()
                eventVS = eventVSService.checkEventVSDates(eventVS).eventVS
                if(request.contentType?.contains('json')) {
                    render eventVSService.getEventVSMap(eventVS) as JSON
                } else {
                    render(view:"eventVSElection", model: [eventMap: eventVSService.getEventVSMap(eventVS)])
                }
            }
            return false
        }
        List<EventVSElection> resultList
        Map eventsVSMap = new HashMap()
        eventsVSMap.eventVS = []
        params.sort = "dateBegin"
        EventVS.State eventVSState
        try {eventVSState = EventVS.State.valueOf(params.eventVSState)} catch(Exception ex) {}
        if(!eventVSState) eventVSState = EventVS.State.ACTIVE
        EventVSElection.withTransaction {
            resultList = EventVSElection.createCriteria().list(max: params.max, offset: params.offset,
                    sort:params.sort, order:'desc') {
                if(eventVSState == EventVS.State.TERMINATED) {
                    or{
                        eq("state", EventVS.State.TERMINATED)
                        eq("state", EventVS.State.CANCELLED)
                    }
                } else if(eventVSState && eventVSState != EventVS.State.DELETED_FROM_SYSTEM) {
                    eq("state", eventVSState)
                }
            }
        }
        eventsVSMap.totalCount = resultList?.totalCount
        eventsVSMap.offset = params.long('offset')
        resultList.each {eventVSItem ->
            eventVSItem = eventVSService.checkEventVSDates(eventVSItem).eventVS
            eventsVSMap.eventVS.add(eventVSService.getEventVSElectionMap(eventVSItem))
        }
        if(request.contentType?.contains("json")) {
            render eventsVSMap as JSON
        } else render(view:"index" , model:[eventsVSMap:eventsVSMap])
	}
	
	/**
	 * Servicio para publicar votaciones.
	 *
	 * @serviceURL [/eventVSElection]
	 * @httpMethod [POST]
	 * 
	 * @requestContentType [application/x-pkcs7-signature] Obligatorio. documento firmado
	 *                     en formato SMIME con los datos de la votación que se desea publicar
	 * @responseContentType [application/x-pkcs7-signature] Obligatorio. Recibo firmado por el sistema.
	 * 
	 * @return Recibo que consiste en el archivo firmado recibido con la signatureVS añadida del servidor.
	 */
    def save () {
		MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        ResponseVS responseVS = eventVSElectionService.saveEvent(messageSMIME)
		if (ResponseVS.SC_OK == responseVS.statusCode) {
			response.setHeader('eventURL', 
				"${grailsApplication.config.grails.serverURL}/eventVSElection/${responseVS.eventVS.id}")
		}
        return [responseVS:responseVS]
    }

	/**
	 * Servicio que devuelve estadísticas asociadas a una votación.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSElection/$id/stats]
	 * @param [id] Obligatorio. Identificador en la base de datos de la votación que se desea consultar.
         * @requestContentType [application/json] Para solicitar una respuesta en formato JSON
         * @requestContentType [text/html] Para solicitar una respuesta en formato HTML
         * @responseContentType [application/json]                    
	 * @responseContentType [text/html]
	 * @return documento JSON con las estadísticas asociadas a la votación solicitada.
	 */
    def stats () {
        if (params.long('id')) {
            EventVSElection eventVSElection
            if (!params.eventVS) {
				EventVSElection.withTransaction {eventVSElection = EventVSElection.get(params.id)}
			} else eventVSElection = params.eventVS
            if (eventVSElection) {
                response.status = ResponseVS.SC_OK
                def statsMap = eventVSElectionService.getStatsMap(eventVSElection)
                if(request.contentType?.contains("json")) {
                    if (params.callback) render "${params.callback}(${statsMap as JSON})"
                    else render statsMap as JSON
                } else render(view:"stats", model: [statsJSON: statsMap  as JSON])
            } else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'eventVSNotFound', args:[params.id]))]
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrors', args:[]))]
    }
    
	/**
	 * Servicio que proporciona una copia de la votación publicada con la signatureVS
	 * añadida del servidor.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSElection/${id}/validated]
	 * @param [id] Obligatorio. El identificador de la votación en la base de datos.
	 * @return Archivo SMIME de la publicación de la votación firmada por el usuario que
	 *         la publicó y el servidor.
	 */
    def validated () {
        if (params.long('id')) {
			def eventVS
			EventVS.withTransaction {
				eventVS = EventVS.get(params.id)
			}
            MessageSMIME messageSMIME
            if (eventVS) {
                MessageSMIME.withTransaction {
                    List results =MessageSMIME.createCriteria().list {
                        smimeParent{
                            eq("eventVS", eventVS)
                            eq("type", TypeVS.VOTING_EVENT)
                        }
                    }
                    messageSMIME = results.iterator().next()
                }
                if (messageSMIME) {
                    return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK,
                            contentType:ContentTypeVS.TEXT_STREAM, messageBytes: messageSMIME.content)]
                }
            }
            if (!eventVS || !messageSMIME) {
                return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                        message(code: 'eventVSNotFound', args:[params.id]))]
            }
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrors', args:[]))]
    }
	
	/**
	 * Servicio que proporciona una copia de la votación publicada.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSElection/${id}/signed]
	 * @param [id] Obligatorio. El identificador de la votación en la base de datos.
	 * @return Archivo SMIME de la publicación de la votación firmada por el usuario
	 *         que la publicó.
	 */
	def signed () {
		if (params.long('id')) {
			def eventVS
			EventVS.withTransaction { eventVS = EventVS.get(params.id) }
			if (eventVS) {
				MessageSMIME messageSMIME
				MessageSMIME.withTransaction {
					messageSMIME = MessageSMIME.findWhere(eventVS:eventVS, type: TypeVS.VOTING_EVENT)
				}
				if (messageSMIME) {
                    return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, messageBytes: messageSMIME.content,
                            contentType: ContentTypeVS.TEXT_STREAM)]
				}
			} else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'eventVSNotFound', args:[params.id]))]
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrors', args:[]))]
	}

	/**
	 * Servicio que devuelve información sobre la actividad de una votación
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSElection/$id/voteVSInfo]
	 * @param [id] Obligatorio. El identificador de la votación en la base de datos.
	 * @responseContentType [application/json]
	 * @return documento JSON con información sobre los votos y solicitudes de acceso de una votación.
	 */
	def voteVSInfo () {
		if (params.long('id')) {
			EventVSElection eventVS
			EventVSElection.withTransaction {
				eventVS = EventVSElection.get(params.long('id'))
			}
			if (!eventVS) {
                return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                        message(code: 'eventVSNotFound', args:[params.id]))]
			}
			def voteVSInfoMap = new HashMap()
			def requestsOK, requestsCancelled;
			AccessRequestVS.withTransaction {
				requestsOK = AccessRequestVS.findAllWhere(state:TypeVS.OK, eventVSElection:eventVS)
				requestsCancelled = AccessRequestVS.findAllWhere(state:TypeVS.CANCELLED, eventVSElection:eventVS)
			}
			def solicitudesCSROk, numCSRCollected_CANCELLED;
            CertificateVS.withTransaction {
				solicitudesCSROk = CertificateVS.findAllWhere(type:CertificateVS.Type.VOTEVS,
                        state:CertificateVS.State.OK, eventVSElection:eventVS)
				numCSRCollected_CANCELLED = CertificateVS.findAllWhere(
					state:CertificateVS.State.CANCELLED, eventVSElection:eventVS)
			}
			voteVSInfoMap.numRequestCollected = eventVS.accessRequest.size()
			voteVSInfoMap.numRequestCollected_OK = requestsOK.size()
			voteVSInfoMap.numRequestCollected_CANCELLED = requestsCancelled.size()
			voteVSInfoMap.numCSRCollected = eventVS.solicitudesCSR.size()
			voteVSInfoMap.numCSRCollected_OK = solicitudesCSROk.size()
			voteVSInfoMap.numCSRCollected_CANCELLED = numCSRCollected_CANCELLED.size()
			voteVSInfoMap.rootCertificateEventVS = "${grailsApplication.config.grails.serverURL}" +
				"/certificateVS/eventCA/${params.id}"
			voteVSInfoMap.accessRequest = []
			voteVSInfoMap.votesVS = []
			voteVSInfoMap.fieldsEventVS = []
			eventVS.accessRequest.each { solicitud ->
				def solicitudMap = [id:solicitud.id, dateCreated:solicitud.dateCreated,
				state:solicitud.state.toString(),
				hashAccessRequestBase64:solicitud.hashAccessRequestBase64,
				userVS:solicitud.userVS.nif,
				accessRequestVSURL:"${grailsApplication.config.grails.serverURL}/messageSMIME" +
					"/get?id=${solicitud?.messageSMIME?.id}"]
				if(AccessRequestVS.State.CANCELLED.equals(solicitud.state)) {
					VoteVSCanceller votevsCanceller = VoteVSCanceller.findWhere(accessRequestVS:solicitud)
					solicitudMap.cancellerURL="${grailsApplication.config.grails.serverURL}" +
						"/messageSMIME/${votevsCanceller?.messageSMIME?.id}"
				}
				voteVSInfoMap.accessRequest.add(solicitudMap)
			}
			eventVS.fieldsEventVS.each { option ->
				def numVotesVS = VoteVS.findAllWhere(optionSelected:option, state:VoteVS.State.OK).size()
				def optionMap = [id:option.id, content:option.content, numVotesVS:numVotesVS]
				voteVSInfoMap.fieldsEventVS.add(optionMap)
			}
			
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			eventVS.votesVS.each { voteVS ->
				def hashCertVoteHex = hexConverter.marshal(
					voteVS.getCertificateVS.hashCertVSBase64.getBytes() )
				def voteVSMap = [id:voteVS.id, optionSelectedId:voteVS.getFieldEventVS.id,
					state:voteVS.state.toString(),
					hashCertVSBase64:voteVS.getCertificateVS.hashCertVSBase64,
					certificateURL:"${grailsApplication.config.grails.serverURL}/certificateVS" +
						"/voteVS/${hashCertVoteHex}",
					voteVSURL:"${grailsApplication.config.grails.serverURL}/messageSMIME" +
						"/${voteVS.messageSMIME.id}"]
				if(VoteVS.State.CANCELLED.equals(voteVS.state)) {
					VoteVSCanceller votevsCanceller = VoteVSCanceller.findWhere(voteVS:voteVS)
					voteVSMap.cancellerURL="${grailsApplication.config.grails.serverURL}" +
						"/messageSMIME/${votevsCanceller?.messageSMIME?.id}"
				}
				voteVSInfoMap.votesVS.add(voteVSMap)
			}
			render voteVSInfoMap as JSON
		} else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrors', args:[]))]
	}

	/**
	 * Servicio que devuelve un archivo zip con los errores que se han producido
	 * en una votación
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSElection/$id/votingErrors]
	 * @param [id] Obligatorio. Identificador del eventVS en la base de datos
	 * @return Archivo zip con los messages con errores
	 */
	def votingErrors() {
		EventVSElection eventVS
		EventVSElection.withTransaction {
			eventVS = EventVSElection.get(params.long('id'))
		}
		if (!eventVS) {
            return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND, message(code:'eventVSNotFound',args:[params.id]))]
		}
		def errors
		MessageSMIME.withTransaction { errors = MessageSMIME.findAllWhere ( type:TypeVS.VOTE_ERROR,  eventVS:eventVS) }

		if(errors.size == 0){
            return [responseVS:new ResponseVS(ResponseVS.SC_OK, message(code: 'votingWithoutErrorsMsg',
                    args:[eventVS.id, eventVS.subject]))]
		} else {
			String datePathPart = DateUtils.getDateStr(eventVS.getDateFinish(),"yyyy/MM/dd")
			String baseDirPath = "${grailsApplication.config.vs.errorsBaseDir}" +
				"/${datePathPart}/Event_${eventVS.id}"
			errors.each { messageSMIME ->
				File errorFile = new File("${baseDirPath}/MessageSMIME_${messageSMIME.id}")
				errorFile.setBytes(messageSMIME.content)
			}
			File zipResult = new File("${baseDirPath}.zip")
			def ant = new AntBuilder()
			ant.zip(destfile: zipResult, basedir: "${baseDirPath}")
            return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, messageBytes:zipResult.getBytes(),
                contentType: ContentTypeVS.ZIP)]
		}
	}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}