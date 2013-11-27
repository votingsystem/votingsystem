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
	 * @param [id] El identificador del PDFDocumentVS en la base de datos.
	 * @return El PDFDocumentVS PDF asociado al identificador.
	 */
	def getSignedManifest () {
		PDFDocumentVS documento
		PDFDocumentVS.withTransaction {
			documento = PDFDocumentVS.findWhere(id:params.long('id'),
				state:PDFDocumentVS.State.MANIFEST_SIGNATURE_VALIDATED)
		}
		if(!documento) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'documentNotFoundMsg', args:[params.id])
			return false
		}
		//response.setHeader("Content-disposition", "attachment; filename=manifest.pdf")
		response.contentType = ContentTypeVS.PDF
		response.setHeader("Content-Length", "${documento.pdf.length}")
		response.outputStream << documento.pdf // Performing a binary stream copy
		response.outputStream.flush()
	}
}
