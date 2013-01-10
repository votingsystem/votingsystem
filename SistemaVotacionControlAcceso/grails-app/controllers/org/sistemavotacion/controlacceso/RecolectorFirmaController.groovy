package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.util.FileUtils;
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

class RecolectorFirmaController {
	
    def eventoFirmaService
	def recolectorFirmaService
	def pdfService
	
	def index() { }
	
	def validarPDF() { 
		EventoFirma evento = EventoFirma.get(params.id)
		if(!evento) {
			response.status = 400
			render "El evento '${params.id}' no existe"
			return false
		}
		try {
			String nombreArchivo = ((MultipartHttpServletRequest) request)?.getFileNames()?.next();
			log.debug "Recibido archivo: ${nombreArchivo}"
			MultipartFile multipartFile = ((MultipartHttpServletRequest) request)?.getFile(nombreArchivo);
			if (multipartFile?.getBytes() != null || params.archivoFirmado) {
				Respuesta respuesta = pdfService.validarFirma(multipartFile.getBytes(), 
					evento, Documento.Estado.FIRMA_DE_MANIFIESTO, request.getLocale())
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
	
    def guardarAdjuntandoValidacion = {
        try {
            flash.respuesta = recolectorFirmaService.guardar(
				params.smimeMessageReq, request.getLocale())
        } catch (Exception ex) {
            log.error (ex.getMessage(), ex)
            flash.respuesta = new Respuesta(tipo:Tipo.ERROR_DE_SISTEMA,
                codigoEstado:500, mensaje:Tipo.ERROR_DE_SISTEMA.toString())
        }
    }
	
	/* TODO
	 * def guardarAdjuntandoValidacionAsync = {
		try {
			flash.respuesta = recolectorFirmaService.guardarAsync(params.smimeMessageReq)
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			flash.respuesta = new Respuesta(tipo:Tipo.ERROR_DE_SISTEMA,
				codigoEstado:500, mensaje:Tipo.ERROR_DE_SISTEMA.toString())
		}
	}*/
	

}