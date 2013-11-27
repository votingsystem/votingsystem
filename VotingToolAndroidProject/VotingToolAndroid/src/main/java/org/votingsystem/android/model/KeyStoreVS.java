package org.votingsystem.android.model;

import org.votingsystem.model.EventVS;

import java.io.Serializable;
import java.security.KeyStore;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class KeyStoreVS implements Serializable {
	
    private static final long serialVersionUID = 1L;

    private Long id;
    private EventVS eventVS;
    private byte[] hashEncabezado;
    private String hashEncabezadoSolicitudTokenBase64;
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

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public EventVS getEventVS() {
        return eventVS;
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
