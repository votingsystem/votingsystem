package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.PDFDocumentVS
import org.votingsystem.model.ResponseVS
/**
 * @infoController Documentos
 * @descController Servicios relacionados con PDFs.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class PDFDocumentVSController {

	
	/**
	 * Servicio que proporciona acceso a las firmas que recibe un manifiesto.
	 * 
	 * @httpMethod [GET]
	 * @param [id] El identificador del documento en la base de datos.
	 * @return El documento PDF asociado al identificador.
	 */
	def getSignedManifest () {
		PDFDocumentVS pdfDocument
		PDFDocumentVS.withTransaction {
			pdfDocument = PDFDocumentVS.findWhere(id:params.long('id'),
				state:PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED)
		}
		if(pdfDocument) {
            params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.PDF,
                messageBytes: pdfDocument.pdf)
		} else params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'documentNotFoundMsg', args:[params.id]))
	}
}
