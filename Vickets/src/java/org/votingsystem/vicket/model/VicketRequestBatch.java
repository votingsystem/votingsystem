package org.votingsystem.vicket.model;

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

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("VicketRequestBatch")
public class VicketRequestBatch extends BatchRequest implements Serializable  {

    private static Logger logger = Logger.getLogger(VicketRequestBatch.class);

    public static final long serialVersionUID = 1L;

    @OneToOne private MessageSMIME messageSMIME;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="tag", nullable=false) private VicketTagVS tag;

    @Transient private Map<String, Vicket> vicketsMap;
    @Transient private VicketServer vicketServer;
    @Transient private BigDecimal requestAmount;
    @Transient private BigDecimal vicketsValue;
    @Transient private String currencyCode;
    @Transient private List<Map> vicketCSRList;
    @Transient private String tagVS;

    public VicketRequestBatch() {}

    public VicketRequestBatch(byte[] vicketBatchRequest, MessageSMIME messageSMIME, String localServer) throws Exception {
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
            String csrTagVS = vicketRequestJSON.getString("tagVS");
            if(!this.tagVS.equals(csrTagVS)) throw new ExceptionVS("Request is for tag '" + this.tagVS +
                    "' and request number '" + i + "' is for tag '" + csrTagVS + "'");
            PKCS10CertificationRequest csr = CertUtil.fromPEMToPKCS10CertificationRequest(
                    vicketRequestJSON.getString("csr").getBytes());
            Vicket vicket = new Vicket(csr);
            if(vicketValue.compareTo(vicket.getAmount()) != 0 || !currencyCode.equals(vicket.getCurrencyCode()) ||
                    !csrTagVS.equals(vicket.getSignedTagVS())) throw new ExceptionVS(
                    "Vicket CSR request number '" + i + "' with ERRORS. JSON request: '" + vicketRequestJSON.toString() +
                            "'. Cert extension data: '" + vicket.getCertExtensionData().toString() + "'");
            if (!localServer.equals(vicket.getVicketServerURL()))  throw new ExceptionVS("Vicket signed server URL '" +
                    vicket.getVicketServerURL() + "' doesn't match local server URL '" + localServer + "'");
            csrRequestAmount = csrRequestAmount.add(vicket.getAmount());
            vicketsMap.put(vicket.getHashCertVS(), vicket);
        }
        if(requestAmount.compareTo(csrRequestAmount) != 0) throw new ExceptionVS("Vicket batch request with errors. " +
            "Amount signed '" + requestAmount.toString() + "' - total amount in CSRs '" + csrRequestAmount.toString() + "'");
    }

    public VicketRequestBatch(BigDecimal requestAmount, BigDecimal vicketsValue, String currencyCode, VicketTagVS tag,
                              VicketServer vicketServer) throws Exception {
        this.setRequestAmount(requestAmount);
        this.setVicketServer(vicketServer);
        this.setCurrencyCode(currencyCode);
        this.tag = tag == null ? new VicketTagVS(VicketTagVS.WILDTAG):tag;
        this.tagVS = this.tag.getName();
        this.vicketsValue = vicketsValue;
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
        if(certificates.isEmpty()) throw new ExceptionVS("Unable to init Vicket. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        JSONObject certExtensionData = CertUtil.getCertExtensionData(x509Certificate, ContextVS.VICKET_OID);
        Vicket vicket = vicketsMap.get(certExtensionData.getString("hashCertVS")).setState(Vicket.State.OK);
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
        for(Vicket vicket : vicketsMap.values()) {
            vicket.setTag(tag);
        }
    }

    public TransactionVS getTransactionVS(String subject) {
        TransactionVS transaction = new TransactionVS();
        transaction.setAmount(requestAmount);
        transaction.setState(TransactionVS.State.OK);
        transaction.setCurrencyCode(currencyCode);
        transaction.setTag(tag);
        transaction.setSubject(subject);
        transaction.setMessageSMIME(messageSMIME);
        transaction.setType(TransactionVS.Type.VICKET_REQUEST);
        transaction.setFromUserVS(messageSMIME.getUserVS());
        return transaction;
    }

}