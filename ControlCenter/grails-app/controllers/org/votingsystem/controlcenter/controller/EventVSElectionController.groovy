package org.votingsystem.controlcenter.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.*
import org.votingsystem.util.DateUtils

import javax.xml.bind.annotation.adapters.HexBinaryAdapter

/**
 * @infoController Votaciones
 * @descController Servicios relacionados con las votaciones publicadas en el servidor.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class EventVSElectionController {

    def eventVSElectionService
	def grailsApplication

	/**
	 * Servicio de consulta de las votaciones publicadas.
	 *
	 * @param [id]  Opcional. El identificador en la base de datos del documento que se
	 * 			  desee consultar.
	 * @param [max]	Opcional (por defecto 20). Número máximo de documentos que
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [offset]	Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @httpMethod [GET]
	 * @serviceURL [/eventVS/$id?]
	 * @responseContentType [application/json]
	 * @return Documento JSON con las votaciones que cumplen el criterio de búsqueda.
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
                eventVS = eventVSElectionService.checkEventVSDates(eventVS).eventVS
                if(request.contentType?.contains('json')) {
                    render eventVSElectionService.getEventVSElectionMap(eventVS) as JSON
                } else {
                    render(view:"eventVSElection", model: [eventMap: eventVSElectionService.getEventVSElectionMap(eventVS)])
                }
            }
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
            eventVSItem = eventVSElectionService.checkEventVSDates(eventVSItem).eventVS
            eventsVSMap.eventVS.add(eventVSElectionService.getEventVSElectionMap(eventVSItem))
        }
        if(request.contentType?.contains("json")) {
            render eventsVSMap as JSON
        } else render(view:"index" , model:[eventsVSMap:eventsVSMap])
	}

	/**
	 * Servicio que da de alta las votaciones.
	 * 
	 * @httpMethod [POST]
	 * @serviceURL [/eventVS]
	 * @contentType [application/pkcs7-signature] Obligatorio. El archivo con los datos de la votación firmado
	 * 		  por el usuario que la publica y el Control de Acceso en el que se publica.
	 */
	def save () {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        return [responseVS : eventVSElectionService.saveEvent(messageSMIME)]
	}

	/**
	 * Servicio de consulta de los votesVS
	 *
	 * @param [eventAccessControlURL] Obligatorio. URL del evento en el Control de Acceso.
	 * @httpMethod [GET]
	 * @responseContentType [application/json]
	 * @return Documento JSON con la lista de votesVS recibidos por la votación solicitada.
	 */
    def votes () {
        if (params.eventAccessControlURL) {
            EventVSElection eventVSElection
            EventVSElection.withTransaction {
                eventVSElection = EventVSElection.findWhere(url:params.eventAccessControlURL)
			}
            if (eventVSElection) {
                def votesVSMap = new HashMap()
                votesVSMap.votesVS = []
                votesVSMap.fieldsEventVS = []
				List<VoteVS> votesVS
				VoteVS.withTransaction { votesVS = VoteVS.findAllWhere(eventVS:eventVSElection) }
				def votesVSCancelled = VoteVS.findAllWhere(eventVS:eventVSElection, state:VoteVS.State.CANCELLED)
				def votesVSOk = VoteVS.findAllWhere(eventVS:eventVSElection, state:VoteVS.State.OK)
                votesVSMap.numVotesVS = votesVS.size()
				votesVSMap.numVotesVSOK = votesVSOk.size()
				votesVSMap.numVotesVSVotesVSCANCELLED = votesVSCancelled.size()
                votesVSMap.accessControlURL = eventVSElection.accessControlVS.serverURL
				votesVSMap.eventVSElectionURL = eventVSElection.url
                HexBinaryAdapter hexConverter = new HexBinaryAdapter();
                votesVS.each {voteVS ->
                    String hashCertVoteHex = hexConverter.marshal(voteVS.getCertificateVS().hashCertVSBase64.getBytes());
                    def voteVSMap = [id:voteVS.id, hashCertVSBase64:voteVS.getCertificateVS().hashCertVSBase64,
                        accessControlFieldEventVSId:voteVS.getFieldEventVS().accessControlFieldEventId,
                        eventVSElectionId:voteVS.eventVS.getFieldsEventVS(), state:voteVS.state,
						certificateURL:"${grailsApplication.config.grails.serverURL}/certificateVS/voteVS/hashHex/${hashCertVoteHex}",
                        voteVSSMIMEURL:"${grailsApplication.config.grails.serverURL}/messageSMIME/${voteVS.messageSMIME.id}"]
					if(VoteVS.State.CANCELLED == voteVS.state) {
						VoteVSCanceller voteVSCanceller
						VoteVSCanceller.withTransaction { voteVSCanceller = VoteVSCanceller.findWhere(voteVS:voteVS) }
						voteVSMap.cancellerURL =
                                "${grailsApplication.config.grails.serverURL}/messageSMIME/${voteVSCanceller?.messageSMIME?.id}"
					}
					votesVSMap.votesVS.add(voteVSMap)
                }
                eventVSElection.fieldsEventVS.each {fieldEventVS ->
					def numVotesVS = VoteVS.findAllWhere(optionSelected:fieldEventVS, state:VoteVS.State.OK).size()
                    def fieldEventVSMap = [fieldEventVSId:fieldEventVS.id,
                            content:fieldEventVS.content, numVotesVS:numVotesVS]
                    votesVSMap.fieldsEventVS.add(fieldEventVSMap)
                }
				render votesVSMap as JSON
                return
            }
            return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND, message(code: 'eventVSNotFoundByURL',
                    args:[params.eventAccessControlURL]))]
        }
        return [responseVS : new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrors', args:[]))]
    }

	/**
	 * Servicio que comprueba las fechas de una votación
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVS/$id/checkDates]
	 * @param [id] Obligatorio. El identificador de la votación en la base de datos.	
	 */
    def checkDates () {
		EventVS eventVSElection
		if (params.long('id')) {
            EventVS.withTransaction { eventVSElection = EventVS.get(params.id) }
		}
		if(!eventVSElection) {
            return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'eventVSNotFoundErrorMsg', args:[params.id]))]
		}
        return [responseVS : eventVSElectionService.checkEventVSDates(eventVSElection, request.getLocale())]
	}
	
	/**
	 * Servicio de cancelación de votaciones 
	 *
	 * @contentType [application/pkcs7-signature] Obligatorio. Archivo con los datos de la votación que se
	 * 			desea cancelar firmado por el Control de Acceso que publicó la votación y por el usuario que
	 *          la publicó o un administrador de sistema.
	 * @httpMethod [POST]
	 */
	def cancelled() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        return [responseVS : eventVSElectionService.cancelEvent(messageSMIME)]
	}
	
	
	/**
	 * Servicio que devuelve un archivo zip con los errores que se han producido
	 * en una votación
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVS/$id/votingErrors]
	 * @param [id] Obligatorio. Identificador del evento en la base de datos
	 * @return Archivo zip con los messages con errores
	 */
	def votingErrors() {
		EventVS event
		EventVS.withTransaction {
			event = EventVS.get(params.long('id'))
		}
		if (!event) {
            return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'eventVSNotFound', args:[params.id]))]
		}
		def errors
		MessageSMIME.withTransaction { errors = MessageSMIME.findAllWhere (type:TypeVS.VOTE_ERROR,  eventVS:event)}
		
		if(errors.size == 0){
            return [responseVS : new ResponseVS(ResponseVS.SC_OK,message(code: 'votingWithoutErrorsMsg',
                    args:[event.id, event.subject]))]
		} else {
			String datePathPart = DateUtils.getDateStr(event.getDateFinish(), "yyyy/MM/dd")
			String baseDirPath = "${grailsApplication.config.vs.errorsBaseDir}" +
				"/${datePathPart}/Event_${event.id}"
			errors.each { messageSMIME ->
				File errorFile = new File("${baseDirPath}/MessageSMIME_${messageSMIME.id}")
				errorFile.setBytes(messageSMIME.content)
			}
			File zipResult = new File("${baseDirPath}.zip")
			def ant = new AntBuilder()
			ant.zip(destfile: zipResult, basedir: "${baseDirPath}")
            return [responseVS : new ResponseVS(statusCode:ResponseVS.SC_OK, messageBytes:zipResult.getBytes(),
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
