package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.util.FileUtils;
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

/**
 * @infoController Recogida de firmas
 * @descController Servicios relacionados con la recogida de firmas.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 */
class RecolectorFirmaController {
	
    def eventoFirmaService
	def recolectorFirmaService
	def pdfService
	
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/recolectorFirma'.
	 */
	def index() { }
	
	/**
	 * Servicio que valida firmas recibidas en documentos PDF
	 *
	 * @httpMethod POST
	 * @param signedPDF Obligatorio. PDF con el documento firmado.
	 * @param id Obligatorio. El identificador en la base de datos del manifiesto que se está firmando.
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
	def validarPDF() { 
		EventoFirma evento = EventoFirma.get(params.id)
		if(!evento) {
			response.status = Respuesta.SC_ERROR_PETICION
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
	
	/**
	 * Servicio que valida firmas recibidas en documentos SMIME
	 *
	 * @httpMethod POST
	 * @param archivoFirmado Obligatorio. Documento SMIME firmado.
	 * @return El archivo SMIME recibido con la firma añadida del servidor.
	 */
    def guardarAdjuntandoValidacion () {
        try {
            flash.respuesta = recolectorFirmaService.guardar(
				params.smimeMessageReq, request.getLocale())
        } catch (Exception ex) {
            log.error (ex.getMessage(), ex)
            flash.respuesta = new Respuesta(tipo:Tipo.ERROR_DE_SISTEMA,
                codigoEstado:Respuesta.SC_ERROR_EJECUCION, mensaje:Tipo.ERROR_DE_SISTEMA.toString())
        }
    }
	
	/* TODO
	 * def guardarAdjuntandoValidacionAsync () {
		try {
			flash.respuesta = recolectorFirmaService.guardarAsync(params.smimeMessageReq)
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			flash.respuesta = new Respuesta(tipo:Tipo.ERROR_DE_SISTEMA,
				codigoEstado:Respuesta.SC_ERROR_EJECUCION, mensaje:Tipo.ERROR_DE_SISTEMA.toString())
		}
	}*/
	

}