package org.votingsystem.model;

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
public class CertificateVS {

    public enum State {OK, ERROR, CANCELLED, USED, UNKNOWN}

    private Long id;
    private BigInteger serialNumber;
    private byte[] content;
    private Date dateCreated;
    private Date lastUpdated;

    /**
     * @return the content
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(byte[] content) {
        this.content = content;
    }

    /**
     * @return the id
     */
    public BigInteger getSerialNumber() {
        return serialNumber;
    }

    /**
     * @param serialNumber the id to set
     */
    public void setSerialNumber(BigInteger serialNumber) {
        this.serialNumber = serialNumber;
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

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public X509Certificate cargarCertificado () throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        //FileInputStream fileInputStream = new FileInputStream("CertFirmaDigital.der.cer");
        ByteArrayInputStream bais = new ByteArrayInputStream(content);
        Collection<X509Certificate> certificateChain =
                (Collection<X509Certificate>) certificateFactory.generateCertificates(bais);
        X509Certificate cert = certificateChain.iterator().next();
        return cert;
    }
}
