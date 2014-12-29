package org.votingsystem.model;

import android.net.Uri;
import android.util.Log;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.cms.SignerInformation;
import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.signature.util.CertUtils;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserVS implements Serializable {

    public static final String TAG = UserVS.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    public enum Type {USER, GROUP, SYSTEM, REPRESENTATIVE, BANKVS, CONTACT}

    public enum State {ACTIVE, PENDING, SUSPENDED, CANCELLED}

    private Long id;
    private Long numRepresentations;
    private String nif;
    private String IBAN;
    private State state;
    private Type type = Type.USER;
    private byte[] imageBytes;
    private String firstName;
    private String lastName;
    private String country;
    private String cn;
    private String URL;
    private String name;
    private String organization;
    private String deviceId;
    private String email;
    private String phone;
    private String reason;
    private String description;
    private transient Uri contactURI;
    private String subjectDN;
    private SignerInformation signer;
    private CertPath certPath;
    private String signedContent;
    private TimeStampToken timeStampToken;
    private Set<CommentVS> commentSet = new HashSet<CommentVS>(0);
    private Map<String, X509Certificate> certificateMap;
    private X509Certificate certificate;

    public UserVS() {}

    public UserVS(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    public static UserVS getUserVS (X509Certificate x509Cert) {
        UserVS userVS = new UserVS();
        userVS.setCertificate(x509Cert);
        String subjectDN = x509Cert.getSubjectDN().getName();
        if (subjectDN.contains("C="))
            userVS.setCountry(subjectDN.split("C=")[1].split(",")[0]);
        if (subjectDN.contains("SERIALNUMBER="))
            userVS.setNif(subjectDN.split("SERIALNUMBER=")[1].split(",")[0]);
        if (subjectDN.contains("SURNAME="))
            userVS.setLastName(subjectDN.split("SURNAME=")[1].split(",")[0]);
        if (subjectDN.contains("GIVENNAME="))
            userVS.setFirstName(subjectDN.split("GIVENNAME=")[1].split(",")[0]);
        if (subjectDN.contains("CN="))
            userVS.setCn(subjectDN.split("CN=")[1]);
        try {
            JSONObject deviceData = CertUtils.getCertExtensionData(x509Cert, ContextVS.DEVICEVS_OID);
            if(deviceData != null) {
                if(deviceData.has("email")) userVS.setEmail(deviceData.getString("email"));
                if(deviceData.has("mobilePhone")) userVS.setPhone(deviceData.getString("mobilePhone"));
                if(deviceData.has("deviceId")) userVS.setDeviceId(deviceData.getString("deviceId"));
            }
        } catch(Exception ex) {ex.printStackTrace();}
        return userVS;
    }

    public Uri getContactURI() {
        return contactURI;
    }

    public void setContactURI(Uri contactURI) {
        this.contactURI = contactURI;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
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
        if(name == null) return ((firstName != null)? firstName + " ": "") + ((lastName != null)? lastName:"");
        else return name;
    }

    public String getLastName() {
        return lastName;
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

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Map<String, X509Certificate> getCertificateMap() {
        return certificateMap;
    }

    public void setCertificateMap(Map<String, X509Certificate> certificateMap) {
        this.certificateMap = certificateMap;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if(contactURI != null) s.writeObject(contactURI.toString());
        else s.writeObject(null);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        try {
            String contactURIStr = (String) s.readObject();
            if(contactURIStr != null) contactURI = Uri.parse(contactURIStr);
        } catch(Exception ex) { Log.d(TAG, "readObject EXCEPTION");}
    }

    public static UserVS parse(JSONObject userJSON) throws Exception {
        UserVS userVS = new UserVS();
        if (userJSON.has("id")) userVS.setId(userJSON.getLong("id"));
        if (userJSON.has("URL")) userVS.setURL(userJSON.getString("URL"));
        if (userJSON.has("nif")) userVS.setNif(userJSON.getString("nif"));
        if (userJSON.has("IBAN"))  userVS.setIBAN(userJSON.getString("IBAN"));
        if (userJSON.has("state"))  userVS.setState(State.valueOf(userJSON.getString("state")));
        if (userJSON.has("type"))  userVS.setType(Type.valueOf(userJSON.getString("type")));
        if (userJSON.has("reason"))  userVS.setReason(userJSON.getString("reason")) ;
        if (userJSON.has("certificateList")) {
            JSONArray jsonArrayData = userJSON.getJSONArray("certificateList");
            Map certificateMap = new HashMap<String, X509Certificate>();
            for(int i = 0; i < jsonArrayData.length(); i++) {
                JSONObject certInfo = (JSONObject) jsonArrayData.get(i);
                String serialNumber = certInfo.getString("serialNumber");
                String pemCert = certInfo.getString("pemCert");
                X509Certificate x509Cert = CertUtils.fromPEMToX509Cert(pemCert.getBytes("UTF-8"));
                certificateMap.put(pemCert, x509Cert);
            }
            userVS.setCertificateMap(certificateMap);
        }
        if (userJSON.has("numRepresentations")) userVS.setNumRepresentations(
                userJSON.getLong("numRepresentations"));
        if (userJSON.has("name")) userVS.setName(userJSON.getString("name"));
        if (userJSON.has("firstName"))  userVS.setFirstName(userJSON.getString("firstName"));
        if (userJSON.has("lastName"))  userVS.setLastName(userJSON.getString("lastName"));
        if (userJSON.has("description")) userVS.setDescription(userJSON.getString("description"));
        return userVS;
    }

    public static List<UserVS> parseList(JSONObject usersJSON) throws Exception {
        List<UserVS> result = new ArrayList<>();
        JSONArray usersArray = null;
        if(usersJSON.has("userVSList")) {
            usersArray = usersJSON.getJSONArray("userVSList");
        } else return Arrays.asList(UserVS.parse(usersJSON));
        for(int i = 0; i < usersArray.length(); i++) {
            result.add(UserVS.parse(usersArray.getJSONObject(i)));
        }
        return result;
    }

    public JSONObject toJSON() throws Exception {
        JSONObject jsonData = new JSONObject();
        jsonData.put("id", id);
        jsonData.put("URL", URL);
        jsonData.put("nif", nif);
        jsonData.put("IBAN", IBAN);
        if(state != null) jsonData.put("state", state.toString());
        if(type != null) jsonData.put("type", type.toString());
        if(reason != null) jsonData.put("reason", reason);
        if(certificateMap != null) {
            Set<String> certIdSet = certificateMap.keySet();
            JSONArray jsonArrayData = new JSONArray();
            for(String certId : certIdSet) {
                X509Certificate x509Cert = certificateMap.get(certId);
                JSONObject jsonCertData = new JSONObject();
                jsonCertData.put("serialNumber", certId);
                jsonCertData.put("pemCert", new String(CertUtils.getPEMEncoded(x509Cert), "UTF-8"));
                jsonArrayData.put(jsonCertData);
            }
            jsonData.put("certificateList", jsonArrayData);
        }
        JSONObject deviceData = new JSONObject();
        deviceData.put("phone", getPhone());
        deviceData.put("email", getEmail());
        deviceData.put("deviceId", getDeviceId());
        jsonData.put("deviceData", deviceData);
        jsonData.put("numRepresentations", numRepresentations);
        jsonData.put("name", name);
        jsonData.put("firstName", firstName);
        jsonData.put("lastName", lastName);
        jsonData.put("description", description);
        return jsonData;
    }

}