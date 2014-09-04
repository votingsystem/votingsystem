package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EventVSManifest
import org.votingsystem.model.PDFDocumentVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS

/**
 * @infoController Recogida de firmas
 * @descController Servicios relacionados con la recogida de firmas.
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
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
		PDFDocumentVS pdfDocument = request.pdfDocument
		if(params.long('id') && pdfDocument && pdfDocument.state == PDFDocumentVS.State.VALIDATED) {
			EventVSManifest eventVS = null;
			EventVSManifest.withTransaction{ eventVS = EventVSManifest.get(params.long('id')) }
			if(!eventVS) {
                return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                        message(code: 'manifestNotFound', args:[params.id]))]
			} else {
                return [responseVS:eventVSManifestSignatureCollectorService.saveManifestSignature(
                        pdfDocument, eventVS, request.getLocale())]
            }
		} else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrorsHTML',
                args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))]
	}


    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }
}