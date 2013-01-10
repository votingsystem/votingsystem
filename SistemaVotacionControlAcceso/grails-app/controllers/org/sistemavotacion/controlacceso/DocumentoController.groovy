package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*

class DocumentoController {

    def index() { }
	
	def obtenerManifiesto () {
		if(!params.long('id')) {
			log.debug("redirecionando a index")
			forward action: "index"
			return false
		}
		EventoFirma evento = EventoFirma.get(params.id)
		if(!evento) {
			render "El evento de recogida de firmas con id '${params.id}' no existe"
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
	
	def obtenerFirmaManifiesto () {
		if(!params.long('id')) {
			log.debug("redirecionando a index")
			forward action: "index"
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
