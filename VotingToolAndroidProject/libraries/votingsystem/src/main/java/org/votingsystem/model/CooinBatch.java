package org.votingsystem.model;

import android.util.Log;

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
public class CooinBatch {

    public static final String TAG = CooinBatch.class.getSimpleName();

    private Map<String, Cooin> cooinsMap;
    private CooinServer cooinServer;
    private String subject;
    private BigDecimal totalAmount;
    private BigDecimal cooinsValue;
    private String currencyCode;
    private Boolean isTimeLimited;
    private List<Map> cooinCSRList;
    private String tagVS;

    public CooinBatch(BigDecimal totalAmount, BigDecimal cooinsValue, String currencyCode,
               String tagVS, Boolean isTimeLimited, CooinServer cooinServer) throws Exception {
        this.setTotalAmount(totalAmount);
        this.setCooinServer(cooinServer);
        this.setCurrencyCode(currencyCode);
        this.cooinsValue = cooinsValue;
        this.isTimeLimited = isTimeLimited;
        this.tagVS = (tagVS == null)? TagVS.WILDTAG:tagVS;
    }

    public static CooinBatch getRequestBatch(BigDecimal totalAmount, BigDecimal cooinsValue,
            String currencyCode, String tagVS, Boolean isTimeLimited, CooinServer cooinServer)
            throws Exception {
        CooinBatch result = new CooinBatch(totalAmount, cooinsValue, currencyCode, tagVS,
                isTimeLimited, cooinServer);
        Map<String, Cooin> cooinsMap = new HashMap<String, Cooin>();
        BigDecimal numCooins = totalAmount.divide(cooinsValue);
        for(int i = 0; i < numCooins.intValue(); i++) {
            Cooin cooin = new Cooin(cooinServer.getServerURL(),
                    cooinsValue, currencyCode, tagVS, TypeVS.COOIN);
            cooinsMap.put(cooin.getHashCertVS(), cooin);
        }
        List<Map> cooinCSRList = new ArrayList<Map>();
        for(Cooin cooin : cooinsMap.values()) {
            Map csrCooinMap = new HashMap();
            csrCooinMap.put("currencyCode", currencyCode);
            csrCooinMap.put("tag", tagVS);
            csrCooinMap.put("cooinValue", cooinsValue.toString());
            csrCooinMap.put("csr", new String(cooin.getCertificationRequest().getCsrPEM(), "UTF-8"));
            cooinCSRList.add(csrCooinMap);
        }
        result.cooinsMap = cooinsMap;
        result.cooinCSRList = cooinCSRList;
        return result;
    }

    public static CooinBatch getAnonymousSignedTransactionBatch(BigDecimal totalAmount,
            String currencyCode, String tagVS, Boolean isTimeLimited,
            CooinServer cooinServer) throws Exception {
        CooinBatch result = new CooinBatch(totalAmount, null, currencyCode, tagVS,
                isTimeLimited, cooinServer);
        return result;
    }

    public Map<String, Cooin> getCooinsMap() {
        return cooinsMap;
    }

    public void setCooinsMap(Map<String, Cooin> cooinsMap) {
        this.cooinsMap = cooinsMap;
    }

    public CooinServer getCooinServer() {
        return cooinServer;
    }

    public void setCooinServer(CooinServer cooinServer) {
        this.cooinServer = cooinServer;
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

    public BigDecimal getCooinsValue() {
        return cooinsValue;
    }

    public void setCooinsValue(BigDecimal cooinsValue) {
        this.cooinsValue = cooinsValue;
    }

    public JSONObject getRequestDataToSignJSON() {
        Map smimeContentMap = new HashMap();
        smimeContentMap.put("operation", TypeVS.COOIN_REQUEST.toString());
        smimeContentMap.put("serverURL", cooinServer.getServerURL());
        smimeContentMap.put("subject", subject);
        smimeContentMap.put("totalAmount", totalAmount.toString());
        smimeContentMap.put("currencyCode", currencyCode);
        smimeContentMap.put("isTimeLimited", isTimeLimited);
        smimeContentMap.put("tag", tagVS);
        smimeContentMap.put("UUID", UUID.randomUUID().toString());
        JSONObject requestJSON = new JSONObject(smimeContentMap);
        return requestJSON;
    }

    public List<Map> getCooinCSRList () {
        return cooinCSRList;
    }

    public Cooin initCooin(String signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(
                signedCsr.getBytes());
        if(certificates.isEmpty()) throw new ExceptionVS(
                "Unable to init Cooin. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        JSONObject certExtensionData = CertUtils.getCertExtensionData(x509Certificate, ContextVS.COOIN_OID);
        Cooin cooin = cooinsMap.get(certExtensionData.getString("hashCertVS")).setState(Cooin.State.OK);
        cooin.initSigner(signedCsr.getBytes());
        return cooin;
    }

    public String getTagVS() {
        return tagVS;
    }

    public void setTagVS(String tagVS) {
        this.tagVS = tagVS;
    }

    public void initCooins(JSONArray issuedCooinsArray) throws Exception {
        Log.d(TAG + ".initCooins", "num cooins: " + issuedCooinsArray.length());
        if(issuedCooinsArray.length() != cooinsMap.size()) {
            Log.d(TAG + ".initCooins", "num cooins requested: " + cooinsMap.size() +
                    " - num. cooins received: " + issuedCooinsArray.length());
        }
        for(int i = 0; i < issuedCooinsArray.length(); i++) {
            Cooin cooin = initCooin(issuedCooinsArray.getString(i));
            cooinsMap.put(cooin.getHashCertVS(), cooin);
        }
    }
}
