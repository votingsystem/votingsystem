package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.PDFDocumentVS
import org.votingsystem.model.SubSystemVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSManifest
import org.votingsystem.model.ResponseVS
import org.votingsystem.util.DateUtils
/**
 * @infoController Manifiestos
 * @descController Servicios relacionados con la publicación de manifiestos.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class EventVSManifestController {

	def eventVSManifestService
	def pdfRenderingService
	def eventVSService
	def htmlService
	
	
	/**
	 * @httpMethod [GET]
	 * @return La página principal de la aplicación web de manifiestos.
	 */
	def mainPage() {
		render(view:"mainPage" , model:[selectedSubsystem:SubSystemVS.MANIFESTS.toString()])
	}
	
	/**
	 * @httpMethod [GET]
     * @serviceURL [/eventVSManifest/$id]
	 * @param [id] Opcional. El identificador del manifiesto en la base de datos.
	 * @responseContentType [application/json]
	 * @return PDFDocumentVS JSON con información del manifiesto solicitado.
	 */
	def index() { 
		if(request.contentType?.contains(ContentTypeVS.PDF)) {
			forward action: "getPDF"
			return false
		}
		if(params.long('id')) {
			EventVSManifest eventVS
			EventVSManifest.withTransaction {
				eventVS = EventVSManifest.get(params.long('id'))
			}
			if(!(eventVS.state == EventVS.State.ACTIVE || eventVS.state == EventVS.State.AWAITING ||
				eventVS.state == EventVS.State.CANCELLED || eventVS.state == EventVS.State.TERMINATED)) eventVS = null
			if(!eventVS) {
				response.status = ResponseVS.SC_NOT_FOUND
				render message(code:'eventVSNotFound', args:["${params.id}"])
				return false
			}
			if(request.contentType?.contains(ContentTypeVS.JSON)) {
				render eventVSService.getEventVSMap(eventVS) as JSON
				return false
			} else {
				render(view:"eventVSManifest", model: [
					selectedSubsystem:SubSystemVS.MANIFESTS.toString(),
					eventMap:eventVSService.getEventVSMap(eventVS)])
				return
			}
		}
		flash.forwarded = true
		forward action: "getManifests"
		return false
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
				render message(code:'eventVSNotFound', args:["${params.id}"])
				return false
			}
			response.setHeader("Content-disposition", "attachment; filename=manifest.pdf")
			response.contentType = ContentTypeVS.PDF
			if(eventVS.pdf)
				response.outputStream << eventVS.pdf // Performing a binary stream copy
			else {
				ByteArrayOutputStream bytes = pdfRenderingService.render(
					template: "/eventVSManifest/pdf", model:[eventVS:eventVS])
				EventVS.withTransaction{
					eventVS.pdf = bytes.toByteArray()
					eventVS.save()
					log.debug "Generado PDF de eventVS ${eventVS.id}"
				}
				response.outputStream << eventVS.pdf // Performing a binary stream copy
			}
			response.outputStream.flush()
			return false
		}
		response.status = ResponseVS.SC_ERROR_REQUEST
		render message(code: 'requestWithErrorsHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}

	
	def save() {
		if(request.contentType?.contains(ContentTypeVS.PDF)) {
			flash.forwarded = Boolean.TRUE
			forward action: "validarPDF"
			return false
		} else {
			forward action: "publicarPDF"
			return false
		}
	}
	
	/**
	 * Servicio que valida los manifiestos que se desean publicar. <br/>
	 * La publicación de manifiestos se produce en dos fases. En la primera
	 * se envía a '/eventVSManifest/publicarPDF' el manifiesto en formato HTML, el servidor
	 * lo valida y si todo es correcto genera el PDF y envía al programa cliente el identificador 
	 * del manifiesto en la base de datos. El programa cliente puede descargarse con ese
	 * identificador el PDF firmarlo y enviarlo a este servicio.
	 * 
	 * @httpMethod [POST]
     * @serviceURL [/eventVSManifest/$id]
     * @requestContentType [application/pdf,application/x-pkcs7-signature] Obligatorio. El archivo PDF con 
     * 				el manifiesto que se desea publicar firmado por el autor.
	 * @param [id] Obligatorio. El identificador en la base de datos del manifiesto. 
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
	def validarPDF() {
		PDFDocumentVS documento = params.pdfDocument
		if (params.long('id') && documento &&
			documento.state == PDFDocumentVS.State.VALIDATED) {
			EventVSManifest eventVS = null;
			EventVSManifest.withTransaction{
				eventVS = EventVSManifest.get(params.id)
			}			 
			if(!eventVS) {
				response.status = ResponseVS.SC_ERROR_REQUEST;
				render message(code:'eventVSNotFound', args:["${params.id}"])
				return false
			}
			if(eventVS.state != EventVS.State.PENDING_SIGNATURE) {
				response.status = ResponseVS.SC_ERROR_REQUEST;
				render message(code:'manifestNotPending', args:["${params.id}"])
				return false
			}
			try {
				ResponseVS responseVS = eventVSManifestService.saveManifest(documento,
					eventVS, request.getLocale())
				response.status = responseVS.statusCode
				render responseVS.message
				return false
			} catch (Exception ex) {
				log.error (ex.getMessage(), ex)
				response.status = ResponseVS.SC_ERROR_REQUEST
				render(ex.getMessage())
				return false
			}
		} else log.error ("Missing Manifest tu publish id")
			
		response.status = ResponseVS.SC_ERROR_REQUEST
		render message(code: 'requestWithErrorsHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
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
	def publicarPDF () {
		try {
			String eventVSStr = "${request.getInputStream()}"
			if (!eventVSStr) {
				response.status = ResponseVS.SC_ERROR_REQUEST
				render message(code: 'requestWithErrorsHTML', args:[
					"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
				return false
			} else {
				def eventVSJSON = JSON.parse(eventVSStr)
				log.debug "eventVSJSON.content: ${eventVSJSON.content}"
				eventVSJSON.content = htmlService.prepareHTMLToPDF(eventVSJSON.content.getBytes())
				Date dateFinish = new Date().parse("yyyy/MM/dd HH:mm:ss", eventVSJSON.dateFinish)
				if(dateFinish.before(DateUtils.getTodayDate())) {
					String msg = message(code:'publishDocumentDateErrorMsg', 
						args:[DateUtils.getStringFromDate(dateFinish)])
					log.error("DATE ERROR - msg: ${msg}")
					response.status = ResponseVS.SC_ERROR
					render msg
					return false
				}
				EventVSManifest eventVS = new EventVSManifest(subject:eventVSJSON.subject,
					dateBegin:DateUtils.todayDate, state: EventVS.State.PENDING_SIGNATURE,
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
				response.contentType = ContentTypeVS.PDF
				response.outputStream << pdfByteStream.toByteArray() // Performing a binary stream copy
				return false
			}
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = ResponseVS.SC_ERROR
			render message(code:'publishManifestErrorMessage')
			return false 
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
			if(!eventVS) {
				render message(code:'eventVSNotFound', args:["${params.id}"])
				return false
			}
			render eventVS.content
			return false
		}
		response.status = ResponseVS.SC_ERROR_REQUEST
		render message(code: 'requestWithErrorsHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * @httpMethod [GET]
     * @serviceURL [/eventVSManifest]
	 * @param [max] Opcional (por defecto 20). Número máximo de documentos que 
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [eventVSState] Opcional, posibles valores 'ACTIVE','CANCELLED', 'TERMINATED', 'AWAITING'.
	 * 		               El estado de los eventos que se desea consultar.
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param [order] Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de inicio.
	 * @responseContentType [application/json]
	 * @return PDFDocumentVS JSON con los manifiestos que cumplen con el criterio de búsqueda.
	 */
	def getManifests () {
		def eventVSList = []
		def responseMap = new HashMap()
		responseMap.eventsVS = new HashMap()
		def manifests = []
		if (params.long('id')) {
			EventVSManifest eventVS = null
			EventVSManifest.withTransaction {
				eventVS = EventVSManifest.get(params.long('id'))
			}
			if(!eventVS) {
				response.status = ResponseVS.SC_NOT_FOUND
				render message(code: 'eventVSNotFound', args:[params.id])
				return false
			} else {
				render eventVSService.getEventVSMap(eventVS) as JSON
				return false
			}
	   } else {
		   params.sort = "dateBegin"
		   //params.order="dwefeasc"
		   log.debug " -Params: " + params
		   EventVS.State eventVSState
		   try {
			   if(params.eventVSState) eventVSState = EventVS.State.valueOf(params.eventVSState)
		   } catch(Exception ex) {
		   		log.error(ex.getMessage(), ex)
				response.status = ResponseVS.SC_ERROR_REQUEST
				render message(code: 'paramValueERRORMsg', args:[
					params.eventVSState, "${EventVS.State.values()}"])
				return 
		   }
		   EventVSManifest.withTransaction {
			   if(eventVSState) {
				   if(eventVSState == EventVS.State.TERMINATED) {
					   eventVSList =  EventVSManifest.findAllByStateOrState(
						   EventVS.State.CANCELLED, EventVS.State.TERMINATED, params)
					   responseMap.numEventsVSManifestInSystem = EventVSManifest.countByStateOrState(
						   EventVS.State.CANCELLED, EventVS.State.TERMINATED)
				   } else {
					   eventVSList =  EventVSManifest.findAllByState(eventVSState, params)
					   responseMap.numEventsVSManifestInSystem = EventVSManifest.countByState(eventVSState)
				   }
			   } else {
				   eventVSList =  EventVSManifest.findAllByStateOrStateOrStateOrState(EventVS.State.ACTIVE,
					   EventVS.State.CANCELLED, EventVS.State.TERMINATED, EventVS.State.AWAITING, params)
				   responseMap.numEventsVSManifestInSystem =
				   		EventVSManifest.countByStateOrStateOrStateOrState(EventVS.State.ACTIVE,
					    EventVS.State.CANCELLED, EventVS.State.TERMINATED, EventVS.State.AWAITING)
			   }
		   }
            responseMap.offset = params.long('offset')
	   }
	   responseMap.numEventsVSManifest = eventVSList.size()
	   eventVSList.each {eventVSItem ->
		   manifests.add(eventVSService.getEventVSManifestMap(eventVSItem))
	   }
	   responseMap.eventsVS.manifests = manifests
	   response.setContentType(ContentTypeVS.JSON)
	   render responseMap as JSON
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
		EventVSManifest.withTransaction {
			eventVS = EventVSManifest.get(params.long('id'))
		}
		if(!eventVS) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'eventVSNotFound', args:[params.id])
			return false
		}
		PDFDocumentVS documento
		PDFDocumentVS.withTransaction {
			documento = PDFDocumentVS.findWhere(eventVS:eventVS, state:PDFDocumentVS.State.VALIDATED_MANIFEST)
		}
		if(!documento) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'validatedManifestNotFoundErrorMsg', args:[params.id])
			return false
		}
		//response.setHeader("Content-disposition", "attachment; filename=manifiesto.pdf")
		response.contentType = ContentTypeVS.PDF
		response.setHeader("Content-Length", "${documento.pdf.length}")
		response.outputStream << documento.pdf // Performing a binary stream copy
		response.outputStream.flush()
	}
	
	/**
	 * Servicio que devuelve información sobre la actividad de una acción de recogida de firmas
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSManifest/$id/signaturesInfo]
	 * @param [id] Obligatorio. El identificador del manifiesto en la base de datos.
	 * @responseContentType [application/json]
	 * @return PDFDocumentVS JSON con información sobre las firmas recibidas por el manifiesto solicitado.
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
		response.status = ResponseVS.SC_OK
		render signaturesInfoMap as JSON
	}
	
	/**
	 * Servicio que devuelve estadísticas asociadas a un manifiesto.
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/eventVSManifest/$id/statistics]
	 * @param [id] Identificador en la base de datos del manifiesto que se desea consultar.
	 * @responseContentType [application/json]
	 * @return PDFDocumentVS JSON con las estadísticas asociadas al manifiesto solicitado.
	 */
	def statistics () {
		if (params.long('id')) {
			EventVSManifest eventVSManifest
			if (!params.eventVS) {
				EventVSManifest.withTransaction {
					eventVSManifest = EventVSManifest.get(params.id)
				}
			} 
			else eventVSManifest = params.eventVS //forwarded from /eventVS/statistics
			if (eventVSManifest) {
				def statisticsMap = eventVSService.getEventVSManifestMap(eventVSManifest)
				statisticsMap.numSignatures = PDFDocumentVS.countByEventVSAndState(
					eventVSManifest, PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED)
				statisticsMap.signaturesInfoURL = "${grailsApplication.config.grails.serverURL}" +
					"/eventVS/signaturesInfo?id=${eventVSManifest.id}"
				statisticsMap.URL = "${grailsApplication.config.grails.serverURL}" + 
					"/eventVS/${eventVSManifest.id}"
				if(request.contentType?.contains(ContentTypeVS.JSON)) {
					if (params.callback) render "${params.callback}(${statisticsMap as JSON})"
					else render statisticsMap as JSON
					return false
				} else {
					render(view:"statistics", model: [statisticsMap:statisticsMap])
					return
				}
			}
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'eventVSNotFound', args:[params.id])
			return false
		}
		response.status = ResponseVS.SC_ERROR_REQUEST
		render message(code: 'requestWithErrorsHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}

}