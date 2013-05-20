package org.sistemavotacion.centrocontrol.modelo;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public enum Tipo {
    
    //GENERICOS
    PETICION_CON_ERRORES(""),
    PETICION_SIN_ARCHIVO(""),
    EVENTO_CON_ERRORES(""),
    OK(""),
    ANULADO(""),
    ERROR(""),     
    RECIBO(""),  
    RECIBO_ERROR(""),   
    USER_ERROR(""),
	
    //EVENTO_VOTACION
    EVENTO_VOTACION(""),
    EVENTO_VOTACION_ERROR(""),
    EVENTO_INICIALIZADO(""),
    EVENTO_VOTACION_FINALIZADO(""),

    //VOTO
    VOTO_CON_ERRORES(""),
    VOTO(""),
    VOTO_VALIDADO_CENTRO_CONTROL(""),
    VOTO_VALIDADO_CONTROL_ACCESO(""),
    ANULADOR_VOTO(""),
    ANULADOR_VOTO_ERROR(""),

    CANCELAR_EVENTO(""),
    CANCELAR_EVENTO_ERROR(""),
    
    //EVENTO_FIRMA
    EVENTO_FIRMA(""),
    EVENTO_FIRMA_FINALIZADO(""),
    EVENTO_CANCELADO_ERROR(""),
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
    EVENTO_BORRADO_DE_SISTEMA(""),
    EVENTO_CANCELADO(""),
                             
    //USUARIO
    HISTORICO_USUARIO(""), 
     ERROR_FIRMANDO_VALIDACION(""),
    
     //Actores
    CENTRO_CONTROL(""), 
    CONTROL_ACCESO(""),
    
    
    SOLICITUD_INDEXACION(""),
    SOLICITUD_INDEXACION_ERROR("");
    
       
    private String mensaje;
    
    Tipo(String mensaje) {
        this.mensaje = mensaje;
    }
    public String getMensaje() {
        return this.mensaje;
    }
    
}
