package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.util.FileUtils;

class RecolectorReclamacionController {

    def eventoReclamacionService
	def reclamacionService
        
	def index() { }
	
    def guardarAdjuntandoValidacion = {
        try {
            flash.respuesta = reclamacionService.guardar(
				params.smimeMessageReq, request.getLocale())
        } catch (Exception ex) {
            log.error (ex.getMessage(), ex)
            flash.respuesta = new Respuesta(tipo:Tipo.ERROR_DE_SISTEMA,
                codigoEstado:500, mensaje:Tipo.ERROR_DE_SISTEMA.toString())
        }
    }
	

}