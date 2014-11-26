package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.*
import org.votingsystem.util.DateUtils

/**
 * @infoController Manifiestos
 * @descController Servicios relacionados con la publicación de manifiestos.
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class EventVSManifestController {

	def eventVSManifestService
	def pdfRenderingService
	def eventVSService
	def htmlService
	
	/**
	 * @httpMethod [GET]
     * @serviceURL [/eventVSManifest/$id]
	 * @param [id] Opcional. El identificador del manifiesto en la base de datos.
	 * @responseContentType [application/json]
	 * @return documento JSON con información del manifiesto solicitado.
	 */
	def index() {
        def resultList
		if(request.contentType?.contains(ContentTypeVS.PDF.getName())) getPDF();
		else {
            if(params.long('id')) {
                EventVSManifest eventVS = null
                EventVSManifest.withTransaction {
                    resultList = EventVSManifest.createCriteria().list {
                        or {
                            eq("state", EventVS.State.ACTIVE)
                            eq("state", EventVS.State.PENDING)
                            eq("state", EventVS.State.CANCELLED)
                            eq("state", EventVS.State.TERMINATED)
                        }
                        and { eq("id", params.long('id'))}
                    }
                }
                if(!resultList.isEmpty()) eventVS = resultList.iterator().next()
                if(eventVS) {
                    eventVS = eventVSService.checkEventVSDates(eventVS).eventVS
                    if(request.contentType?.contains(ContentTypeVS.JSON.getName())) {
                        return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.JSON,
                                data:eventVSService.getEventVSMap(eventVS))]
                    } else {
                        render(view:"eventVSManifest", model: [eventMap:eventVSService.getEventVSMap(eventVS)])
                    }
                } else {
                    return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                            message(code:'eventVSNotFound', args:["${params.id}"]))]
                }
            } else getManifests()
        }
	}

    def editor() {
        render(view:"editor")
    }

	/**
	 * Servicio que devuelve el PDF que se tienen que firmar para publicar un 
	 * manifiesto.
	 * 
	 * @httpMethod [GET]
     * @serviceURL [/eventVSManifest/$id]
	 * @responseContentType [application/pdf] 
	 * @param [id] Obligatorio. El identificador del manifiesto en la base de datos.
	 * @return El manifiesto en formato PDF.
	 */
	def getPDF () {
		log.debug("getPDF - ${params.id}")
		if (params.long('id')) {
			EventVSManifest eventVS
			EventVS.withTransaction{ eventVS = EventVSManifest.get(params.id) }
			if(!eventVS) {
                return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                        message(code:'eventVSNotFound', args:["${params.id}"]))]
			} else {
                if(!eventVS.pdf) {
                    ByteArrayOutputStream bytes = pdfRenderingService.render(
                            template: "/eventVSManifest/pdf", model:[eventVS:eventVS])
                    EventVS.withTransaction{
                        eventVS.pdf = bytes.toByteArray()
                        eventVS.save()
                    }
                }
                return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.PDF,
                        messageBytes: eventVS.pdf, message:"manifest_${params.id}.pdf")]
            }
		} else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrors', args:[]))]
	}

	
	def save() {
		if(request.contentType?.contains(ContentTypeVS.PDF.getName())) validatePDF()
		else publishPDF()
	}
	
	/**
	 * Servicio que valida los manifiestos que se desean publicar. <br/>
	 * La publicación de manifiestos se produce en dos fases. En la primera
	 * se envía a '/eventVSManifest/publishPDF' el manifiesto en formato HTML, el servidor
	 * lo valida y si todo es correcto genera el PDF y envía al programa cliente el identificador 
	 * del manifiesto en la base de datos. El programa cliente puede descargarse con ese
	 * identificador el PDF firmarlo y enviarlo a este servicio.
	 * 
	 * @httpMethod [POST]
     * @serviceURL [/eventVSManifest/$id]
     * @requestContentType [application/pdf,application/pkcs7-signature] Obligatorio. El archivo PDF con
     * 				el manifiesto que se desea publicar firmado por el autor.
	 * @param [id] Obligatorio. El identificador en la base de datos del manifiesto. 
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
	def validatePDF() {
		PDFDocumentVS pdfDocument = request.pdfDocument
		if (params.long('id') && pdfDocument && pdfDocument.state == PDFDocumentVS.State.VALIDATED) {
			EventVSManifest eventVS = null;
			EventVSManifest.withTransaction{ eventVS = EventVSManifest.get(params.id) }
			if(!eventVS) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                        message(code:'eventVSNotFound', args:["${params.id}"]))]
			} else {
                if(eventVS.state != EventVS.State.PENDING_SIGNATURE) {
                    return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                            message(code:'manifestNotPending', args:["${params.id}"]))]
                } else {
                    return [responseVS:eventVSManifestService.saveManifest(pdfDocument, eventVS)]
                }
            }
		} else {
            return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                    contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrors', args:[]))]
        }
	}
	
	/**
	 * @httpMethod [POST]
     * @serviceURL [/eventVSManifest]
	 * @param [htmlManifest] Manifiesto que se desea publicar en formato HTML.
	 * @responseHeader [eventId] Identificador en la base de datos del eventVS que se desea publicar
	 * @responseContentType [application/pdf] 
	 * @return Si todo va bien devuelve un código de estado HTTP 200 con el identificador
	 * del nuevo manifiesto en la base de datos en el cuerpo del message.
	 */
	def publishPDF () {
        String eventVSStr = "${request.getInputStream()}"
        if (!eventVSStr) {
            return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                    contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrors', args:[]))]
        } else {
            def eventVSJSON = JSON.parse(eventVSStr)
            log.debug "eventVSJSON.content: ${eventVSJSON.content}"
            eventVSJSON.content = htmlService.prepareHTMLToPDF(eventVSJSON.content.getBytes())
            Date dateFinish = new Date().parse("yyyy/MM/dd HH:mm:ss", eventVSJSON.dateFinish)
            if(dateFinish.before(Calendar.getInstance().getTime())) {
                String msg = message(code:'publishDocumentDateErrorMsg',
                        args:[DateUtils.getDateStr(dateFinish)])
                log.error("DATE ERROR - msg: ${msg}")
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR, msg)]
            }
            EventVSManifest eventVS = new EventVSManifest(subject:eventVSJSON.subject,
                    dateBegin:Calendar.getInstance().getTime(), state: EventVS.State.PENDING_SIGNATURE,
                    content:eventVSJSON.content, dateFinish:dateFinish)
            eventVS.save()
            ByteArrayOutputStream pdfByteStream = pdfRenderingService.render(
                    template: "/eventVSManifest/pdf", model:[eventVS:eventVS])
            /*EventVS.withTransaction{
                eventVS.pdf = bytes.toByteArray()
                eventVS.save()
                log.debug "Generado PDF de eventVS ${eventVS.id}"
            }*/
            log.debug "Saved event ${eventVS.id}"
            response.setHeader('eventId', "${eventVS.id}")
            return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PDF,
                messageBytes: pdfByteStream.toByteArray())]
        }
	}
	
	/**
	 * @httpMethod [GET]
	 * @param [id] el identificador del manifiesto en la base de datos.
	 * @return El manifiesto en formato HTML.
	 */
	def getHtml () {
		if (params.long('id')) {
			EventVSManifest eventVS = EventVSManifest.get(params.id)
			if(eventVS) return [responseVS:new ResponseVS(ResponseVS.SC_OK, eventVS.content)]
            else return [responseVS:new ResponseVS(ResponseVS.SC_OK,
                    message(code:'eventVSNotFound', args:["${params.id}"]))]
		} else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrors', args:[]))]
	}
	
	/**
	 * @httpMethod [GET]
     * @serviceURL [/eventVSManifest]
	 * @param [max] Opcional (por defecto 20). Número máximo de documentos que 
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [eventVSState] Opcional, posibles valores 'ACTIVE','CANCELLED', 'TERMINATED', 'PENDING'.
	 * 		               El estado de los eventos que se desea consultar.
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param [order] Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de inicio.
	 * @responseContentType [application/json]
	 * @return PDFDocumentVS JSON con los manifiestos que cumplen con el criterio de búsqueda.
	 */
	def getManifests () {
        if (params.long('id')) {
            def resultList
            EventVSManifest.withTransaction {
                resultList = EventVSManifest.createCriteria().list {
                    or {
                        eq("state", EventVS.State.ACTIVE)
                        eq("state", EventVS.State.PENDING)
                        eq("state", EventVS.State.CANCELLED)
                        eq("state", EventVS.State.TERMINATED)
                    }
                    and { eq("id", params.long('id')) }
                }
            }
            if (resultList.isEmpty()) {
                return [responseVS: new ResponseVS(ResponseVS.SC_NOT_FOUND,
                        message(code: 'eventVSNotFound', args: [params.id]))]
            } else {
                EventVSManifest eventVS = resultList.iterator().next()
                eventVS = eventVSService.checkEventVSDates(eventVS).eventVS
                render eventVSService.getEventVSMap(eventVS) as JSON
            }
        } else if(request.contentType?.contains("json")) {
            def resultList
            def eventsVSMap = new HashMap()
            eventsVSMap.eventVS = []
            params.sort = "dateBegin"
            EventVS.State eventVSState = null
            try {
                eventVSState = EventVS.State.valueOf(params.eventVSState)
            } catch (Exception ex) {
            }
            EventVSManifest.withTransaction {
                EventVSManifest.withTransaction {
                    resultList = EventVSManifest.createCriteria().list(max: params.max, offset: params.offset,
                            sort: params.sort, order: params.order) {
                        if (eventVSState == EventVS.State.TERMINATED) {
                            or {
                                eq("state", EventVS.State.TERMINATED)
                                eq("state", EventVS.State.CANCELLED)
                            }
                        } else if (eventVSState) {
                            eq("state", eventVSState)
                        } else {
                            or {
                                eq("state", EventVS.State.ACTIVE)
                                eq("state", EventVS.State.PENDING)
                                eq("state", EventVS.State.TERMINATED)
                                eq("state", EventVS.State.CANCELLED)
                            }
                        }
                    }
                    eventsVSMap.totalCount = resultList.totalCount
                    eventsVSMap.offset = params.long('offset')
                }
            }
            resultList.each { eventVSItem ->
                eventVSItem = eventVSService.checkEventVSDates(eventVSItem).eventVS
                eventsVSMap.eventVS.add(eventVSService.getEventVSManifestMap(eventVSItem)) }
            render eventsVSMap as JSON
        } else render(view:"index")
    }
	
	
	/**
	 * Servicio que proporciona acceso a lo documentos PDF firmados por los usersVS
	 * enviados para publicar manifiestos.
	 *
	 * @httpMethod [GET]
     * @serviceURL [/eventVSManifest/signed/$id]
	 * @param [id] El identificador del manifiesto en la base de datos.
	 * @return El manifiesto en formato PDF.
	 */
	def signed () {
		EventVSManifest eventVS
		EventVSManifest.withTransaction { eventVS = EventVSManifest.get(params.long('id')) }
		if(!eventVS) {
            return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'eventVSNotFound', args:[params.id]))]
		} else {
            PDFDocumentVS pdfDocument
            PDFDocumentVS.withTransaction {
                pdfDocument = PDFDocumentVS.findWhere(eventVS:eventVS, state:PDFDocumentVS.State.VALIDATED_MANIFEST)
            }
            if(pdfDocument) {
                return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.PDF,
                       message:"manifest_${eventVS.id}.pdf", messageBytes: pdfDocument.pdf)]
            } else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'validatedManifestNotFoundErrorMsg', args:[params.id]))]
        }
	}
	
	/**
	 * Servicio que devuelve información sobre la actividad de una acción de recogida de firmas
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSManifest/$id/signaturesInfo]
	 * @param [id] Obligatorio. El identificador del manifiesto en la base de datos.
	 * @responseContentType [application/json]
	 * @return documento JSON con información sobre las firmas recibidas por el manifiesto solicitado.
	 */
	def signaturesInfo () {
		EventVSManifest eventVS
		EventVSManifest.withTransaction {
			eventVS = EventVSManifest.get(params.id)
		}
		if (!eventVS) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'eventVSNotFound', args:[params.id])
			return false
		}
		def signaturesInfoMap = new HashMap()
		def signatures
		PDFDocumentVS.withTransaction {
			signatures = PDFDocumentVS.findAllWhere(eventVS:eventVS,
				state:PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED)
		}
		signaturesInfoMap.numSignatures = signatures.size()
		signaturesInfoMap.eventVSSubject = eventVS.subject
		signaturesInfoMap.eventURL =
			"${grailsApplication.config.grails.serverURL}/eventVS/${eventVS.id}"
		signaturesInfoMap.signatures = []
		signatures.each { signature ->
			def signatureMap = [id:signature.id, dateCreated:signature.dateCreated, userVS:signature.userVS.nif,
			signatureURL:"${grailsApplication.config.grails.serverURL}/PDFDocumentVS" +
				"/getSignedManifest?id=${signature.id}"]
			signaturesInfoMap.signatures.add(signatureMap)
		}
		render signaturesInfoMap as JSON
	}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}