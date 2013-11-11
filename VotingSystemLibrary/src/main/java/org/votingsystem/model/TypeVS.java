package org.votingsystem.model;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public enum TypeVS {
    
	   
    //GENERICOS
    PETICION_CON_ERRORES(""),
    PETICION_SIN_ARCHIVO(""),
    EVENTO_CON_ERRORES(""),
    OK(""),
    ERROR(""),
	
    //EVENTO_VOTACION
    EVENTO_VOTACION(""),
    EVENTO_VOTACION_ERROR(""),
    EVENTO_VOTACION_FINALIZADO(""),

    //VOTO
    VOTO(""),
    VOTO_CON_ERRORES(""),
    ANULADOR_VOTO(""),
    ANULADOR_VOTO_ERROR(""),

    BACKUP(""),
    
    //EVENTO_FIRMA
    EVENTO_FIRMA(""),
    EVENTO_FIRMA_FINALIZADO(""),
    //FIRMA
    FIRMA_EVENTO_CON_ERRORES(""),
    FIRMA_EVENTO_FIRMA(""),
    FIRMA_EVENTO_FIRMA_REPETIDA(""),
    FIRMA_EVENTO_RECLAMACION(""),
    FIRMA_ERROR_CONTENIDO_CON_ERRORES(""),
    
    //EVENTO_RECLAMACION
    EVENTO_RECLAMACION(""), 
    EVENTO_RECLAMACION_ERROR(""),
    EVENTO_RECLAMACION_FINALIZADO(""),
                             
    //USUARIO
    HISTORICO_USUARIO(""), 
     
    //CLIENTE_VOTO
    SOLICITUD_REPETIDA(""),
    HASH_INVALIDO(""),
    RECIBO_VOTO_ERROR(""),
    RECIBO_VOTO_OK(""),
    SOLICITUD_RECIBOS_OBSERVADORES(""),
    SOLICITUD_ACCESO(""),
    
    FIRMA_RECLAMACION_SMIME(""),
    ENVIO_VOTO_SMIME(""),
    REPRESENTATIVE_SELECTION("");
       
    private String message;
    
    TypeVS(String message) {
        this.message = message;
    }
    public String getMessage() {
        return this.message;
    }
    
}
