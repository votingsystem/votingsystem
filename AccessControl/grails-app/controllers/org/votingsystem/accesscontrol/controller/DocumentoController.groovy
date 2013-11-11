package org.votingsystem.accesscontrol.controller

import org.votingsystem.accesscontrol.model.*
import org.votingsystem.model.ResponseVS;

/**
 * @infoController Documentos
 * @descController Servicios relacionados con PDFs.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class DocumentoController {

	
	/**
	 * Servicio que proporciona acceso a las firmas que recibe un manifiesto.
	 * 
	 * @httpMethod [GET]
	 * @param [id] El identificador del documento en la base de datos.
	 * @return El documento PDF asociado al identificador.
	 */
	def obtenerFirmaManifiesto () {
		Documento documento
		Documento.withTransaction {
			documento = Documento.findWhere(id:params.long('id'), 
				estado:Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
		}
		if(!documento) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'documentNotFoundMsg', args:[params.id])
			return false
		}
		//response.setHeader("Content-disposition", "attachment; filename=manifiesto.pdf")
		response.contentType = "application/pdf"
		response.setHeader("Content-Length", "${documento.pdf.length}")
		response.outputStream << documento.pdf // Performing a binary stream copy
		response.outputStream.flush()
	}
}
