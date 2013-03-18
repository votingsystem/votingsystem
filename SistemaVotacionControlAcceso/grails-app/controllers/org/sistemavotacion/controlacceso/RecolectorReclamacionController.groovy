package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.util.FileUtils;

/**
 * @infoController Recogida de reclamaciones
 * @descController Servicios relacionados con la recogida de reclamaciones.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 */
class RecolectorReclamacionController {

    def eventoReclamacionService
	def reclamacionService
        
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/recolectorReclamacion'.
	 */
	def index() { }
	
	/**
	 * Servicio que valida reclamaciones recibidas en documentos SMIME
	 *
	 * @httpMethod POST
	 * @param archivoFirmado Obligatorio. Documento SMIME firmado con la reclamación.
	 * @return El archivo SMIME recibido con la firma añadida del servidor.
	 */
    def guardarAdjuntandoValidacion () {
        try {
            flash.respuesta = reclamacionService.guardar(
				params.smimeMessageReq, request.getLocale())
        } catch (Exception ex) {
            log.error (ex.getMessage(), ex)
            flash.respuesta = new Respuesta(tipo:Tipo.ERROR_DE_SISTEMA,
                codigoEstado:Respuesta.SC_ERROR_EJECUCION, mensaje:Tipo.ERROR_DE_SISTEMA.toString())
        }
    }
	

}