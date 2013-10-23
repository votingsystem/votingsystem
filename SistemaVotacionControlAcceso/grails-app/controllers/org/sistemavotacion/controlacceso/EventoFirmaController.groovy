package org.sistemavotacion.controlacceso

import java.util.Map;
import org.sistemavotacion.controlacceso.modelo.*
import grails.converters.JSON
import org.sistemavotacion.util.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

/**
 * @infoController Manifiestos
 * @descController Servicios relacionados con la publicación de manifiestos.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class EventoFirmaController {

	def eventoFirmaService
	def pdfRenderingService
	def eventoService
	def htmlService
	
	
	/**
	 * @httpMethod [GET]
	 * @return La página principal de la aplicación web de manifiestos.
	 */
	def mainPage() {
		render(view:"mainPage" , model:[selectedSubsystem:Subsystem.MANIFESTS.toString()])
	}
	
	/**
	 * @httpMethod [GET]
     * @serviceURL [/eventoFirma/$id]	 
	 * @param [id] Opcional. El identificador del manifiesto en la base de datos.
	 * @responseContentType [application/json]
	 * @return Documento JSON con información del manifiesto solicitado.
	 */
	def index() { 
		if(request.contentType?.contains("application/pdf")) {
			forward action: "obtenerPDF"
			return false
		}
		if(params.long('id')) {
			EventoFirma evento
			EventoFirma.withTransaction {
				evento = EventoFirma.get(params.long('id'))
			}
			if(!(evento.estado == Evento.Estado.ACTIVO || evento.estado == Evento.Estado.PENDIENTE_COMIENZO ||
				evento.estado == Evento.Estado.CANCELADO || evento.estado == Evento.Estado.FINALIZADO)) evento = null
			if(!evento) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code:'eventNotFound', args:["${params.id}"])
				return false
			}
			if(request.contentType?.contains("application/json")) {
				render eventoService.optenerEventoMap(evento) as JSON
				return false
			} else {
				render(view:"eventoFirma", model: [
					selectedSubsystem:Subsystem.MANIFESTS.toString(), 
					eventMap:eventoService.optenerEventoMap(evento)])
				return
			}
		}
		flash.forwarded = true
		forward action: "obtenerManifiestos"
		return false
	}
	
	/**
	 * Servicio que devuelve el PDF que se tienen que firmar para publicar un 
	 * manifiesto.
	 * 
	 * @httpMethod [GET]
     * @serviceURL [/eventoFirma/$id]	
	 * @responseContentType [application/pdf] 
	 * @param [id] Obligatorio. El identificador del manifiesto en la base de datos.
	 * @return El manifiesto en formato PDF.
	 */
	def obtenerPDF () {
		log.debug("obtenerPDF - ${params.id}")
		if (params.long('id')) {
			EventoFirma evento
			Evento.withTransaction{
				evento = EventoFirma.get(params.id)
			}
			if(!evento) {
				render message(code:'eventNotFound', args:["${params.id}"])
				return false
			}
			response.setHeader("Content-disposition", "attachment; filename=manifiesto.pdf")
			response.contentType = "application/pdf"
			if(evento.pdf)
				response.outputStream << evento.pdf // Performing a binary stream copy
			else {
				ByteArrayOutputStream bytes = pdfRenderingService.render(
					template: "/eventoFirma/pdf", model:[evento:evento])
				Evento.withTransaction{
					evento.pdf = bytes.toByteArray()
					evento.save()
					log.debug "Generado PDF de evento ${evento.id}"
				}
				response.outputStream << evento.pdf // Performing a binary stream copy
			}
			response.outputStream.flush()
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}

	
	def save() {
		if(request.contentType?.contains("application/pdf")) {
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
	 * se envía a '/eventoFirma/publicarPDF' el manifiesto en formato HTML, el servidor 
	 * lo valida y si todo es correcto genera el PDF y envía al programa cliente el identificador 
	 * del manifiesto en la base de datos. El programa cliente puede descargarse con ese
	 * identificador el PDF firmarlo y enviarlo a este servicio.
	 * 
	 * @httpMethod [POST]
     * @serviceURL [/eventoFirma/$id]
     * @requestContentType [application/pdf,application/x-pkcs7-signature] Obligatorio. El archivo PDF con 
     * 				el manifiesto que se desea publicar firmado por el autor.
	 * @param [id] Obligatorio. El identificador en la base de datos del manifiesto. 
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
	def validarPDF() {
		Documento documento = params.pdfDocument
		if (params.long('id') && documento &&
			documento.estado == Documento.Estado.VALIDADO) {
			EventoFirma evento = null;
			EventoFirma.withTransaction{
				evento = EventoFirma.get(params.id)
			}			 
			if(!evento) {
				response.status = Respuesta.SC_ERROR_PETICION;
				render message(code:'eventNotFound', args:["${params.id}"]) 
				return false
			}
			if(evento.estado != Evento.Estado.PENDIENTE_DE_FIRMA) {
				response.status = Respuesta.SC_ERROR_PETICION;
				render message(code:'manifestNotPending', args:["${params.id}"])
				return false
			}
			try {
				Respuesta respuesta = eventoFirmaService.saveManifest(documento,
					evento, request.getLocale())
				response.status = respuesta.codigoEstado
				render respuesta.mensaje
				return false
			} catch (Exception ex) {
				log.error (ex.getMessage(), ex)
				response.status = Respuesta.SC_ERROR_PETICION
				render(ex.getMessage())
				return false
			}
		} else log.error ("Missing Manifest tu publish id")
			
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * @httpMethod [POST]
     * @serviceURL [/eventoFirma]	 
	 * @param [htmlManifest] Manifiesto que se desea publicar en formato HTML.
	 * @responseHeader [eventId] Identificador en la base de datos del evento que se desea publicar
	 * @responseContentType [application/pdf] 
	 * @return Si todo va bien devuelve un código de estado HTTP 200 con el identificador
	 * del nuevo manifiesto en la base de datos en el cuerpo del mensaje.
	 */
	def publicarPDF () {
		try {
			String eventoStr = "${request.getInputStream()}"
			if (!eventoStr) {
				response.status = Respuesta.SC_ERROR_PETICION
				render message(code: 'error.PeticionIncorrectaHTML', args:[
					"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
				return false
			} else {
				def eventoJSON = JSON.parse(eventoStr)
				log.debug "eventoJSON.contenido: ${eventoJSON.contenido}"
				eventoJSON.contenido = htmlService.prepareHTMLToPDF(eventoJSON.contenido.getBytes())
				Date fechaFin = new Date().parse("yyyy/MM/dd HH:mm:ss", eventoJSON.fechaFin)
				if(fechaFin.before(DateUtils.getTodayDate())) {
					String msg = message(code:'publishDocumentDateErrorMsg', 
						args:[DateUtils.getStringFromDate(fechaFin)]) 
					log.error("DATE ERROR - msg: ${msg}")
					response.status = Respuesta.SC_ERROR
					render msg
					return false
				}
				EventoFirma evento = new EventoFirma(asunto:eventoJSON.asunto,
					fechaInicio:DateUtils.todayDate,
					estado: Evento.Estado.PENDIENTE_DE_FIRMA,
					contenido:eventoJSON.contenido,
					fechaFin:fechaFin)
				evento.save()
				ByteArrayOutputStream pdfByteStream = pdfRenderingService.render(
						template: "/eventoFirma/pdf", model:[evento:evento])
				/*Evento.withTransaction{
					evento.pdf = bytes.toByteArray()
					evento.save()
					log.debug "Generado PDF de evento ${evento.id}"
				}*/
				log.debug "Saved event ${evento.id}"
				response.setHeader('eventId', "${evento.id}")
				response.contentType = "application/pdf"
				response.outputStream << pdfByteStream.toByteArray() // Performing a binary stream copy
				return false
			}
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR
			render message(code:'publishManifestErrorMessage')
			return false 
		}
	}
	
	/**
	 * @httpMethod [GET]
	 * @param [id] el identificador del manifiesto en la base de datos.
	 * @return El manifiesto en formato HTML.
	 */
	def obtenerHtml () {
		if (params.long('id')) {
			EventoFirma evento = EventoFirma.get(params.id)
			if(!evento) {
				render message(code:'eventNotFound', args:["${params.id}"]) 
				return false
			}
			render evento.contenido
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * @httpMethod [GET]
     * @serviceURL [/eventoFirma]	 
	 * @param [max] Opcional (por defecto 20). Número máximo de documentos que 
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param [estadoEvento] Opcional, posibles valores 'ACTIVO','CANCELADO', 'FINALIZADO', 'PENDIENTE_COMIENZO'.
	 * 		               El estado de los eventos que se desea consultar.
	 * @param [offset] Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param [order] Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de inicio.
	 * @responseContentType [application/json]
	 * @return Documento JSON con los manifiestos que cumplen con el criterio de búsqueda.
	 */
	def obtenerManifiestos () {
		def eventoList = []
		def consultaMap = new HashMap()
		consultaMap.eventos = new HashMap()
		def firmas = []
		if (params.long('id')) {
			EventoFirma evento = null
			EventoFirma.withTransaction {
				evento = EventoFirma.get(params.long('id'))
			}
			if(!evento) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'eventNotFound', args:[params.id])
				return false
			} else {
				render eventoService.optenerEventoMap(evento) as JSON
				return false
			}
	   } else {
		   params.sort = "fechaInicio"
		   //params.order="dwefeasc"
		   log.debug " -Params: " + params
		   Evento.Estado estadoEvento
		   try {
			   if(params.estadoEvento) estadoEvento = Evento.Estado.valueOf(params.estadoEvento)
		   } catch(Exception ex) {
		   		log.error(ex.getMessage(), ex)
				response.status = Respuesta.SC_ERROR_PETICION
				render message(code: 'paramValueERRORMsg', args:[
					params.estadoEvento, "${Evento.Estado.values()}"])
				return 
		   }
		   EventoFirma.withTransaction {
			   if(estadoEvento) {
				   if(estadoEvento == Evento.Estado.FINALIZADO) {
					   eventoList =  EventoFirma.findAllByEstadoOrEstado(
						   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, params)
					   consultaMap.numeroTotalEventosFirmaEnSistema = EventoFirma.countByEstadoOrEstado(
						   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO)
				   } else {
					   eventoList =  EventoFirma.findAllByEstado(estadoEvento, params)
					   consultaMap.numeroTotalEventosFirmaEnSistema = EventoFirma.countByEstado(estadoEvento)
				   }
			   } else {
				   eventoList =  EventoFirma.findAllByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
					   Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO, params)
				   consultaMap.numeroTotalEventosFirmaEnSistema = 
				   		EventoFirma.countByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
					    Evento.Estado.CANCELADO, Evento.Estado.FINALIZADO, Evento.Estado.PENDIENTE_COMIENZO)
			   }
		   }
            consultaMap.offset = params.long('offset')
	   }
	   consultaMap.numeroEventosFirmaEnPeticion = eventoList.size()
	   eventoList.each {eventoItem ->
		   firmas.add(eventoService.optenerEventoFirmaMap(eventoItem))
	   }
	   consultaMap.eventos.firmas = firmas
	   response.setContentType("application/json")
	   render consultaMap as JSON
	}
	
	
	/**
	 * Servicio que proporciona acceso a lo documentos PDF firmados por los usuarios 
	 * enviados para publicar manifiestos.
	 *
	 * @httpMethod [GET]
     * @serviceURL [/eventoFirma/firmado/$id]	
	 * @param [id] El identificador del manifiesto en la base de datos.
	 * @return El manifiesto en formato PDF.
	 */
	def firmado () {
		EventoFirma evento
		EventoFirma.withTransaction {
			evento = EventoFirma.get(params.long('id'))
		}
		if(!evento) {
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'eventNotFound', args:[params.id])
			return false
		}
		Documento documento
		Documento.withTransaction {
			documento = Documento.findWhere(evento:evento, estado:Documento.Estado.MANIFIESTO_VALIDADO)
		}
		if(!documento) {
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'validatedManifestNotFoundErrorMsg', args:[params.id])
			return false
		}
		//response.setHeader("Content-disposition", "attachment; filename=manifiesto.pdf")
		response.contentType = "application/pdf"
		response.setHeader("Content-Length", "${documento.pdf.length}")
		response.outputStream << documento.pdf // Performing a binary stream copy
		response.outputStream.flush()
	}
	
	/**
	 * Servicio que devuelve información sobre la actividad de una acción de recogida de firmas
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/eventoFirma/$id/informacionFirmas]
	 * @param [id] Obligatorio. El identificador del manifiesto en la base de datos.
	 * @responseContentType [application/json]
	 * @return Documento JSON con información sobre las firmas recibidas por el manifiesto solicitado.
	 */
	def informacionFirmas () {
		EventoFirma evento
		EventoFirma.withTransaction {
			evento = EventoFirma.get(params.id)
		}
		if (!evento) {
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'eventNotFound', args:[params.id])
			return false
		}
		def informacionFirmasMap = new HashMap()
		def firmas
		Documento.withTransaction {
			firmas = Documento.findAllWhere(evento:evento,
				estado:Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
		}
		informacionFirmasMap.numeroFirmas = firmas.size()
		informacionFirmasMap.asuntoEvento = evento.asunto
		informacionFirmasMap.eventoURL =
			"${grailsApplication.config.grails.serverURL}/evento/${evento.id}"
		informacionFirmasMap.firmas = []
		firmas.each { firma ->
			def firmaMap = [id:firma.id, fechaCreacion:firma.dateCreated,
			usuario:firma.usuario.nif,
			firmaURL:"${grailsApplication.config.grails.serverURL}/documento" +
				"/obtenerFirmaManifiesto?id=${firma.id}"]
			informacionFirmasMap.firmas.add(firmaMap)
		}
		response.status = Respuesta.SC_OK
		render informacionFirmasMap as JSON
	}
	
	/**
	 * Servicio que devuelve estadísticas asociadas a un manifiesto.
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/eventoFirma/$id/estadisticas]
	 * @param [id] Identificador en la base de datos del manifiesto que se desea consultar.
	 * @responseContentType [application/json]
	 * @return Documento JSON con las estadísticas asociadas al manifiesto solicitado.
	 */
	def estadisticas () {
		if (params.long('id')) {
			EventoFirma eventoFirma
			if (!params.evento) { 
				EventoFirma.withTransaction {
					eventoFirma = EventoFirma.get(params.id)
				}
			} 
			else eventoFirma = params.evento //forwarded from /evento/estadisticas
			if (eventoFirma) {
				def statisticsMap = eventoService.optenerEventoFirmaMap(eventoFirma)
				statisticsMap.numeroFirmas = Documento.countByEventoAndEstado(
					eventoFirma, Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
				statisticsMap.informacionFirmasURL = "${grailsApplication.config.grails.serverURL}" +
					"/evento/informacionFirmas?id=${eventoFirma.id}"
				statisticsMap.URL = "${grailsApplication.config.grails.serverURL}" + 
					"/evento/${eventoFirma.id}"
				if(request.contentType?.contains("application/json")) {
					if (params.callback) render "${params.callback}(${statisticsMap as JSON})"
					else render statisticsMap as JSON
					return false
				} else {
					render(view:"statistics", model: [statisticsMap:statisticsMap])
					return
				}
			}
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'eventNotFound', args:[params.id])
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}

}