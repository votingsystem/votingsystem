package org.centrocontrol.clientegwt.client.modelo;

import java.io.Serializable;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class Respuesta implements Serializable{
    
	private static final long serialVersionUID = 1L;
	
	public static final int SC_OK = 200;
    public static final int SC_OK_ANULACION_SOLICITUD_ACCESO = 270;
    public static final int SC_ERROR_PETICION = 400;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_ERROR_VOTO_REPETIDO = 470;
    public static final int SC_ANULACION_REPETIDA = 471;
    
    public static final int SC_ERROR_EJECUCION = 500;
    public static final int SC_PROCESANDO = 700;
    public static final int SC_CANCELADO = 0;
 
}