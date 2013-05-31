package org.sistemavotacion.modelo;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Certificado {

    private Long id;
    private BigInteger numeroSerie;
    private byte[] contenido;
    private Boolean valido;
    private Date dateCreated;
    private Date lastUpdated;

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

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
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
