package org.sistemavotacion.test.modelo;

import org.sistemavotacion.modelo.Evento;
import org.sistemavotacion.modelo.ReciboVoto;
import org.sistemavotacion.seguridad.PKCS10WrapperClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class InfoVoto {
    
    private static Logger logger = LoggerFactory.getLogger(InfoVoto.class);

    private int codigoEstado = 400;
    private String mensaje;
    
    private PKCS10WrapperClient pkcs10WrapperClient;
    private Evento voto;
    private String from;
    private String userPath;
    private ReciboVoto reciboVoto;
    private boolean errorControlAcceso = false;

    
    public InfoVoto(Evento evento, String from) {
        this.voto = evento;
        this.from = from;
    }
    
    /**
     * @return the pkcs10WrapperClient
     */
    public PKCS10WrapperClient getPkcs10WrapperClient() {
        return pkcs10WrapperClient;
    }

    /**
     * @param pkcs10WrapperClient the pkcs10WrapperClient to set
     */
    public void setPkcs10WrapperClient(PKCS10WrapperClient pkcs10WrapperClient) {
        this.pkcs10WrapperClient = pkcs10WrapperClient;
    }

    /**
     * @return the voto
     */
    public Evento getVoto() {
        return voto;
    }

    /**
     * @param voto the voto to set
     */
    public void setVoto(Evento voto) {
        this.voto = voto;
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
     * @return the errorControlAcceso
     */
    public boolean isErrorControlAcceso() {
        return errorControlAcceso;
    }

    /**
     * @param errorControlAcceso the errorControlAcceso to set
     */
    public void setErrorControlAcceso(boolean errorControlAcceso) {
        this.errorControlAcceso = errorControlAcceso;
    }
 
}
