package org.centrocontrol.clientegwt.client.modelo;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public enum Tipo {
    
    //GENERICOS
    PETICION_CON_ERRORES(""),
    PETICION_SIN_ARCHIVO(""),
    EVENTO_CON_ERRORES(""),
    OK(""),
    ANULADO(""),
    ERROR(""),     
	
    //EVENTO_VOTACION
    EVENTO_VOTACION(""),
    EVENTO_VOTACION_ERROR(""),
    EVENTO_VOTACION_FINALIZADO(""),

    //VOTO
    VOTO(""),
    VOTO_CON_ERRORES(""),

    
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
    
    //EVENTO
    EVENTO_CANCELADO(""),
    EVENTO_CANCELADO_ERROR(""),
    EVENTO_BORRADO_DE_SISTEMA(""),
                             
    //USUARIO
    HISTORICO_USUARIO(""), 
    
     //Actores
    CENTRO_CONTROL(""), 
    CONTROL_ACCESO("");
     
       
    private String mensaje;
    
    Tipo(String mensaje) {
        this.mensaje = mensaje;
    }
    public String getMensaje() {
        return this.mensaje;
    }
    
}
