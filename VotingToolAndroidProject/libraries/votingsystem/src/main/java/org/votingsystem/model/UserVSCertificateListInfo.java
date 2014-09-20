package org.votingsystem.model;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.signature.util.CertUtil;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserVSCertificateListInfo {

    public static final String TAG = UserVSCertificateListInfo.class.getSimpleName();

    private Map<String, X509Certificate> certificateMap;

    public UserVSCertificateListInfo() { }

    public Map<String, X509Certificate> getCertificateMap() {
        return certificateMap;
    }

    public void setCertificateMap(Map<String, X509Certificate> certificateMap) {
        this.certificateMap = certificateMap;
    }

    public static UserVSCertificateListInfo parse(JSONArray jsonArrayData)
            throws Exception {
        UserVSCertificateListInfo result = new UserVSCertificateListInfo();
        Map certificateMap = new HashMap<String, X509Certificate>();
        for(int i = 0; i < jsonArrayData.length(); i++) {
            JSONObject certInfo = (JSONObject) jsonArrayData.get(i);
            String serialNumber = certInfo.getString("serialNumber");
            String pemCert = certInfo.getString("pemCert");
            X509Certificate x509Cert = CertUtil.fromPEMToX509Cert(pemCert.getBytes("UTF-8"));
            certificateMap.put(pemCert, x509Cert);
        }
        result.setCertificateMap(certificateMap);
        return result;
    }

}
