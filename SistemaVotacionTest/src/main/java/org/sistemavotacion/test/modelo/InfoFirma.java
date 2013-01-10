package org.sistemavotacion.test.modelo;

import org.sistemavotacion.modelo.Evento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class InfoFirma {
    
    private static Logger logger = LoggerFactory.getLogger(InfoFirma.class);

    private int codigoEstado = 400;
    private String mensaje;
    
    private Evento evento;
    private String from;
    private String userPath;
    private Respuesta respuesta;

    
    public InfoFirma(Evento evento, String from) {
        this.evento = evento;
        this.from = from;
    }

    /**
     * @return the from
     */
    public String getFrom() {
        return from;
    }

    /**
     * @param from the from to set
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * @return the userPath
     */
    public String getUserPath() {
        return userPath;
    }

    /**
     * @param userPath the userPath to set
     */
    public void setUserPath(String userPath) {
        this.userPath = userPath;
    }

    /**
     * @return the codigoEstado
     */
    public int getCodigoEstado() {
        return codigoEstado;
    }

    /**
     * @param codigoEstado the codigoEstado to set
     */
    public void setCodigoEstado(int codigoEstado) {
        this.codigoEstado = codigoEstado;
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

    /**
     * @return the respuesta
     */
    public Respuesta getRespuesta() {
        return respuesta;
    }

    /**
     * @param respuesta the respuesta to set
     */
    public void setRespuesta(Respuesta respuesta) {
        this.respuesta = respuesta;
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
 
}
