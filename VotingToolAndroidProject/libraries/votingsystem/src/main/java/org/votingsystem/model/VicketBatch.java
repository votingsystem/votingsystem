package org.votingsystem.model;

import android.util.Log;

import org.bouncycastle2.asn1.DERTaggedObject;
import org.bouncycastle2.asn1.DERUTF8String;
import org.bouncycastle2.x509.extension.X509ExtensionUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.ExceptionVS;

import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VicketBatch {

    public static final String TAG = VicketBatch.class.getSimpleName();

    private Map<String, Vicket> vicketsMap;
    private VicketServer vicketServer;
    private String subject;
    private BigDecimal totalAmount;
    private BigDecimal vicketsValue;
    private String currencyCode;
    private Boolean isTimeLimited;
    private List<Map> vicketCSRList;
    private String tagVS;

    public VicketBatch(BigDecimal totalAmount, BigDecimal vicketsValue, String currencyCode,
               String tagVS, Boolean isTimeLimited, VicketServer vicketServer) throws Exception {
        this.setTotalAmount(totalAmount);
        this.setVicketServer(vicketServer);
        this.setCurrencyCode(currencyCode);
        this.vicketsValue = vicketsValue;
        this.isTimeLimited = isTimeLimited;
        this.tagVS = (tagVS == null)? TagVS.WILDTAG:tagVS;
        this.vicketsMap = getVicketBatch(totalAmount,vicketsValue, currencyCode, tagVS, vicketServer);
        vicketCSRList = new ArrayList<Map>();
        for(Vicket vicket : vicketsMap.values()) {
            Map csrVicketMap = new HashMap();
            csrVicketMap.put("currencyCode", currencyCode);
            csrVicketMap.put("tag", tagVS);
            csrVicketMap.put("vicketValue", vicketsValue.toString());
            csrVicketMap.put("csr", new String(vicket.getCertificationRequest().getCsrPEM(), "UTF-8"));
            vicketCSRList.add(csrVicketMap);
        }
    }

    public Map<String, Vicket> getVicketsMap() {
        return vicketsMap;
    }

    public void setVicketsMap(Map<String, Vicket> vicketsMap) {
        this.vicketsMap = vicketsMap;
    }

    public VicketServer getVicketServer() {
        return vicketServer;
    }

    public void setVicketServer(VicketServer vicketServer) {
        this.vicketServer = vicketServer;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getVicketsValue() {
        return vicketsValue;
    }

    public void setVicketsValue(BigDecimal vicketsValue) {
        this.vicketsValue = vicketsValue;
    }

    public static Map<String, Vicket> getVicketBatch(BigDecimal requestAmount,
             BigDecimal vicketsValue, String currencyCode, String tagVS, VicketServer vicketServer) {
        Map<String, Vicket> vicketsMap = new HashMap<String, Vicket>();
        BigDecimal numVickets = requestAmount.divide(vicketsValue);
        for(int i = 0; i < numVickets.intValue(); i++) {
            Vicket vicket = new Vicket(vicketServer.getServerURL(),
                    vicketsValue, currencyCode, tagVS, TypeVS.VICKET);
            vicketsMap.put(vicket.getHashCertVS(), vicket);
        }
        return vicketsMap;
    }

    public JSONObject getRequestDataToSignJSON() {
        Map smimeContentMap = new HashMap();
        smimeContentMap.put("operation", TypeVS.VICKET_REQUEST.toString());
        smimeContentMap.put("serverURL", vicketServer.getServerURL());
        smimeContentMap.put("subject", subject);
        smimeContentMap.put("totalAmount", totalAmount.toString());
        smimeContentMap.put("currencyCode", currencyCode);
        smimeContentMap.put("isTimeLimited", isTimeLimited);
        smimeContentMap.put("tag", tagVS);
        smimeContentMap.put("UUID", UUID.randomUUID().toString());
        JSONObject requestJSON = new JSONObject(smimeContentMap);
        return requestJSON;
    }

    public List<Map> getVicketCSRList () {
        return vicketCSRList;
    }

    public Vicket initVicket(String signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(
                signedCsr.getBytes());
        if(certificates.isEmpty()) throw new ExceptionVS(
                "Unable to init Vicket. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        JSONObject certExtensionData = CertUtils.getCertExtensionData(x509Certificate, ContextVS.VICKET_OID);
        Vicket vicket = vicketsMap.get(certExtensionData.getString("hashCertVS")).setState(Vicket.State.OK);
        vicket.initSigner(signedCsr.getBytes());
        return vicket;
    }

    public String getTagVS() {
        return tagVS;
    }

    public void setTagVS(String tagVS) {
        this.tagVS = tagVS;
    }

    public void initVickets(JSONArray issuedVicketsArray) throws Exception {
        Log.d(TAG + ".initVickets", "num vickets: " + issuedVicketsArray.length());
        if(issuedVicketsArray.length() != vicketsMap.size()) {
            Log.d(TAG + ".initVickets", "num vickets requested: " + vicketsMap.size() +
                    " - num. vickets received: " + issuedVicketsArray.length());
        }
        for(int i = 0; i < issuedVicketsArray.length(); i++) {
            Vicket vicket = initVicket(issuedVicketsArray.getString(i));
            vicketsMap.put(vicket.getHashCertVS(), vicket);
        }
    }
}
