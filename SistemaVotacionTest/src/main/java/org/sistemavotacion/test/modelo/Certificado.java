package org.sistemavotacion.test.modelo;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;

import org.sistemavotacion.modelo.Usuario;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class Certificado {

    public enum Estado {ACTIVO, PENDIENTE_ACTIVACION}
            
    private Long id;
    private BigInteger numeroSerie;
    private byte[] contenido;
    private Usuario usuario;
    private Boolean valido;
    private Date dateCreated;
    private Date lastUpdated;
    private String telefono;
    private String email;
    private String idDispositivo;
    private Estado estado;

    /**
     * @return the contenido
     */
    public byte[] getContenido() {
        return contenido;
    }

    /**
     * @param contenido the contenido to set
     */
    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }

    /**
     * @return the id
     */
    public BigInteger getNumeroSerie() {
        return numeroSerie;
    }

    /**
     * @param id the id to set
     */
    public void setNumeroSerie(BigInteger numeroSerie) {
        this.numeroSerie = numeroSerie;
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

    /**
     * @return the lastUpdated
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated the lastUpdated to set
     */
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public void setValido(Boolean valido) {
        this.valido = valido;
    }

    public Boolean getValido() {
        return valido;
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

    /**
     * @return the telefono
     */
    public String getTelefono() {
        return telefono;
    }

    /**
     * @param telefono the telefono to set
     */
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the idDispositivo
     */
    public String getIdDispositivo() {
        return idDispositivo;
    }

    /**
     * @param idDispositivo the idDispositivo to set
     */
    public void setIdDispositivo(String idDispositivo) {
        this.idDispositivo = idDispositivo;
    }    
    
    /**
     * @return the estado
     */
    public Estado getEstado() {
        return estado;
    }

    /**
     * @param estado the estado to set
     */
    public void setEstado(Estado estado) {
        this.estado = estado;
    }
    
    public X509Certificate cargarCertificado () throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        //FileInputStream fileInputStream = new FileInputStream("CertFirmaDigital.der.cer");
        ByteArrayInputStream bais = new ByteArrayInputStream(contenido);
        Collection<X509Certificate> certificateChain =
                (Collection<X509Certificate>) certificateFactory.generateCertificates(bais);
        X509Certificate cert = certificateChain.iterator().next();
        return cert;
    }
    
}