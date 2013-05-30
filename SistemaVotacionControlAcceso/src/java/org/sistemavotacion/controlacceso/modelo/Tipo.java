package org.sistemavotacion.controlacceso.modelo;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public enum Tipo {
    
    //GENERICOS
    PETICION_CON_ERRORES(""),
    PETICION_SIN_ARCHIVO(""),
    EVENTO_CON_ERRORES(""),
    ERROR_CONEXION_CON_ACTOR(""),
    OK(""),
    ANULADO(""),
    ERROR(""),   
    RECIBO_ERROR(""),   
    USER_ERROR(""),

    REPRESENTATIVE_DATA(""),
    REPRESENTATIVE_DATA_ERROR(""),
    REPRESENTATIVE_REVOKE(""),
    REPRESENTATIVE_REVOKE_ERROR(""),
    
    BACKUP_GEN_ERROR(""),
    BACKUP_GEN(""),
    
    REPRESENTATIVE_SELECTION(""),
    REPRESENTATIVE_SELECTION_ERROR(""),
    REPRESENTATIVE_VOTING_HISTORY_REQUEST(""),
    REPRESENTATIVE_VOTING_HISTORY_REQUEST_ERROR(""),
    REPRESENTATIVE_ACCREDITATIONS_REQUEST(""),
    REPRESENTATIVE_ACCREDITATIONS_REQUEST_ERROR(""),
    
    RECIBO(""),
    CANCELAR_EVENTO(""),
    CANCELAR_EVENTO_ERROR(""),
    EVENTO(""),
    //EVENTO_VOTACION
    EVENTO_VOTACION(""),
    EVENTO_VOTACION_ERROR(""),
    EVENTO_VOTACION_FINALIZADO(""),

    //VOTO
    VOTO(""),
    VOTO_CON_ERRORES(""),
    ANULADOR_VOTO(""),
    ANULADOR_VOTO_ERROR(""),
    
    VOTO_VALIDADO_USUARIO(""),
    VOTO_VALIDADO_CENTRO_CONTROL(""),
    VOTO_VALIDADO_CONTROL_ACCESO(""),

    
    //EVENTO_FIRMA
    EVENTO_FIRMA(""),
    EVENTO_FIRMA_FINALIZADO(""),
    //FIRMA
    FIRMA_VALIDADA(""),
    FIRMA_EVENTO_CON_ERRORES(""),
    FIRMA_EVENTO_FIRMA(""),
    FIRMA_EVENTO_FIRMA_REPETIDA(""),
    FIRMA_EVENTO_RECLAMACION_ERROR(""),
    FIRMA_EVENTO_RECLAMACION(""),
    FIRMA_ERROR_CONTENIDO_CON_ERRORES(""),
    
    //EVENTO_RECLAMACION
    EVENTO_RECLAMACION(""), 
    EVENTO_RECLAMACION_ERROR(""),
    EVENTO_RECLAMACION_FINALIZADO(""),
    
    //EVENTO
    EVENTO_NO_ENCONTRADO(""),
    EVENTO_CANCELADO(""),
    EVENTO_CANCELADO_ERROR(""),
    EVENTO_BORRADO_DE_SISTEMA(""),
                             
    //USUARIO
    HISTORICO_USUARIO(""), 
    ERROR_USUARIO(""), 
    ERROR_FIRMANDO_VALIDACION(""),
    ERROR_VALIDANDO_CSR(""),
    
    SOLICITUD_INDEXACION(""),
     
    CENTRO_CONTROL(""), 
    CONTROL_ACCESO(""),

    SOLICITUD_ACCESO_ERROR(""),
    SOLICITUD_ACCESO(""),
    
    ASOCIAR_CENTRO_CONTROL(""),
    ASOCIAR_CENTRO_CONTROL_ERROR("");
    

    private String mensaje;
    
    Tipo(String mensaje) {
        this.mensaje = mensaje;
    }
    public String getMensaje() {
        return this.mensaje;
    }
    
}
