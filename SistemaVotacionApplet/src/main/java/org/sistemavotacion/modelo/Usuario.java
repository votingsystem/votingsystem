package org.sistemavotacion.modelo;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Usuario {

    private static final long serialVersionUID = 1L;

    private String nif;
    private String primerApellido;
    private String pais;
    private String cn;
    private String organizacion;
    private String email;
    private String nombre;
    private String telefono;
    
    private KeyStore keyStore;


    private X509Certificate certificate;

    public Usuario() { }
    
    public Usuario(String nif) {
        this.nif = nif;
    }
    
   /**
     * @return the id
     */
    public String getNif() {
        return nif;
    }

    /**
     * @param id the id to set
     */
    public void setNif(String nif) {
        this.nif = nif;
    }

    public static Usuario getUsuario (X509Certificate certificate) {
    	Usuario usuario = new Usuario();
    	usuario.setCertificate(certificate);
    	String subjectDN = certificate.getSubjectDN().getName();
        if (subjectDN.split("C=").length > 1) {
            usuario.setPais(subjectDN.split("C=")[1].split(",")[0]);
        }
        if (subjectDN.split("SERIALNUMBER=").length > 1)
            usuario.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
        if (subjectDN.split("SURNAME=").length > 1)
            usuario.setPrimerApellido(subjectDN.split("SURNAME=")[1].split(",")[0]);
        if (subjectDN.split("GIVENNAME=").length > 1)
            usuario.setNombre(subjectDN.split("GIVENNAME=")[1].split(",")[0]);
        if (subjectDN.split("O=").length > 1)
             usuario.setOrganizacion(subjectDN.split("O=")[1].split(",")[0]);
        if (subjectDN.split("CN=").length > 1)
            usuario.setCn(subjectDN.split("CN=")[1]);
    	return usuario;
    }

    public void setPais(String pais) {
            this.pais = pais;
    }

    public String getPais() {
            return pais;
    }

    public void setPrimerApellido(String primerApellido) {
            this.primerApellido = primerApellido;
    }

    public String getPrimerApellido() {
            return primerApellido;
    }

    public void setCn(String cn) {
            this.cn = cn;
    }

    public String getCn() {
            return cn;
    }

    public void setCertificate(X509Certificate certificate) {
            this.certificate = certificate;
    }

    public X509Certificate getCertificate() {
            return certificate;
    }

    public void setEmail(String email) {
            this.email = email;
    }

    public String getEmail() {
            return email;
    }

    public void setTelefono(String telefono) {
            this.telefono = telefono;
    }

    public String getTelefono() {
            return telefono;
    }

    /**
     * @return the organizacion
     */
    public String getOrganizacion() {
        return organizacion;
    }

    /**
     * @param organizacion the organizacion to set
     */
    public void setOrganizacion(String organizacion) {
        this.organizacion = organizacion;
    }

    /**
     * @return the nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * @param nombre the nombre to set
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * @return the keyStore
     */
    public KeyStore getKeyStore() {
        return keyStore;
    }

    /**
     * @param keyStore the keyStore to set
     */
    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }
}
