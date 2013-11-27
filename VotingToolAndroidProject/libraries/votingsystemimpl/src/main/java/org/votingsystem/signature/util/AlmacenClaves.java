package org.votingsystem.signature.util;

import java.util.Date;


public class AlmacenClaves {
	
    private Boolean valid;
    private Boolean isRoot;
    private Date dateCreated;
    private Date lastUpdated;
    private Date validFrom;
    private Date validTo;
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

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setIsRoot(Boolean isRoot) {
        this.isRoot = isRoot;
    }

    public Boolean getIsRoot() {
            return isRoot;
    }

    private void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    private Date getLastUpdated() {
        return lastUpdated;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Date getValidTo() {
        return validTo;
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
