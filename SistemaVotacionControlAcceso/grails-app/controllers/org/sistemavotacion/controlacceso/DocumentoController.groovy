package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*

/**
 * @infoController Documentos
 * @descController Servicios relacionados con PDFs.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 */
class DocumentoController {

	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/documento'.
	 */
    def index() { }
	
	/**
	 * @httpMethod GET
	 * @param id El identificador del manifiesto en la base de datos.
	 * @return El manifiesto en formato PDF.
	 */
	def obtenerManifiesto () {
		if(!params.long('id')) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'error.PeticionIncorrectaHTML', args:[
				"${grailsApplication.config.grails.serverURL}/${params.controller}"])
			return false
		}
		EventoFirma evento = EventoFirma.get(params.id)
		if(!evento) {
			render message(code: 'eventNotFound', args:[params.id])
			return false
		}
		Documento documento
		Documento.withTransaction {
			documento = Documento.findWhere(evento:evento, estado:Documento.Estado.MANIFIESTO_VALIDADO)
		}
		if(!documento) {
			render "El evento de recogida de firmas con id '${params.id}' no tiene asociado un PDF firmado"
			return false
		}
		//response.setHeader("Content-disposition", "attachment; filename=manifiesto.pdf")
		response.contentType = "application/pdf"
		response.setHeader("Content-Length", "${documento.pdf.length}")
		response.outputStream << documento.pdf // Performing a binary stream copy
		response.outputStream.flush()
	}
	
	/**
	 * @httpMethod GET
	 * @param id El identificador del documento en la base de datos.
	 * @return El documento PDF asociado al identificador.
	 */
	def obtenerFirmaManifiesto () {
		if(!params.long('id')) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'error.PeticionIncorrectaHTML', args:[
				"${grailsApplication.config.grails.serverURL}/${params.controller}"])
			return false
		}
		Documento documento
		Documento.withTransaction {
			documento = Documento.get(params.id)
		}
		if(!documento ||
			documento.estado != Documento.Estado.FIRMA_MANIFIESTO_VALIDADA) {
			render "El evento de recogida de firmas con id '${params.id}' no tiene asociado un PDF firmado"
			return false
		}
		//response.setHeader("Content-disposition", "attachment; filename=manifiesto.pdf")
		response.contentType = "application/pdf"
		response.setHeader("Content-Length", "${documento.pdf.length}")
		response.outputStream << documento.pdf // Performing a binary stream copy
		response.outputStream.flush()
	}
}
