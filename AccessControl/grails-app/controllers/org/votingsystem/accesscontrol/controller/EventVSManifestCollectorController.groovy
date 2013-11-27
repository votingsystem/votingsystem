package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.PDFDocumentVS
import org.votingsystem.model.EventVSManifest
import org.votingsystem.model.ResponseVS
/**
 * @infoController Recogida de firmas
 * @descController Servicios relacionados con la recogida de firmas.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class EventVSManifestCollectorController {
	
    def eventVSManifestService
	def eventVSManifestSignatureCollectorService
	
	/**
	 * Servicio que valida firmas recibidas en documentos PDF
	 *
	 * @httpMethod [POST]
     * @serviceURL [/eventVSManifestCollector/$id]
	 * @param [id] Obligatorio. El identificador en la base de datos del manifiesto que se está firmando.
     * @requestContentType [application/pdf,application/x-pkcs7-signature] Obligatorio. El archivo PDF con la signatureVS.
	 * @return Si todo va bien devuelve un código de estado HTTP 200.
	 */
	def index() {
		PDFDocumentVS pdfDocument = params.pdfDocument
		if(params.long('id') && pdfDocument &&
			pdfDocument.state == PDFDocumentVS.State.VALIDATED) {
			EventVSManifest eventVS = null;
			EventVSManifest.withTransaction{
				eventVS = EventVSManifest.get(params.long('id'))
			}
			if(!eventVS) {
				response.status = ResponseVS.SC_ERROR_REQUEST
				render message(code: 'manifestNotFound', args:[params.id])
				return false
			}
			try {
				ResponseVS responseVS = eventVSManifestSignatureCollectorService.saveManifestSignature(
					pdfDocument, eventVS, request.getLocale())
				response.status = responseVS.statusCode
				render responseVS.message
				return false
			} catch (Exception ex) {
				log.error (ex.getMessage(), ex)
				response.status = ResponseVS.SC_ERROR_REQUEST
				render(ex.getMessage())
				return false
			}
		}
		response.status = ResponseVS.SC_ERROR_REQUEST
		render message(code: 'requestWithErrorsHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
}