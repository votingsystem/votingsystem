package org.votingsystem.model;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.util.encoders.Base64;
import org.bouncycastle2.util.encoders.Hex;
import org.votingsystem.signature.smime.CMSUtils;

import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class UserVS {

    private static final long serialVersionUID = 1L;

    private String nif;
    private String firstName;
    private String country;
    private String cn;
    private String name;
    private String fullName = "";
    private String organizacion;
    private String email;
    private String phone;

    private Date fechaFirma;
    private String subjectDN;
    private SignerInformation signer;
    private CertPath certPath;
    private String signedContent;
    private TimeStampToken timeStampToken;

    private Set<CommentVS> commentVSes = new HashSet<CommentVS>(0);

    private X509Certificate certificate;
    private CertificateVS certificateCA;

    /**
     * @return the id
     */
    public String getNif() {
        return nif;
    }

    /**
     * @param nif the nif to set
     */
    public void setNif(String nif) {
        this.nif = nif;
    }

    /**
     * @return the commentVSes
     */
    public Set<CommentVS> getCommentVSes() {
        return commentVSes;
    }

    /**
     * @param commentVSes the commentVSes to set
     */
    public void setCommentVSes(Set<CommentVS> commentVSes) {
        this.commentVSes = commentVSes;
    }

    public static UserVS getUserVS (X509Certificate certificate) {
        UserVS userVS = new UserVS();
        userVS.setCertificate(certificate);
        String subjectDN = certificate.getSubjectDN().getName();
        if (subjectDN.contains("C="))
            userVS.setCountry(subjectDN.split("C=")[1].split(",")[0]);
        if (subjectDN.contains("SERIALNUMBER="))
            userVS.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
        if (subjectDN.contains("SURNAME="))
            userVS.setFirstName(subjectDN.split("SURNAME=")[1].split(",")[0]);
        if (subjectDN.contains("GIVENNAME="))
            userVS.setName(subjectDN.split("GIVENNAME=")[1].split(",")[0]);
        if (subjectDN.contains("CN="))
            userVS.setCn(subjectDN.split("CN=")[1]);
        return userVS;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getCn() {
        return cn;
    }

    /**
     * @return the fechaFirma
     */
    public Date getFechaFirma() {
        return fechaFirma;
    }

    /**
     * @param fechaFirma the fechaFirma to set
     */
    public void setFechaFirma(Date fechaFirma) {
        this.fechaFirma = fechaFirma;
    }

    /**
     * @return the subjectDN
     */
    public String getSubjectDN() {
        return subjectDN;
    }

    /**
     * @param subjectDN the subjectDN to set
     */
    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }



    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificateCA(CertificateVS certificateCA) {
        this.certificateCA = certificateCA;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * @return the contentDigest
     */
    public String getContentDigestBase64() {
        if (signer.getContentDigest() == null) return null;
        return new String(Base64.encode(signer.getContentDigest()));
    }

    /**
     * @return the contentDigest
     */
    public String getContentDigestHex() {
        if (signer.getContentDigest() == null) return null;
        return new String(Hex.encode(signer.getContentDigest()));
    }

    /**
     * @return the contentDigest
     */
    public String getFirmaBase64() {
        if (signer.getSignature() == null) return null;
        return new String(Base64.encode(signer.getSignature()));
    }

    /**
     * @return the contentDigest
     */
    public String getFirmaHex() {
        if (signer.getSignature() == null) return null;
        return new String(signer.getSignature());
    }

    /**
     * @param signer the signer to set
     */
    public void setSigner(SignerInformation signer) {
        this.signer = signer;
    }

    public String getEncryptiontId() {
        return CMSUtils.getEncryptiontId(signer.getEncryptionAlgOID());
    }

    public String getDigestId() {
        return CMSUtils.getDigestId(signer.getDigestAlgOID());
    }

    /**
     * @return the timeStampToken
     */
    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    /**
     * @param timeStampToken the timeStampToken to set
     */
    public void setTimeStampToken(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
    }

    public void setSignerInformation(SignerInformation signer) { this.signer = signer; }

    public String getPhone() {
        return phone;
    }

    /**
     * @return the certPath
     */
    public CertPath getCertPath() {
        return certPath;
    }

    /**
     * @param certPath the certPath to set
     */
    public void setCertPath(CertPath certPath) {
        this.certPath = certPath;
    }


    /**
     * @return the signedContent
     */
    public String getContentSigned() {
        return signedContent;
    }

    /**
     * @param signedContent the signedContent to set
     */
    public void setContentSigned(String signedContent) {
        this.signedContent = signedContent;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }


}
