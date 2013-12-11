package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.AccessRequestVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubSystemVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.VoteRequestCsrVS
import org.votingsystem.model.VoteVS
import org.votingsystem.model.VoteVSCanceller
import org.votingsystem.util.DateUtils

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
/**
 * @infoController Votaciones
 * @descController Servicios relacionados con la publicación de votaciones.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class EventVSElectionController {

    def eventVSElectionService
    def eventVSService


    /**
     * @httpMethod [GET]
     * @return La página principal de la aplicación web de votación.
     */
    def main() {
        render(view:"main" , model:[selectedSubsystem:SubSystemVS.VOTES.toString()])
    }

    /**
     * @httpMethod [GET]
     * @return La página principal de la aplicación web de votación.
     */
    def elections() {
        render(view:"elections" , model:[selectedSubsystem:SubSystemVS.VOTES.toString()])
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
        def eventVSList = []
        def eventsVSMap = new HashMap()
        eventsVSMap.eventsVS = new HashMap()
        eventsVSMap.eventsVS.elections = []
        if (params.long('id')) {
            EventVSElection eventVS = null
            EventVSElection.withTransaction { eventVS = EventVSElection.get(params.long('id'))}
            if(eventVS) {
                if(!(eventVS.state == EventVS.State.ACTIVE || eventVS.state == EventVS.State.AWAITING ||
                        eventVS.state == EventVS.State.CANCELLED || eventVS.state == EventVS.State.TERMINATED)) {
                    eventVS = null   }
            }
            if(!eventVS) {
                params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                        message(code: 'eventVSNotFound', args:[params.id]))
                return
            } else {
                if(request.contentType?.contains(ContentTypeVS.JSON.getName())) {
                    render eventVSService.getEventVSMap(eventVS) as JSON
                    return
                } else {
                    render(view:"eventVSElection", model: [selectedSubsystem:SubSystemVS.VOTES.toString(),
                            eventMap: eventVSService.getEventVSMap(eventVS)])
                    return
                }
            }
        } else {
            params.sort = "dateBegin"
            log.debug " -Params: " + params
            EventVS.State eventVSState
            if(params.eventVSState) eventVSState = EventVS.State.valueOf(params.eventVSState)
            EventVSElection.withTransaction {
                if(eventVSState) {
                    if(eventVSState == EventVS.State.TERMINATED) {
                        eventVSList =  EventVSElection.findAllByStateOrState(
                                EventVS.State.CANCELLED, EventVS.State.TERMINATED, params)
                        eventsVSMap.numEventsVSElectionInSystem = EventVSElection.countByStateOrState(
                                EventVS.State.CANCELLED, EventVS.State.TERMINATED)
                    } else {
                        eventVSList =  EventVSElection.findAllByState(eventVSState, params)
                        log.debug " -eventVSState: " + eventVSState
                        eventsVSMap.numEventsVSElectionInSystem = EventVSElection.countByState(eventVSState)
                    }
                } else {
                    eventVSList =  EventVSElection.findAllByStateOrStateOrStateOrState(EventVS.State.ACTIVE,
                            EventVS.State.CANCELLED, EventVS.State.TERMINATED, EventVS.State.AWAITING, params)
                    eventsVSMap.numEventsVSElectionInSystem =
                            EventVSElection.countByStateOrStateOrStateOrState(EventVS.State.ACTIVE,
                                    EventVS.State.CANCELLED, EventVS.State.TERMINATED, EventVS.State.AWAITING)
                }
            }
            eventsVSMap.offset = params.long('offset')
        }
        eventsVSMap.numEventsVSElection = eventVSList.size()
        eventVSList.each {eventVSItem ->
            eventsVSMap.eventsVS.elections.add(eventVSService.getEventVSElectionMap(eventVSItem))
        }
        render eventsVSMap as JSON
	}
	
	/**
	 * Servicio para publicar votaciones.
	 *
	 * @serviceURL [/eventVSElection]
	 * @httpMethod [POST]
	 * 
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. documento firmado
	 *                     en formato SMIME con los datos de la votación que se desea publicar
	 * @responseContentType [application/x-pkcs7-signature] Obligatorio. Recibo firmado por el sistema.
	 * 
	 * @return Recibo que consiste en el archivo firmado recibido con la signatureVS añadida del servidor.
	 */
    def save () {
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
            params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "requestWithoutFile"))
            return
		}
        ResponseVS responseVS = eventVSElectionService.saveEvent(messageSMIME, request.getLocale())
		params.responseVS = responseVS
		if (ResponseVS.SC_OK == responseVS.statusCode) {
			response.setHeader('eventURL', 
				"${grailsApplication.config.grails.serverURL}/eventVSElection/${responseVS.eventVS.id}")
            responseVS.setContentType(ContentTypeVS.SIGNED)
		}
    }

	/**
	 * Servicio que devuelve estadísticas asociadas a una votación.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSElection/$id/statistics]
	 * @param [id] Obligatorio. Identificador en la base de datos de la votación que se desea consultar.
         * @requestContentType [application/json] Para solicitar una respuesta en formato JSON
         * @requestContentType [text/html] Para solicitar una respuesta en formato HTML
         * @responseContentType [application/json]                    
	 * @responseContentType [text/html]
	 * @return documento JSON con las estadísticas asociadas a la votación solicitada.
	 */
    def statistics () {
        if (params.long('id')) {
            EventVSElection eventVSElection
            if (!params.eventVS) {
				EventVSElection.withTransaction {eventVSElection = EventVSElection.get(params.id)}
			} 
            else eventVSElection = params.eventVS
            if (eventVSElection) {
                response.status = ResponseVS.SC_OK
                def statisticsMap = eventVSElectionService.getStatisticsMap(eventVSElection, request.getLocale())
                if(request.contentType?.contains(ContentTypeVS.JSON.getName())) {
                    if (params.callback) render "${params.callback}(${statisticsMap as JSON})"
                    else render statisticsMap as JSON
                } else render(view:"statistics", model: [statisticsJSON: statisticsMap  as JSON])
            } else params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'eventVSNotFound', args:[params.id]))
        } else params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'requestWithErrorsHTML',
                args:[ "${grailsApplication.config.grails.serverURL}/${params.controller}"]))
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
					List results = MessageSMIME.withCriteria {
						createAlias("smimeParent", "smimeParent")
						eq("smimeParent.eventVS", eventVS)
						eq("smimeParent.type", TypeVS.VOTING_EVENT)
					}
					messageSMIME = results.iterator().next()
				}
                if (messageSMIME) {
                    params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK,
                            contentType:ContentTypeVS.TEXT_STREAM, messageBytes: messageSMIME.content)
                }
            }
            if (!eventVS || !messageSMIME) {
                params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                        message(code: 'eventVSNotFound', args:[params.id]))
            }
        } else params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'requestWithErrorsHTML',
                args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))
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
                    params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, messageBytes: messageSMIME.content,
                            contentType: ContentTypeVS.TEXT_STREAM)
				}
			} else params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'eventVSNotFound', args:[params.id]))
        } else params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'requestWithErrorsHTML',
                args:["${grailsApplication.config.grails.serverURL}/${params.controller}"]))
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
                params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                        message(code: 'eventVSNotFound', args:[params.id]))
			}
			def voteVSInfoMap = new HashMap()
			def requestsOK, requestsCancelled;
			AccessRequestVS.withTransaction {
				requestsOK = AccessRequestVS.findAllWhere(state:TypeVS.OK, eventVSElection:eventVS)
				requestsCancelled = AccessRequestVS.findAllWhere(state:TypeVS.CANCELLED, eventVSElection:eventVS)
			}
			def solicitudesCSROk, numCSRCollected_CANCELLED;
			VoteRequestCsrVS.withTransaction {
				solicitudesCSROk =VoteRequestCsrVS.findAllWhere(state:VoteRequestCsrVS.State.OK,eventVSElection:eventVS)
				numCSRCollected_CANCELLED = VoteRequestCsrVS.findAllWhere(
					state:VoteRequestCsrVS.State.CANCELLED, eventVSElection:eventVS)
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
					VoteVSCanceller anuladorVoto = VoteVSCanceller.findWhere(accessRequestVS:solicitud)
					solicitudMap.cancellerURL="${grailsApplication.config.grails.serverURL}" +
						"/messageSMIME/${anuladorVoto?.messageSMIME?.id}"
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
					voteVS.getCertificateVS.hashCertVoteBase64.getBytes() )
				def voteVSMap = [id:voteVS.id, optionSelectedId:voteVS.getFieldEventVS.id,
					state:voteVS.state.toString(),
					hashCertVoteBase64:voteVS.getCertificateVS.hashCertVoteBase64,
					certificateURL:"${grailsApplication.config.grails.serverURL}/certificateVS" +
						"/voteVS/${hashCertVoteHex}",
					voteVSURL:"${grailsApplication.config.grails.serverURL}/messageSMIME" +
						"/${voteVS.messageSMIME.id}"]
				if(VoteVS.State.CANCELLED.equals(voteVS.state)) {
					VoteVSCanceller anuladorVoto = VoteVSCanceller.findWhere(voteVS:voteVS)
					voteVSMap.cancellerURL="${grailsApplication.config.grails.serverURL}" +
						"/messageSMIME/${anuladorVoto?.messageSMIME?.id}"
				}
				voteVSInfoMap.votesVS.add(voteVSMap)
			}
			render voteVSInfoMap as JSON
		} else params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'requestWithErrorsHTML',
                args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))
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
            params.responseVS =new ResponseVS(ResponseVS.SC_NOT_FOUND, message(code:'eventVSNotFound',args:[params.id]))
            return
		}
		def errors
		MessageSMIME.withTransaction { errors = MessageSMIME.findAllWhere ( type:TypeVS.VOTE_ERROR,  eventVS:eventVS) }

		if(errors.size == 0){
            params.responseVS = new ResponseVS(ResponseVS.SC_OK, message(code: 'votingWithoutErrorsMsg',
                    args:[eventVS.id, eventVS.subject]))
		} else {
			String datePathPart = DateUtils.getShortStringFromDate(eventVS.getDateFinish())
			String baseDirPath = "${grailsApplication.config.VotingSystem.errorsBaseDir}" +
				"/${datePathPart}/Event_${eventVS.id}"
			errors.each { messageSMIME ->
				File errorFile = new File("${baseDirPath}/MessageSMIME_${messageSMIME.id}")
				errorFile.setBytes(messageSMIME.content)
			}
			File zipResult = new File("${baseDirPath}.zip")
			def ant = new AntBuilder()
			ant.zip(destfile: zipResult, basedir: "${baseDirPath}")
            params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, messageBytes:zipResult.getBytes(),
                contentType: ContentTypeVS.ZIP)
		}
	}


}