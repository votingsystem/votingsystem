package org.sistemavotacion.test.modelo;

import java.io.File;
import java.io.Serializable;
import java.security.KeyStore;
import java.sql.Clob;
import java.util.Date;

import org.sistemavotacion.modelo.Usuario;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class SolicitudAcceso implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Tipo tipo;
    private Clob contenido;
    private String hashCertificadoVotoBase64;
    private String hashSolicitudAccesoBase64;
    private String OrigenHashSolicitudAcceso;
    private String OrigenHashCertificadoVoto;
    private String userNif;
    private String eventoURL;
    private String eventoId;
    private boolean firmaCorrecta;
    private Usuario usuario;
    private byte[] hashEncabezado;
    private Date dateCreated;
    private File archivo;
    private KeyStore userKeyStore;

    
    public SolicitudAcceso() { }
        
    public SolicitudAcceso(boolean firmaCorrecta) {
        this.firmaCorrecta = firmaCorrecta;
    }
    
    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public Clob getContenido () {
        return contenido;
    }

    public void setContenido (Clob contenido) {
        this.contenido = contenido;
    }

     /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setHashEncabezado(byte[] hashEncabezado) {
        this.hashEncabezado = hashEncabezado;
    }

    public byte[] getHashEncabezado() {
        return hashEncabezado;
    }

    /**
     * @return the hashCertificadoVotoBase64
     */
    public String getHashCertificadoVotoBase64() {
        return hashCertificadoVotoBase64;
    }

    /**
     * @param hashCertificadoVotoBase64 the hashCertificadoVotoBase64 to set
     */
    public void setHashCertificadoVotoBase64(String hashCertificadoVotoBase64) {
        this.hashCertificadoVotoBase64 = hashCertificadoVotoBase64;
    }

    /**
     * @return the firmaCorrecta
     */
    public boolean isFirmaCorrecta() {
        return firmaCorrecta;
    }

    /**
     * @param firmaCorrecta the firmaCorrecta to set
     */
    public void setFirmaCorrecta(boolean firmaCorrecta) {
        this.firmaCorrecta = firmaCorrecta;
    }

    /**
     * @return the hashSolicitudAccesoBase64
     */
    public String getHashSolicitudAccesoBase64() {
        return hashSolicitudAccesoBase64;
    }

    /**
     * @param hashSolicitudAccesoBase64 the hashSolicitudAccesoBase64 to set
     */
    public void setHashSolicitudAccesoBase64(String hashSolicitudAccesoBase64) {
        this.hashSolicitudAccesoBase64 = hashSolicitudAccesoBase64;
    }

    /**
     * @return the eventoURL
     */
    public String getEventoURL() {
        return eventoURL;
    }

    /**
     * @param eventoURL the eventoURL to set
     */
    public void setEventoURL(String eventoURL) {
        this.eventoURL = eventoURL;
    }

    /**
     * @return the userKeyStore
     */
    public KeyStore getUserKeyStore() {
        return userKeyStore;
    }

    /**
     * @param userKeyStore the userKeyStore to set
     */
    public void setUserKeyStore(KeyStore userKeyStore) {
        this.userKeyStore = userKeyStore;
    }

    /**
     * @return the OrigenHashCertificadoVoto
     */
    public String getOrigenHashCertificadoVoto() {
        return OrigenHashCertificadoVoto;
    }

    /**
     * @param OrigenHashCertificadoVoto the OrigenHashCertificadoVoto to set
     */
    public void setOrigenHashCertificadoVoto(String OrigenHashCertificadoVoto) {
        this.OrigenHashCertificadoVoto = OrigenHashCertificadoVoto;
    }

    /**
     * @return the OrigenHashSolicitudAcceso
     */
    public String getOrigenHashSolicitudAcceso() {
        return OrigenHashSolicitudAcceso;
    }

    /**
     * @param OrigenHashSolicitudAcceso the OrigenHashSolicitudAcceso to set
     */
    public void setOrigenHashSolicitudAcceso(String OrigenHashSolicitudAcceso) {
        this.OrigenHashSolicitudAcceso = OrigenHashSolicitudAcceso;
    }

    /**
     * @return the userNif
     */
    public String getUserNif() {
        return userNif;
    }

    /**
     * @param userNif the userNif to set
     */
    public void setUserNif(String userNif) {
        this.userNif = userNif;
    }

    /**
     * @return the eventoId
     */
    public String getEventoId() {
        return eventoId;
    }

    /**
     * @param eventoId the eventoId to set
     */
    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
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

}
