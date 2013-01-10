package org.sistemavotacion.modelo;

import java.io.File;
import java.io.Serializable;
import java.security.KeyStore;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class AlmacenClaves implements Serializable {
	
    private static final long serialVersionUID = 1L;

    private Long id;
    private Evento evento;
    private byte[] hashEncabezado;
    private String hashEncabezadoSolicitudTokenBase64;
    private File archivoAlmacen;
    private static KeyStore almacenClaves;

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

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public Evento getEvento() {
        return evento;
    }

    /**
     * @return the hashEncabezadoSolicitudTokenBase64
     */
    public String getHashEncabezadoSolicitudTokenBase64() {
        return hashEncabezadoSolicitudTokenBase64;
    }

    /**
     * @param hashEncabezadoSolicitudTokenBase64 the hashEncabezadoSolicitudTokenBase64 to set
     */
    public void setHashEncabezadoSolicitudTokenBase64(String hashEncabezadoSolicitudTokenBase64) {
        this.hashEncabezadoSolicitudTokenBase64 = hashEncabezadoSolicitudTokenBase64;
    }

    /**
     * @return the archivoAlmacen
     */
    public File getArchivoAlmacen() {
        return archivoAlmacen;
    }

    /**
     * @param archivoAlmacen the archivoAlmacen to set
     */
    public void setArchivoAlmacen(File archivoAlmacen) {
        this.archivoAlmacen = archivoAlmacen;
    }
    
    /**
     * @return the almacenClaves
     */
    public static KeyStore getAlmacenClaves() {
        return almacenClaves;
    }

    /**
     * @param aAlmacenClaves the almacenClaves to set
     */
    public static void setAlmacenClaves(KeyStore aAlmacenClaves) {
        almacenClaves = aAlmacenClaves;
    }
    
    
}
