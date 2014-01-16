package org.votingsystem.model;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.util.encoders.Base64;
import org.bouncycastle2.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.signature.smime.CMSUtils;

import java.io.Serializable;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class UserVS implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {REPRESENTATIVE, USER}

    private Long id;
    private Long numRepresentations;
    private String nif;
    private byte[] imageBytes;
    private String firstName;
    private String lastName;
    private String country;
    private String cn;
    private String URL;
    private String name;
    private String fullName;
    private String organization;
    private String email;
    private String phone;
    private String description;

    private String subjectDN;
    private SignerInformation signer;
    private CertPath certPath;
    private String signedContent;
    private TimeStampToken timeStampToken;
    private Set<CommentVS> commentSet = new HashSet<CommentVS>(0);
    private X509Certificate certificate;
    private CertificateVS certificateCA;


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

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

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

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public void setSigner(SignerInformation signer) {
        this.signer = signer;
    }

    public Set<CommentVS> getCommentSet() {
        return commentSet;
    }

    public void setCommentSet(Set<CommentVS> commentSet) {
        this.commentSet = commentSet;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    public void setTimeStampToken(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
    }

    public void setSignerInformation(SignerInformation signer) { this.signer = signer; }

    public String getPhone() {
        return phone;
    }

    public CertPath getCertPath() {
        return certPath;
    }

    public void setCertPath(CertPath certPath) {
        this.certPath = certPath;
    }

    public String getContentSigned() {
        return signedContent;
    }

    public void setContentSigned(String signedContent) {
        this.signedContent = signedContent;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        if(fullName != null) return fullName;
        else return ((firstName != null)? firstName: "")  + ((lastName != null)? lastName:"");
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Long getNumRepresentations() {
        return numRepresentations;
    }

    public void setNumRepresentations(Long numRepresentations) {
        this.numRepresentations = numRepresentations;
    }

    public static UserVS populate(JSONObject userJSON) throws ParseException, JSONException {
        UserVS userVS = new UserVS();
        if (userJSON.has("id")) userVS.setId(userJSON.getLong("id"));
        if (userJSON.has("URL")) userVS.setURL(userJSON.getString("URL"));
        if (userJSON.has("nif")) userVS.setNif(userJSON.getString("nif"));
        if (userJSON.has("numRepresentations")) userVS.setNumRepresentations(
                userJSON.getLong("numRepresentations"));
        if (userJSON.has("name")) userVS.setName(userJSON.getString("name"));
        if (userJSON.has("firstName"))  userVS.setFirstName(userJSON.getString("firstName"));
        if (userJSON.has("lastName"))  userVS.setLastName(userJSON.getString("lastName"));
        if (userJSON.has("description")) userVS.setDescription(userJSON.getString("description"));
        return userVS;
    }

}