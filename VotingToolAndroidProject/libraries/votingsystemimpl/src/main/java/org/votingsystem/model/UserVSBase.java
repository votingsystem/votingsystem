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
public class UserVSBase implements UserVS {

    private static final long serialVersionUID = 1L;

    private String nif;
    private String primerApellido;
    private String pais;
    private String cn;
    private String nombre;
    private String nombreCompleto = "";
    private String organizacion;
    private String email;
    private String telefono;

    private Date fechaFirma;
    private String subjectDN;
    private SignerInformation signer;
    private CertPath certPath;
    private String contenidoFirmado;
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

    public static UserVSBase getUsuario (X509Certificate certificate) {
        UserVSBase userVSBase = new UserVSBase();
        userVSBase.setCertificate(certificate);
        String subjectDN = certificate.getSubjectDN().getName();
        if (subjectDN.contains("C="))
            userVSBase.setPais(subjectDN.split("C=")[1].split(",")[0]);
        if (subjectDN.contains("SERIALNUMBER="))
            userVSBase.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
        if (subjectDN.contains("SURNAME="))
            userVSBase.setPrimerApellido(subjectDN.split("SURNAME=")[1].split(",")[0]);
        if (subjectDN.contains("GIVENNAME="))
            userVSBase.setNombre(subjectDN.split("GIVENNAME=")[1].split(",")[0]);
        if (subjectDN.contains("CN="))
            userVSBase.setCn(subjectDN.split("CN=")[1]);
        return userVSBase;
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

    @Override
    public void setCertificateCA(CertificateVS certificateCA) {
        this.certificateCA = certificateCA;
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

    @Override
    public void setSignerInformation(SignerInformation signer) {

    }

    public String getTelefono() {
        return telefono;
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
     * @return the contenidoFirmado
     */
    public String getContenidoFirmado() {
        return contenidoFirmado;
    }

    /**
     * @param contenidoFirmado the contenidoFirmado to set
     */
    public void setContenidoFirmado(String contenidoFirmado) {
        this.contenidoFirmado = contenidoFirmado;
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

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }


}
