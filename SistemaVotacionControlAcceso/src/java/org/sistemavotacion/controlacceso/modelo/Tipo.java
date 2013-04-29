package org.sistemavotacion.controlacceso.modelo;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public enum Tipo {
    
    //GENERICOS
    PETICION_CON_ERRORES(""),
    PETICION_SIN_ARCHIVO("Ha enviado una petición sin ningún evento asociado"),
    EVENTO_CON_ERRORES(""),
    ERROR_CONEXION_CON_ACTOR(""),
    OK(""),
    ANULADO(""),
    EN_OBSERVACION(""), 
    ERROR(""),     
    ERROR_DE_SISTEMA(""),
    PROBLEMAS_CONEXION_SERVIDOR(""),
    RECURSO_NO_ENCONTRADO(""),
    TEMPORALMENTE_FUERA_DE_SERVICIO(""),
    MENSAJE_OK(""),
    REPRESENTATIVE_DATA(""),
    REPRESENTATIVE_SELECTION(""),
    REPRESENTATIVE_SELECTION_VALIDATION(""),
    
    //EVENTO_VOTACION
    EVENTO_VOTACION(""),
    EVENTO_VOTACION_ERROR(""),
    EVENTO_VOTACION_VALIDADO(""),
    EVENTO_VOTACION_FINALIZADO(""),
    EVENTO_VOTACION_SIN_CENTRO_CONTROL(""),
    PETICION_SIN_EVENTO_VOTACION_ASOCIADO(""),
    //VOTO
    VOTO(""),
    VOTO_REPETIDO(""),
    VOTO_CON_ERRORES(""),
    ANULADOR_VOTO(""),
    ANULADOR_SOLICITUD_ACCESO(""),
    
    VOTO_VALIDADO_USUARIO(""),
    VOTO_VALIDADO_CENTRO_CONTROL(""),
    VOTO_VALIDADO_CONTROL_ACCESO(""),

    
    //EVENTO_FIRMA
    EVENTO_FIRMA(""),
    EVENTO_FIRMA_VALIDADO(""),
    EVENTO_FIRMA_ERROR(""),
    EVENTO_FIRMA_FINALIZADO(""),
    //FIRMA
    FIRMA_VALIDADA(""),
    FIRMA_EVENTO_CON_ERRORES(""),
    FIRMA_EVENTO_FIRMA(""),
    FIRMA_EVENTO_FIRMA_REPETIDA(""),
    FIRMA_EVENTO_RECLAMACION_REPETIDA(""),
    FIRMA_EVENTO_RECLAMACION(""),
    FIRMA_EVENTO_RECLAMACION_VALIDADA(""),
    FIRMA_ERROR_EVENTO_NO_ENCONTRADO(""),
    FIRMA_ERROR_CONTENIDO_CON_ERRORES(""),
    
    //EVENTO_RECLAMACION
    EVENTO_RECLAMACION(""), 
    EVENTO_RECLAMACION_ERROR(""),
    EVENTO_RECLAMACION_FINALIZADO(""),
    EVENTO_RECLAMACION_VALIDADO(""),
    
    //EVENTO
    EVENTO_NO_ENCONTRADO(""),
    EVENTO_CANCELADO(""),
    EVENTO_CANCELADO_ERROR(""),
    EVENTO_BORRADO_DE_SISTEMA(""),
                             
    //USUARIO
    HISTORICO_USUARIO(""), 
    
     ERROR_OPCION(""), 
     ERROR_HASH_TOKEN(""),
     ERROR_IDENTIFICADOR_OPCION(""), 
     ERROR_ID_EVENTO(""), 
     ERROR_CONTROL_ACCESO_SERVER_URL(""), 
     ERROR_PARAMETROS(""),
     ERROR_COMUNICACION(""),
     ERROR_RECUENTO_OPCIONES(""),
     ERROR_FIRMANDO_VALIDACION(""),
     ERROR_VALIDANDO_CSR(""),
    
     //Actores
    CENTRO_CONTROL(""), 
    CONTROL_ACCESO(""),
     
    //TOKEN
    SOLICITUD_ACCESO_FALLO(""),
    SOLICITUD_ACCESO(""),
    SOLICITUD_ACCESO_REPETIDA(""),

    //SOLICITUD_ASOCIACION
    SOLICITUD_ASOCIACION_CON_ERRORES(""),
    SOLICITUD_ASOCIACION(""),
    INDEFINIDO(""),
    SOLICITUD_ASOCIACION_CON_ACTOR_REPETIDO("");
    
       
    private String mensaje;
    
    Tipo(String mensaje) {
        this.mensaje = mensaje;
    }
    public String getMensaje() {
        return this.mensaje;
    }
    
    public static Tipo getTipoEnFuncionEstado(Evento.Estado estadoEvento) {
    	switch(estadoEvento) {
	    	case BORRADO_DE_SISTEMA:
	    		return EVENTO_BORRADO_DE_SISTEMA;
	    	case CANCELADO:
	    		return EVENTO_CANCELADO;
			default:
				return INDEFINIDO;
    	}
    	
    }
    
}
