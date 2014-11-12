package org.votingsystem.vicket.model;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.ExceptionVS;

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

    private static Logger log = Logger.getLogger(VicketRequestBatch.class);

    public static final long serialVersionUID = 1L;

    @OneToOne private MessageSMIME messageSMIME;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;
    @Column(name="isTimeLimited") private Boolean isTimeLimited;


    @Transient private Map<String, Vicket> vicketsMap;
    @Transient private VicketServer vicketServer;
    @Transient private BigDecimal requestAmount;
    @Transient private BigDecimal vicketsValue;
    @Transient private String currencyCode;
    @Transient private List<Map> vicketCSRList;
    @Transient private String tag;
    @Transient private String subject;

    public VicketRequestBatch() {}

    public VicketRequestBatch(byte[] vicketBatchRequest, MessageSMIME messageSMIME, String localServer) throws Exception {
        this.messageSMIME = messageSMIME;
        SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        JSONObject vicketRequest = (JSONObject) JSONSerializer.toJSON(smimeMessage.getSignedContent());
        if(TypeVS.VICKET_REQUEST != TypeVS.valueOf(vicketRequest.getString("operation"))) throw new ExceptionVS(
                "Request operation '" + vicketRequest.getString("operation") + "' doesn't match VICKET_REQUEST");
        this.requestAmount = new BigDecimal(vicketRequest.getString("totalAmount"));
        this.currencyCode = vicketRequest.getString("currencyCode");
        this.subject = vicketRequest.getString("subject");
        this.isTimeLimited = vicketRequest.getBoolean("isTimeLimited");
        this.tag = vicketRequest.getString("tag");
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
            String csrTagVS = vicketRequestJSON.getString("tag");
            if(!this.tag.equals(csrTagVS)) throw new ExceptionVS("Request is for tag '" + this.tag +
                    "' and request number '" + i + "' is for tag '" + csrTagVS + "'");
            PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(
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

    public VicketRequestBatch(BigDecimal requestAmount, BigDecimal vicketsValue, String currencyCode, TagVS tagVS,
              Boolean isTimeLimited, VicketServer vicketServer) throws Exception {
        this.setRequestAmount(requestAmount);
        this.setVicketServer(vicketServer);
        this.setCurrencyCode(currencyCode);
        this.isTimeLimited = isTimeLimited;
        this.tagVS = tagVS == null ? new TagVS(TagVS.WILDTAG):tagVS;
        this.tag = this.tagVS.getName();
        this.vicketsValue = vicketsValue;
        this.vicketsMap = getVicketBatch(requestAmount,vicketsValue, currencyCode, tagVS, isTimeLimited, vicketServer);
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

    public void initVickets(JSONArray issuedVicketsArray) throws Exception {
        log.debug("VicketRequest - Num IssuedVickets: " + issuedVicketsArray.size());
        if(issuedVicketsArray.size() != vicketsMap.size()) {
            log.error("VicketRequest(...) - ERROR - Num vickets requested: " + vicketsMap.size() +
                    " - num. vickets received: " + issuedVicketsArray.size());
        }
        for(int i = 0; i < issuedVicketsArray.size(); i++) {
            Vicket vicket = initVicket(issuedVicketsArray.getString(i));
            vicketsMap.replace(vicket.getHashCertVS(), vicket);
        }
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

    public static Map<String, Vicket> getVicketBatch(BigDecimal requestAmount, BigDecimal vicketsValue,
                 String currencyCode, TagVS tag, Boolean isTimeLimited,  VicketServer vicketServer) {
        Map<String, Vicket> vicketsMap = new HashMap<String, Vicket>();
        BigDecimal numVickets = requestAmount.divide(vicketsValue);
        for(int i = 0; i < numVickets.intValue(); i++) {
            Vicket vicket = new Vicket(vicketServer.getServerURL(), vicketsValue, currencyCode, tag);
            vicket.setIsTimeLimited(isTimeLimited);
            vicketsMap.put(vicket.getHashCertVS(), vicket);
        }
        return vicketsMap;
    }

    public JSONObject getRequestDataToSignJSON() {
        Map smimeContentMap = new HashMap();
        smimeContentMap.put("operation", TypeVS.VICKET_REQUEST.toString());
        smimeContentMap.put("subject", subject);
        smimeContentMap.put("serverURL", vicketServer.getServerURL());
        smimeContentMap.put("totalAmount", requestAmount.toString());
        smimeContentMap.put("currencyCode", currencyCode);
        smimeContentMap.put("isTimeLimited", isTimeLimited);
        smimeContentMap.put("tag", tag);
        smimeContentMap.put("UUID", UUID.randomUUID().toString());
        JSONObject requestJSON = (JSONObject) JSONSerializer.toJSON(smimeContentMap);
        return requestJSON;
    }

    public List<Map> getVicketCSRList () {
        return vicketCSRList;
    }

    public Vicket initVicket(String signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(
                signedCsr.getBytes());
        if(certificates.isEmpty()) throw new ExceptionVS("Unable to init Vicket. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        JSONObject certExtensionData = CertUtils.getCertExtensionData(x509Certificate, ContextVS.VICKET_OID);
        Vicket vicket = vicketsMap.get(certExtensionData.getString("hashCertVS")).setState(Vicket.State.OK);
        vicket.initSigner(signedCsr.getBytes());
        return vicket;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public TagVS getTagVS() {
        return tagVS;
    }

    public void setTagVS(TagVS tagVS) {
        this.tagVS = tagVS;
        for(Vicket vicket : vicketsMap.values()) {
            vicket.setTag(tagVS);
        }
    }

    public JSONArray getVicketCSRRequest() {
        return (JSONArray) JSONSerializer.toJSON(getVicketCSRList());
    }

    public TransactionVS getTransactionVS(String subject, Map<UserVSAccount, BigDecimal> accountFromMovements) {
        TransactionVS transaction = new TransactionVS();
        transaction.setAmount(requestAmount);
        transaction.setState(TransactionVS.State.OK);
        transaction.setCurrencyCode(currencyCode);
        transaction.setTag(tagVS);
        transaction.setSubject(subject);
        transaction.setMessageSMIME(messageSMIME);
        transaction.setType(TransactionVS.Type.VICKET_REQUEST);
        transaction.setFromUserVS(messageSMIME.getUserVS());
        transaction.setAccountFromMovements(accountFromMovements);
        return transaction;
    }

    public Boolean getIsTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

}