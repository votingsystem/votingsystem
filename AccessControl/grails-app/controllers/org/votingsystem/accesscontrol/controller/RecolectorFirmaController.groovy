package org.votingsystem.accesscontrol.controller

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.votingsystem.accesscontrol.model.*;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.FileUtils;
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

import com.itextpdf.text.Document;

/**
 * @infoController Recogida de firmas
 * @descController Servicios relacionados con la recogida de firmas.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class RecolectorFirmaController {
	
    def eventoFirmaService
	def recolectorFirmaService
	
	/**
	 * Servicio que valida firmas recibidas en documentos PDF
	 *
	 * @httpMethod [POST]
     * @serviceURL [/recolectorFirma/$id]
	 * @param [id] Obligatorio. El identificador en la base de datos del manifiesto que se está firmando.
     * @requestContentType [application/pdf,application/x-pkcs7-signature] Obligatorio. El archivo PDF con la firma.
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
	def index() {
		Documento documento = params.pdfDocument
		if(params.long('id') && documento &&	
			documento.estado == Documento.Estado.VALIDADO) {
			EventoFirma evento = null;
			EventoFirma.withTransaction{
				evento = EventoFirma.get(params.long('id'))
			}
			if(!evento) {
				response.status = ResponseVS.SC_ERROR_REQUEST
				render message(code: 'manifestNotFound', args:[params.id])
				return false
			}
			try {
				ResponseVS respuesta = recolectorFirmaService.saveManifestSignature(
					documento, evento, request.getLocale())
				response.status = respuesta.statusCode
				render respuesta.message
				return false
			} catch (Exception ex) {
				log.error (ex.getMessage(), ex)
				response.status = ResponseVS.SC_ERROR_REQUEST
				render(ex.getMessage())
				return false
			}
		}
		response.status = ResponseVS.SC_ERROR_REQUEST
		render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
}