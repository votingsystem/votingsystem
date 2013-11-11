package org.votingsystem.signature.util;

import java.util.Date;


public class AlmacenClaves {
	
    private Boolean activo;
    private Boolean esRaiz;
    private Date dateCreated;
    private Date lastUpdated;
    private Date validoDesde;
    private Date validoHasta;
    private String password;
    private String keyAlias;
    private String rootKeyAlias;

    private byte[] bytes;

    
   public AlmacenClaves() { }

   public byte[] getBytes() {
       return this.bytes;
   }
   
   public void setBytes(byte[] bytes) {
       this.bytes = bytes;
   }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setEsRaiz(Boolean esRaiz) {
        this.esRaiz = esRaiz;
    }

    public Boolean getEsRaiz() {
            return esRaiz;
    }

    private void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    private Date getLastUpdated() {
        return lastUpdated;
    }

    public void setValidoDesde(Date validoDesde) {
        this.validoDesde = validoDesde;
    }

    public Date getValidoDesde() {
        return validoDesde;
    }

    public void setValidoHasta(Date validoHasta) {
        this.validoHasta = validoHasta;
    }

    public Date getValidoHasta() {
        return validoHasta;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setRootKeyAlias(String rootKeyAlias) {
        this.rootKeyAlias = rootKeyAlias;
    }

    public String getRootKeyAlias() {
        return rootKeyAlias;
    }

}
