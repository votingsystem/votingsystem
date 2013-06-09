package org.sistemavotacion.modelo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Respuesta<T> {
    
    private static Logger logger = LoggerFactory.getLogger(Respuesta.class);
    
    public static final int SC_OK = 200;
    public static final int SC_OK_ANULACION_SOLICITUD_ACCESO = 270;
    public static final int SC_REQUEST_TIMEOUT = 408;
    public static final int SC_ERROR_PETICION = 400;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_ERROR_VOTO_REPETIDO = 470;
    public static final int SC_ANULACION_REPETIDA = 471;
    public static final int SC_NULL_REQUEST = 472;
    
    public static final int SC_ERROR = 500;
    public static final int SC_PROCESANDO = 700;
    public static final int SC_CANCELADO = 0;

    private int codigoEstado;
    private String mensaje;
    private T data;
    private Tipo tipo;
    private Evento evento;
    private SMIMEMessageWrapper smimeMessage;
    private ReciboVoto reciboVoto;
    private byte[] bytesArchivo;
    private File archivo;
    private List<String> errorList;
        
    public Respuesta () {  }
    
        
    public Respuesta (int codigoEstado) {
        this.codigoEstado = codigoEstado;    
    }
    
    public Respuesta (int codigoEstado, Evento evento) {
        this.codigoEstado = codigoEstado;
        this.evento = evento;
    }
    
    public Respuesta (int codigoEstado, ReciboVoto reciboVoto) {
        this.codigoEstado = codigoEstado;
        this.reciboVoto = reciboVoto;
    }
    
    public Respuesta (int codigoEstado, String mensaje, Object objeto) {
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
    }

    public Respuesta (int codigoEstado, String mensaje) {
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
    }
        
    public Respuesta (int codigoEstado, String mensaje, byte[] bytesArchivo) {
        this.codigoEstado = codigoEstado;
        this.mensaje = mensaje;
        this.bytesArchivo = bytesArchivo;
    }

    /**
     * @return the mensaje
     */
    public String getMensaje() {
        return mensaje;
    }

    /**
     * @param mensaje the mensaje to set
     */
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
    
    
    public String toString () {
    	StringBuilder respuesta = new StringBuilder();
    	respuesta.append("Estado: ").append(codigoEstado).append(" - Mensaje: ").append(mensaje);
    	if (tipo != null) {
    		respuesta.append(" - Tipo: ").append(tipo.toString());
    	}
    	return respuesta.toString();
    }

    /**
     * @return the codigoEstadoHTTP
     */
    public int getCodigoEstado() {
        return codigoEstado; 
    }

    /**
     * @param codigoEstadoHTTP the codigoEstadoHTTP to set
     */
    public void setCodigoEstado(int codigoEstado) {
        this.codigoEstado = codigoEstado;
    }

    /**
     * @return the mensajeDeError
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * @param mensajeDeError the mensajeDeError to set
     */
    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }


    /**
     * @return the bytesArchivo
     */
    public byte[] getBytesArchivo() {
        return bytesArchivo;
    }

    /**
     * @param bytesArchivo the bytesArchivo to set
     */
    public void setBytesArchivo(byte[] bytesArchivo) {
        this.bytesArchivo = bytesArchivo;
    }

    /**
     * @return the archivo
     */
    public File getArchivo() {
        return archivo;
    }

    /**
     * @param archivo the archivo to set
     */
    public void setArchivo(File archivo) {
        this.archivo = archivo;
    }

    /**
     * @return the reciboVoto
     */
    public ReciboVoto getReciboVoto() {
        return reciboVoto;
    }

    /**
     * @param reciboVoto the reciboVoto to set
     */
    public void setReciboVoto(ReciboVoto reciboVoto) {
        this.reciboVoto = reciboVoto;
    }

    /**
     * @return the data
     */
    public T getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * @return the evento
     */
    public Evento getEvento() {
        return evento;
    }

    /**
     * @param evento the evento to set
     */
    public void setEvento(Evento evento) {
        this.evento = evento;
    }
    
    public void appendMessage(String msg) {
        if(mensaje != null) mensaje = mensaje + "\n" + msg;
        else mensaje = msg;
    }
    
    public void appendErrorMessage(String msg) {
        codigoEstado = SC_ERROR;
        if(mensaje != null) mensaje = mensaje + " - " + msg;
        else mensaje = msg;
    }

    /**
     * @return the smimeMessage
     */
    public SMIMEMessageWrapper getSmimeMessage() {
        return smimeMessage;
    }

    /**
     * @param smimeMessage the smimeMessage to set
     */
    public void setSmimeMessage(SMIMEMessageWrapper smimeMessage) {
        this.smimeMessage = smimeMessage;
    }

    /**
     * @return the errorList
     */
    public List<String> getErrorList() {
        return errorList;
    }

    /**
     * @param errorList the errorList to set
     */
    public void setErrorList(List<String> errorList) {
        this.errorList = errorList;
    }


}