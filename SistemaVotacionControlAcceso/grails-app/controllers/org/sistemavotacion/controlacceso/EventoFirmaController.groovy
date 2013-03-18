package org.sistemavotacion.controlacceso

import java.util.Map;

import org.sistemavotacion.smime.SMIMEMessageWrapper;
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
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 */
class EventoFirmaController {

	def eventoFirmaService
	def pdfRenderingService
	def pdfService
	def eventoService
	def htmlService
	
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/eventoFirma'.
	 */
	def index() { }
	
	/**
	 * Servicio que valida los manifiestos que se desean publicar. <br/>
	 * La publicación de manifiestos se produce en dos fases. En la primera
	 * se envía a '/eventoFirma/publicarPDF' el manifiesto en formato HTML, el servidor 
	 * lo valida y si todo es correcto genera el PDF y envía al programa cliente el identificador 
	 * del manifiesto en la base de datos. El programa cliente puede descargarse con ese
	 * identificador el PDF firmarlo y enviarlo a este servicio.
	 * 
	 * @httpMethod POST
	 * @param signedPDF Obligatorio. PDF con el manifiesto que se desea publicar firmado
	 *        por el autor.
	 * @param id Obligatorio. El identificador en la base de datos del manifiesto. 
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
	def validarPDF() {
		if (params.long('id')) {
			EventoFirma evento = null;
			Evento.withTransaction{
				evento = EventoFirma.get(params.id)
			}			 
			if(!evento) {
				response.status = Respuesta.SC_ERROR_PETICION;
				render message(code:'eventNotFound', args:["${params.id}"]) 
				return false
			}
			try {
				String pdfFirmado = ((MultipartHttpServletRequest) request)?.getFileNames()?.next();
				log.debug "Recibido archivo: ${pdfFirmado}"
				MultipartFile multipartFile = ((MultipartHttpServletRequest) request)?.getFile(pdfFirmado);
				if (multipartFile?.getBytes() != null || params.archivoFirmado) {
					Respuesta respuesta = pdfService.validarFirma(multipartFile.getBytes(),
						evento, Documento.Estado.MANIFIESTO, request.getLocale())
					if (Respuesta.SC_OK != respuesta.codigoEstado) {
						log.debug "Problema en la recepción del archivo - ${respuesta.mensaje}"
					}
					response.status = respuesta.codigoEstado
					render respuesta.mensaje
					return false
				}
			} catch (Exception ex) {
				log.error (ex.getMessage(), ex)
				response.status = Respuesta.SC_ERROR_PETICION
				render(ex.getMessage())
				return false
			}
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * @httpMethod POST
	 * @param htmlManifest Manifiesto que se desea publicar en formato HTML.
	 * @return Si todo va bien devuelve un código de estado HTTP 200 con el identificador
	 * del nuevo manifiesto en la base de datos en el cuerpo del mensaje.
	 */
	def publicarPDF () {
		try {
			String eventoStr = StringUtils.getStringFromInputStream(request.getInputStream())
			log.debug "evento: ${eventoStr}"
			if (!eventoStr) {
				render(view:"index")
				return false
			} else {
				def eventoJSON = JSON.parse(eventoStr)
				log.debug "eventoJSON.contenido: ${eventoJSON.contenido}"
				eventoJSON.contenido = htmlService.prepareHTMLToPDF(eventoJSON.contenido.getBytes())
				EventoFirma evento = new EventoFirma(asunto:eventoJSON.asunto,
					fechaInicio:DateUtils.todayDate,
					estado: Evento.Estado.PENDIENTE_DE_FIRMA,
					contenido:eventoJSON.contenido,
					fechaFin:new Date().parse("yyyy-MM-dd HH:mm:ss", eventoJSON.fechaFin))
				evento.save()
				evento.url = "${grailsApplication.config.grails.serverURL}" +
					"${grailsApplication.config.SistemaVotacion.sufijoURLEventoFirma}${evento.id}"
				runAsync {
					ByteArrayOutputStream bytes = pdfRenderingService.render(
						template: "/eventoFirma/pdf", model:[evento:evento])
					Evento.withTransaction{
						evento.pdf = bytes.toByteArray()
						evento.save()
						log.debug "Generado PDF de evento ${evento.id}"
					}
				}
				log.debug "Salvado evento ${evento.id}"
				//render(view:"publicarPDF")
				render evento.id
				return false
			}
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR_EJECUCION
			render ex.getMessage()
			return false 
		}
	}
	
	/**
	 * @httpMethod GET
	 * @param id El identificador del manifiesto en la base de datos.
	 * @return El manifiesto en formato PDF.
	 */
	def obtenerPDF () {
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
	
	/**
	 * @httpMethod GET
	 * @param id el identificador del manifiesto en la base de datos.
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
	 * @httpMethod GET
	 * @param id Obligatorio. El identificador del manifiesto en la base de datos.
	 * @return Información del manifiesto en formato JSON.
	 */
	def obtener () {
		if (params.long('id')) {
			EventoFirma evento = EventoFirma.get(params.id)
			if(!evento) {
				render message(code:'eventNotFound', args:["${params.id}"]) 
				return false
			}
			def eventoMap = eventoService.optenerEventoFirmaJSONMap(evento)
			render eventoMap as JSON
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * @httpMethod GET
	 * @param max Opcional (por defecto 20). Número máximo de documentos que 
	 * 		  devuelve la consulta (tamaño de la página).
	 * @param estadoEvento Opcional, posibles valores 'ACTIVO','CANCELADO', 'FINALIZADO', 'PENDIENTE_COMIENZO'.
	 * 		               El estado de los eventos que se desea consultar.
	 * @param offset Opcional (por defecto 0). Indice a partir del cual se pagina el resultado.
	 * @param order Opcional, posibles valores 'asc', 'desc'(por defecto). Orden en que se muestran los
	 *        resultados según la fecha de inicio.
	 * @return Página con manifiestos en formato JSON que cumplen con el criterio de búsqueda.
	 */
	def obtenerManifiestos () {
		def eventoList = []
		def consultaMap = new HashMap()
		consultaMap.eventos = new HashMap()
		def firmas = []
		if (params.ids?.size() > 0) {
			EventoFirma.getAll(params.ids).collect {evento ->
				   if (evento) eventoList << evento;
		   }
		   if (eventoList.size() == 0) {
			   response.status = Respuesta.SC_NOT_FOUND //Not Found
			   render message(code: 'eventNotFound', args:[params.ids])
			   return
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
	   eventoList.collect {eventoItem ->
		   firmas.add(eventoService.optenerEventoFirmaJSONMap(eventoItem))
	   }
	   consultaMap.eventos.firmas = firmas
	   response.setContentType("application/json")
	   render consultaMap as JSON
	}
	
	/**
	 * (EN DESUSO)
	 * @httpMethod POST
	 * @param archivoFirmado Archivo firmado en formato SMIME en cuyo contenido se 
	 *        encuentra el manifiesto que se desea publicar en formato HTML.
	 * @return Recibo que consiste en el archivo firmado recibido con la firma añadida del servidor.
	 */
	def guardarAdjuntandoValidacion () {
		try {
			flash.respuesta = eventoFirmaService.guardarEvento(
				params.smimeMessageReq, request.getLocale())
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR_EJECUCION
			render ex.getMessage()
			return false 
		}
	}
	
	/**
	 * Servicio que devuelve estadísticas asociadas a un manifiesto.
	 * 
	 * @httpMethod GET
	 * @param id Identificador en la base de datos del manifiesto que se desea consultar.
	 * @return Estadísticas asociadas al manifiesto que se desea consultar en formato JSON.
	 */
	def estadisticas () {
		if (params.long('id')) {
			EventoFirma eventoFirma
			if (!params.evento) {
				EventoFirma.withTransaction {
					eventoFirma = EventoFirma.get(params.id)
				}
			} 
			else eventoFirma = params.evento
			if (eventoFirma) {
				def estadisticasMap = eventoService.optenerEventoFirmaJSONMap(eventoFirma)
				estadisticasMap.numeroFirmas = Documento.countByEventoAndEstado(
					eventoFirma, Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
				estadisticasMap.informacionFirmasURL = "${grailsApplication.config.grails.serverURL}" +
					"/evento/informacionFirmas?id=${eventoFirma.id}"
				estadisticasMap.URL = "${grailsApplication.config.grails.serverURL}" + 
					"/evento/obtener?id=${eventoFirma.id}"
				response.status = Respuesta.SC_OK
				render estadisticasMap as JSON
				return false
			}
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'eventNotFound', args:[params.ids])
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}

	/**
	 * Servicio que devuelve todas las firmas recibidas por un manifiesto.
	 *
	 * @httpMethod POST
	 * @param archivoFirmado Archivo firmado en formato SMIME con los datos del
	 * 		  manifiesto origen de la copia de seguridad.
	 * @return Archivo zip con todas las firmas que ha recibido el manifiesto consultado.
	 */
	def guardarSolicitudCopiaRespaldo () {
		EventoFirma eventoFirma
		if (!params.evento) {
			SMIMEMessageWrapper smimeMessage= params.smimeMessageReq
			def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			if (params.long('eventoId')) {
				eventoFirma = EventoFirma.get(mensajeJSON.eventoId)
				if (!eventoFirma) {
					response.status = Respuesta.SC_NOT_FOUND
					render message(code: 'eventNotFound', args:[mensajeJSON.eventoId])
					return false
				}
			} else {
				response.status = Respuesta.SC_ERROR_PETICION
				render message(code: 'error.PeticionIncorrectaHTML', args:[
					"${grailsApplication.config.grails.serverURL}/${params.controller}"])
				return false
			}
		} else eventoFirma = params.evento
		Respuesta respuesta = eventoFirmaService.generarCopiaRespaldo(
			eventoFirma, request.getLocale())
		File copiaRespaldo = null
		if(Respuesta.SC_OK == respuesta.codigoEstado) {
			copiaRespaldo = respuesta.file
		} 
		if (copiaRespaldo != null) {
			def bytesCopiaRespaldo = FileUtils.getBytesFromFile(copiaRespaldo)
			response.contentLength = bytesCopiaRespaldo.length
			response.setHeader("Content-disposition", "filename=${copiaRespaldo.getName()}")
			response.setHeader("NombreArchivo", "${copiaRespaldo.getName()}")
			response.setContentType("application/octet-stream")
			response.outputStream << bytesCopiaRespaldo
			response.outputStream.flush()
			return false
		} else {
			String msg = respuesta?.mensaje
			log.error (message(code: 'error.SinCopiaRespaldo') + " - ${msg}")
			response.status = respuesta.codigoEstado
			if(!msg) msg = message(code: 'error.SinCopiaRespaldo')
			render msg
			return false
		}
	}

}