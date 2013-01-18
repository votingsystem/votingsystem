package org.sistemavotacion.controlacceso

import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.controlacceso.modelo.*
import grails.converters.JSON
import org.sistemavotacion.util.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
class EventoFirmaController {

	def eventoFirmaService
	def pdfRenderingService
	def pdfService
	def eventoService
	def htmlService
	
	def index() { }
	
	
	def validarPDF() {
		if (params.long('id')) {
			EventoFirma evento = EventoFirma.get(params.id)
			if(!evento) {
				response.status = 400
				render "El evento '${params.id}' no existe"
				return false
			}
			try {
				String nombreOferta = ((MultipartHttpServletRequest) request)?.getFileNames()?.next();
				log.debug "Recibido archivo: ${nombreOferta}"
				MultipartFile multipartFile = ((MultipartHttpServletRequest) request)?.getFile(nombreOferta);
				if (multipartFile?.getBytes() != null || params.archivoFirmado) {
					Respuesta respuesta = pdfService.validarFirma(multipartFile.getBytes(),
						evento, Documento.Estado.MANIFIESTO, request.getLocale())
					if (200 != respuesta.codigoEstado) {
						log.debug "Problema en la recepción del archivo - ${respuesta.mensaje}"
					}
					response.status = respuesta.codigoEstado
					render respuesta.mensaje
					return false
				}
			} catch (Exception ex) {
				log.error (ex.getMessage(), ex)
				response.status = 400
				render(ex.getMessage())
				return false
			}
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	def publicarPDF = {
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
				runAsync {
					ByteArrayOutputStream bytes = pdfRenderingService.render(
						template: "/eventoFirma/pdf", model:[evento:evento])
					evento.pdf = bytes.toByteArray()
					evento.refresh()
					evento.save()
				}
				log.debug "Salvado evento ${evento.id}"
				//render(view:"publicarPDF")
				render evento.id
				return false
			}
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			flash.respuesta = new Respuesta(mensaje:ex.getMessage(),
				codigoEstado:500, tipo: Tipo.ERROR_DE_SISTEMA)
			forward controller: "error500", action: "procesar"
		}
	}
	
	def obtenerPDF = {
		if (params.long('id')) {
			EventoFirma evento = EventoFirma.get(params.id)
			if(!evento) {
				render "El evento '${params.id}' no existe"
				return false
			}
			response.setHeader("Content-disposition", "attachment; filename=manifiesto.pdf")
			response.contentType = "application/pdf"
			if( evento.pdf)
				response.outputStream << evento.pdf // Performing a binary stream copy
			else {
				renderPdf(template: "/eventoFirma/pdf", model:[evento:evento])
			}
			response.outputStream.flush()
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	def obtenerHtml = {
		if (params.long('id')) {
			EventoFirma evento = EventoFirma.get(params.id)
			if(!evento) {
				render "El evento '${params.id}' no existe"
				return false
			}
			render evento.contenido
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	def obtener = {
		if (params.long('id')) {
			EventoFirma evento = EventoFirma.get(params.id)
			if(!evento) {
				render "El evento '${params.id}' no existe"
				return false
			}
			def eventoMap = [id:evento.id, asunto: evento.asunto, contenido:evento.contenido,
				fechaFin:evento.getFechaFin(), 
				autor:evento.usuario?.nombre + "" + evento.usuario?.primerApellido]
			render eventoMap as JSON
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	def obtenerManifiestos = {
		def eventoList = []
		def consultaMap = new HashMap()
		consultaMap.eventos = new HashMap()
		def firmas = []
		if (params.ids?.size() > 0) {
			EventoFirma.getAll(params.ids).collect {evento ->
				   if (evento) eventoList << evento;
		   }
		   if (eventoList.size() == 0) {
			   response.status = 404 //Not Found
			   render message(code: 'evento.eventoNotFound', args:[params.ids])
			   return
		   }
	   } else {
		   params.sort = "fechaInicio"
		   log.debug " -Params: " + params
		   Evento.Estado estadoEvento
		   if(params.estadoEvento) estadoEvento = Evento.Estado.valueOf(params.estadoEvento)
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
				   consultaMap.numeroTotalEventosFirmaEnSistema = EventoFirma.countByEstadoOrEstadoOrEstadoOrEstado(Evento.Estado.ACTIVO,
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
	
	def guardarAdjuntandoValidacion = {
		try {
			flash.respuesta = eventoFirmaService.guardarEvento(
				params.smimeMessageReq, request.getLocale())
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			flash.respuesta = new Respuesta(mensaje:ex.getMessage(),
				codigoEstado:500, tipo: Tipo.ERROR_DE_SISTEMA)
			forward controller: "error500", action: "procesar"
		}
	}
	
	def estadisticas = {
		if (params.long('id')) {
			EventoFirma eventoFirma
			if (!params.evento) eventoFirma = EventoFirma.get(params.id)
			else eventoFirma = params.evento
			if (eventoFirma) {
				response.status = 200
				def estadisticasMap = new HashMap()
				estadisticasMap.id = eventoFirma.id
				estadisticasMap.tipo = Tipo.EVENTO_FIRMA.toString()
				estadisticasMap.numeroFirmas = eventoFirma.firmas.size()
				estadisticasMap.estado =  eventoFirma.estado.toString()
				estadisticasMap.usuario = eventoFirma.usuario.getNif()
				estadisticasMap.fechaInicio = eventoFirma.getFechaInicio()
				estadisticasMap.fechaFin = eventoFirma.getFechaFin()
				estadisticasMap.solicitudPublicacionURL =
					"${grailsApplication.config.grails.serverURL}/documento/obtenerManifiesto?id=${eventoFirma.id}"
				estadisticasMap.informacionFirmasURL = "${grailsApplication.config.grails.serverURL}/evento/informacionFirmas?id=${eventoFirma.id}"
				estadisticasMap.URL = "${grailsApplication.config.grails.serverURL}/evento/obtener?id=${eventoFirma.id}"
				render estadisticasMap as JSON
				return false
			}
			response.status = 404
			render message(code: 'evento.eventoNotFound', args:[params.ids])
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}

	
	def guardarSolicitudCopiaRespaldo = {
		EventoFirma eventoFirma
		if (!params.evento) {
			SMIMEMessageWrapper smimeMessage= params.smimeMessageReq
			def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
			if (params.long('eventoId')) {
				eventoFirma = EventoFirma.get(mensajeJSON.eventoId)
				if (!eventoFirma) {
					response.status = 404
					render message(code: 'evento.eventoNotFound', args:[mensajeJSON.eventoId])
					return false
				}
			} else {
				response.status = 400
				render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
				return false
			}
		} else eventoFirma = params.evento
		File copiaRespaldo = eventoFirmaService.generarCopiaRespaldo(eventoFirma, request.getLocale())
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
			flash.respuesta = new Respuesta(tipo: Tipo.ERROR_DE_SISTEMA,
				mensaje:message(code: 'error.SinCopiaRespaldo'),codigoEstado:500)
			forward controller: "error500", action: "procesar"
		}
	}

}