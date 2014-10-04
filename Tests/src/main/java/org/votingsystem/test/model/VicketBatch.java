package org.votingsystem.test.model;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.StringUtils;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VicketBatch extends BatchRequest implements Serializable  {

    private static Logger log = Logger.getLogger(VicketBatch.class);

    public static final long serialVersionUID = 1L;

    private MessageSMIME messageSMIME;
    private VicketTagVS tag;

    private Map<String, Vicket> vicketsMap;
    private VicketServer vicketServer;
    private BigDecimal requestAmount;
    private BigDecimal vicketsValue;
    private String currencyCode;
    private List<Map> vicketCSRList;
    private String tagVS;

    public VicketBatch() {}

    public VicketBatch(byte[] vicketBatchRequest, MessageSMIME messageSMIME, String localServer) throws Exception {
        this.messageSMIME = messageSMIME;
        SMIMEMessage smimeMessage = messageSMIME.getSmimeMessage();
        JSONObject vicketRequest = (JSONObject) JSONSerializer.toJSON(smimeMessage.getSignedContent());
        if(TypeVS.VICKET_REQUEST != TypeVS.valueOf(vicketRequest.getString("operation"))) throw new ExceptionVS(
                "Request operation '" + vicketRequest.getString("operation") + "' doesn't match VICKET_REQUEST");
        this.requestAmount = new BigDecimal(vicketRequest.getString("totalAmount"));
        this.currencyCode = vicketRequest.getString("currencyCode");
        this.tagVS = vicketRequest.getString("tagVS");
        VicketServer vicketServer = new VicketServer();
        vicketServer.setServerURL(vicketRequest.getString("serverURL"));
        if(!localServer.equals(vicketServer.getServerURL())) throw new ExceptionVS("The server from request '" +
                vicketServer.getServerURL() + "' doesn't match local server '" + localServer + "'");
        JSONArray vicketCsrRequest = (JSONArray) JSONSerializer.toJSON(new String(vicketBatchRequest, "UTF-8")); //
        BigDecimal csrRequestAmount = BigDecimal.ZERO;
        vicketsMap = new HashMap<String, Vicket>();
        for(int i = 0; i < vicketCsrRequest.size(); i++) {
            JSONObject vicketRequestJSON = (JSONObject) vicketCsrRequest.get(i);
            BigDecimal vicketValue = new BigDecimal(vicketRequestJSON.getString("vicketValue"));
            String currencyCode = vicketRequestJSON.getString("currencyCode");
            PKCS10CertificationRequest csr = CertUtil.fromPEMToPKCS10CertificationRequest(
                    vicketRequestJSON.getString("csr").getBytes());
            CertificationRequestInfo info = csr.getCertificationRequestInfo();
            Enumeration csrAttributes = info.getAttributes().getObjects();
            JSONObject certAttributeJSON = null;
            while(csrAttributes.hasMoreElements()) {
                DERTaggedObject attribute = (DERTaggedObject)csrAttributes.nextElement();
                switch(attribute.getTagNo()) {
                    case ContextVS.VICKET_TAG:
                        String certAttributeJSONStr = ((DERUTF8String)attribute.getObject()).getString();
                        certAttributeJSON = (JSONObject) JSONSerializer.toJSON(certAttributeJSONStr);
                        break;
                }
            }
            Vicket.CertSubject certSubject = Vicket.getCertSubject(info.getSubject().toString());
            if(certAttributeJSON == null) throw new ExceptionVS("VICKET CSR REQUEST WITHOUT SIGNED ATTRIBUTES");
            String signedVicketServerURL = StringUtils.checkURL(certAttributeJSON.getString("vicketServerURL"));
            String hashCertVSBase64 = certAttributeJSON.getString("hashCertVS");
            BigDecimal signedVicketValue = new BigDecimal(certAttributeJSON.getString("vicketValue"));
            String signedCurrencyCode = certAttributeJSON.getString("currencyCode");
            String signedTagVS = certAttributeJSON.getString("tagVS");
            if(!signedCurrencyCode.equals(certSubject.getCurrencyCode()) || !signedVicketValue.toString().equals(
                    certSubject.getVicketValue()) || !signedVicketServerURL.equals(certSubject.getVicketServerURL())
                    || !signedTagVS.equals(certSubject.gettagVS()))
                throw new ExceptionVS( "Vicket CSR request number '" + i + "' with ERRORS. Subject: '" + info.getSubject().toString() +
                        "'. Signed attributes: '" + certAttributeJSON.toString() + "'");
            if(vicketValue.compareTo(signedVicketValue) != 0 || !currencyCode.equals(signedCurrencyCode) ||
                    !signedTagVS.equals(this.tagVS)) throw new ExceptionVS(
                    "Vicket CSR request number '" + i + "' with ERRORS. JSON request: '" + vicketRequestJSON.toString() +
                            "'. Signed attributes: '" + certAttributeJSON.toString() + "'");
            if (!signedVicketServerURL.equals(localServer))  throw new ExceptionVS("Vicket signed server URL '" +
                    signedVicketServerURL + "' doesn't match local server URL '" + localServer + "'");
            if (hashCertVSBase64 == null) throw new ExceptionVS("Missing hashCertVS on vicket request number '" + i + "' ");
            csrRequestAmount = csrRequestAmount.add(signedVicketValue);
            vicketsMap.put(hashCertVSBase64,
                    new Vicket(csr, signedVicketValue, signedCurrencyCode, hashCertVSBase64, signedVicketServerURL));
        }
        if(requestAmount.compareTo(csrRequestAmount) != 0) throw new ExceptionVS("Vicket batch request with errors. " +
                "Amount signed '" + requestAmount.toString() + "' - total amount in CSRs '" + csrRequestAmount.toString() + "'");
    }

    public VicketBatch(BigDecimal requestAmount, BigDecimal vicketsValue, String currencyCode, VicketTagVS tag,
                       VicketServer vicketServer) throws Exception {
        this.setRequestAmount(requestAmount);
        this.setVicketServer(vicketServer);
        this.setCurrencyCode(currencyCode);
        this.vicketsValue = vicketsValue;
        this.tag = tag == null ? new VicketTagVS(VicketTagVS.WILDTAG):tag;
        this.tagVS = this.tag.getName();
        this.vicketsMap = getVicketBatch(requestAmount,vicketsValue, currencyCode, tag, vicketServer);
        vicketCSRList = new ArrayList<Map>();
        for(Vicket vicket : vicketsMap.values()) {
            vicketCSRList.add(vicket.getCSRDataMap());
        }
    }

    public Map<String, Vicket> getVicketsMap() {
        return vicketsMap;
    }

    public List<String> getIssuedVicketListPEM() throws IOException {
        List<String> result = new ArrayList<String>();
        for(Vicket vicket: vicketsMap.values()) {
            result.add(new String(vicket.getIssuedCertPEM(), "UTF-8"));
        }
        return result;
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

    public BigDecimal getRequestAmount() {
        return requestAmount;
    }

    public void setRequestAmount(BigDecimal requestAmount) {
        this.requestAmount = requestAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public MessageSMIME getMessageSMIME() {
        return messageSMIME;
    }

    public void setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
    }

    public BigDecimal getVicketsValue() {
        return vicketsValue;
    }

    public void setVicketsValue(BigDecimal vicketsValue) {
        this.vicketsValue = vicketsValue;
    }

    public static Map<String, Vicket> getVicketBatch(BigDecimal requestAmount,
                                                     BigDecimal vicketsValue, String currencyCode, VicketTagVS tag, VicketServer vicketServer) {
        Map<String, Vicket> vicketsMap = new HashMap<String, Vicket>();
        BigDecimal numVickets = requestAmount.divide(vicketsValue);
        for(int i = 0; i < numVickets.intValue(); i++) {
            Vicket vicket = new Vicket(vicketServer.getServerURL(), vicketsValue, currencyCode, tag);
            vicketsMap.put(vicket.getHashCertVS(), vicket);
        }
        return vicketsMap;
    }

    public JSONObject getRequestDataToSignJSON() {
        Map smimeContentMap = new HashMap();
        smimeContentMap.put("operation", TypeVS.VICKET_REQUEST.toString());
        smimeContentMap.put("serverURL", vicketServer.getServerURL());
        smimeContentMap.put("totalAmount", requestAmount.toString());
        smimeContentMap.put("currencyCode", currencyCode);
        smimeContentMap.put("tagVS", tagVS);
        smimeContentMap.put("UUID", UUID.randomUUID().toString());
        JSONObject requestJSON = (JSONObject) JSONSerializer.toJSON(smimeContentMap);
        return requestJSON;
    }

    public List<Map> getVicketCSRList () {
        return vicketCSRList;
    }

    public Vicket initVicket(String signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(
                signedCsr.getBytes());
        if(certificates.isEmpty()) throw new ExceptionVS(
                "Unable to init Vicket. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        byte[] vicketExtensionValue = x509Certificate.getExtensionValue(ContextVS.VICKET_OID);
        DERTaggedObject vicketCertDataDER = (DERTaggedObject)
                X509ExtensionUtil.fromExtensionValue(vicketExtensionValue);
        JSONObject vicketCertData = (JSONObject) JSONSerializer.toJSON(((DERUTF8String)
                vicketCertDataDER.getObject()).toString());
        String hashCertVS = vicketCertData.getString("hashCertVS");
        Vicket vicket = vicketsMap.get(hashCertVS);
        vicket.setState(Vicket.State.OK);
        vicket.getCertificationRequest().initSigner(signedCsr.getBytes());
        return vicket;
    }

    public String getTagVS() {
        return tagVS;
    }

    public void setTagVS(String tagVS) {
        this.tagVS = tagVS;
    }

    public VicketTagVS getTag() {
        return tag;
    }

    public void setTag(VicketTagVS tag) {
        this.tag = tag;
    }
}