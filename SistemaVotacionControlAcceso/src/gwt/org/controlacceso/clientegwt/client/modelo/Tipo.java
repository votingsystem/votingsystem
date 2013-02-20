package org.controlacceso.clientegwt.client.modelo;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public enum Tipo {
    
    //GENERICOS
    PETICION_CON_ERRORES(""),
    PETICION_SIN_ARCHIVO("Ha enviado una petición sin ningún evento asociado"),
    EVENTO_CON_ERRORES(""),
    OK(""),
    ANULADO(""),
    EN_OBSERVACION(""), 
    ERROR(""),     
    ERROR_DE_SISTEMA(""),
    PROBLEMAS_CONEXION_SERVIDOR(""),
    RECURSO_NO_ENCONTRADO(""),
    TEMPORALMENTE_FUERA_DE_SERVICIO(""),
    MENSAJE_OK(""),
	
    //EVENTO_VOTACION
    EVENTO_VOTACION(""),
    EVENTO_VOTACION_ERROR(""),
    EVENTO_VOTACION_VALIDADO(""),
    EVENTO_VOTACION_FINALIZADO(""),
    EVENTO_VOTACION_SIN_CENTRO_CONTROL(""),
    PETICION_SIN_EVENTO_VOTACION_ASOCIADO(""),
    //VOTO
    VOTO_REPETIDO(""),
    VOTO_CON_ERRORES(""),

    
    //EVENTO_FIRMA
    EVENTO_FIRMA(""),
    EVENTO_FIRMA_VALIDADO(""),
    EVENTO_FIRMA_ERROR(""),
    EVENTO_FIRMA_FINALIZADO(""),
    //FIRMA
    FIRMA_EVENTO_CON_ERRORES(""),
    FIRMA_EVENTO_FIRMA(""),
    FIRMA_EVENTO_FIRMA_REPETIDA(""),
    FIRMA_EVENTO_RECLAMACION_REPETIDA(""),
    FIRMA_EVENTO_RECLAMACION(""),
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
    
    //Respuestas observador
     ERROR_OPCION(""), 
     ERROR_HASH_TOKEN(""),
     ERROR_IDENTIFICADOR_OPCION(""), 
     ERROR_ID_EVENTO(""), 
     ERROR_CONTROL_ACCESO_SERVER_URL(""), 
     ERROR_PARAMETROS(""),
     ERROR_COMUNICACION(""),
     ERROR_RECUENTO_OPCIONES(""),
    
     //Actores
    CENTRO_CONTROL(""), 
    CONTROL_ACCESO(""),
     
    //TOKEN
    SOLICITUD_T_FALLO(""),
    SOLICITUD_TOKEN(""),
    SOLICITUD_TOKEN_REPETIDA(""),
    TOKEN_SIN_EVENTO_VOTACION_ASOCIADO(""),
    SOLICITUD_T_FALLO_HASH_REPETIDO(""),
    TOKEN_ACCESO("");
    
       
    private String mensaje;
    
    Tipo(String mensaje) {
        this.mensaje = mensaje;
    }
    public String getMensaje() {
        return this.mensaje;
    }
    
}
