package org.votingsystem.cooin.model;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;

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
@DiscriminatorValue("CooinRequestBatch")
public class CooinRequestBatch extends BatchRequest implements Serializable  {

    private static Logger log = Logger.getLogger(CooinRequestBatch.class);

    public static final long serialVersionUID = 1L;

    @OneToOne private MessageSMIME messageSMIME;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;
    @Column(name="isTimeLimited") private Boolean isTimeLimited;


    @Transient private Map<String, Cooin> cooinsMap;
    @Transient private CooinServer cooinServer;
    @Transient private BigDecimal requestAmount;
    @Transient private BigDecimal cooinsValue;
    @Transient private String currencyCode;
    @Transient private List<Map> cooinCSRList;
    @Transient private String tag;
    @Transient private String subject;

    public CooinRequestBatch() {}

    public CooinRequestBatch(byte[] cooinBatchRequest, MessageSMIME messageSMIME, String localServer) throws Exception {
        this.messageSMIME = messageSMIME;
        SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        JSONObject cooinRequest = (JSONObject) JSONSerializer.toJSON(smimeMessage.getSignedContent());
        if(TypeVS.COOIN_REQUEST != TypeVS.valueOf(cooinRequest.getString("operation"))) throw new ExceptionVS(
                "Request operation '" + cooinRequest.getString("operation") + "' doesn't match COOIN_REQUEST");
        this.requestAmount = new BigDecimal(cooinRequest.getString("totalAmount"));
        this.currencyCode = cooinRequest.getString("currencyCode");
        this.subject = cooinRequest.getString("subject");
        this.isTimeLimited = cooinRequest.getBoolean("isTimeLimited");
        this.tag = cooinRequest.getString("tag");
        CooinServer cooinServer = new CooinServer();
        cooinServer.setServerURL(cooinRequest.getString("serverURL"));
        if(!localServer.equals(cooinServer.getServerURL())) throw new ExceptionVS("The server from request '" +
                cooinServer.getServerURL() + "' doesn't match local server '" + localServer + "'");
        JSONArray cooinCsrRequest = (JSONArray) JSONSerializer.toJSON(new String(cooinBatchRequest, "UTF-8")); //
        BigDecimal csrRequestAmount = BigDecimal.ZERO;
        cooinsMap = new HashMap<String, Cooin>();
        for(int i = 0; i < cooinCsrRequest.size(); i++) {
            JSONObject cooinRequestJSON = (JSONObject) cooinCsrRequest.get(i);
            BigDecimal cooinValue = new BigDecimal(cooinRequestJSON.getString("cooinValue"));
            String currencyCode = cooinRequestJSON.getString("currencyCode");
            String csrTagVS = cooinRequestJSON.getString("tag");
            if(!this.tag.equals(csrTagVS)) throw new ExceptionVS("Request is for tag '" + this.tag +
                    "' and request number '" + i + "' is for tag '" + csrTagVS + "'");
            PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(
                    cooinRequestJSON.getString("csr").getBytes());
            Cooin cooin = new Cooin(csr);
            if(cooinValue.compareTo(cooin.getAmount()) != 0 || !currencyCode.equals(cooin.getCurrencyCode()) ||
                    !csrTagVS.equals(cooin.getCertTagVS())) throw new ExceptionVS(
                    "Cooin CSR request number '" + i + "' with ERRORS. JSON request: '" + cooinRequestJSON.toString() +
                            "'. Cert extension data: '" + cooin.getCertExtensionData().toString() + "'");
            if (!localServer.equals(cooin.getCooinServerURL()))  throw new ExceptionVS("Cooin signed server URL '" +
                    cooin.getCooinServerURL() + "' doesn't match local server URL '" + localServer + "'");
            csrRequestAmount = csrRequestAmount.add(cooin.getAmount());
            cooinsMap.put(cooin.getHashCertVS(), cooin);
        }
        if(requestAmount.compareTo(csrRequestAmount) != 0) throw new ExceptionVS("Cooin batch request with errors. " +
            "Amount signed '" + requestAmount.toString() + "' - total amount in CSRs '" + csrRequestAmount.toString() + "'");
    }

    public CooinRequestBatch(BigDecimal requestAmount, BigDecimal cooinsValue, String currencyCode, TagVS tagVS,
                             Boolean isTimeLimited, CooinServer cooinServer) throws Exception {
        this.setRequestAmount(requestAmount);
        this.setCooinServer(cooinServer);
        this.setCurrencyCode(currencyCode);
        this.isTimeLimited = isTimeLimited;
        this.tagVS = tagVS == null ? new TagVS(TagVS.WILDTAG):tagVS;
        this.tag = this.tagVS.getName();
        this.cooinsValue = cooinsValue;
        this.cooinsMap = getCooinBatch(requestAmount,cooinsValue, currencyCode, tagVS, isTimeLimited, cooinServer);
        cooinCSRList = new ArrayList<Map>();
        for(Cooin cooin : cooinsMap.values()) {
            cooinCSRList.add(cooin.getCSRDataMap());
        }
    }

    public Map<String, Cooin> getCooinsMap() {
        return cooinsMap;
    }

    public List<String> getIssuedCooinListPEM() throws IOException {
        List<String> result = new ArrayList<String>();
        for(Cooin cooin : cooinsMap.values()) {
            result.add(new String(cooin.getIssuedCertPEM(), "UTF-8"));
        }
        return result;
    }

    public void initCooins(JSONArray issuedCooinsArray) throws Exception {
        log.debug("CooinRequest - Num IssuedCooins: " + issuedCooinsArray.size());
        if(issuedCooinsArray.size() != cooinsMap.size()) {
            log.error("CooinRequest(...) - ERROR - Num cooins requested: " + cooinsMap.size() +
                    " - num. cooins received: " + issuedCooinsArray.size());
        }
        for(int i = 0; i < issuedCooinsArray.size(); i++) {
            Cooin cooin = initCooin(issuedCooinsArray.getString(i));
            cooinsMap.replace(cooin.getHashCertVS(), cooin);
        }
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

    public BigDecimal getCooinsValue() {
        return cooinsValue;
    }

    public void setCooinsValue(BigDecimal cooinsValue) {
        this.cooinsValue = cooinsValue;
    }

    public static Map<String, Cooin> getCooinBatch(BigDecimal requestAmount, BigDecimal cooinsValue,
                 String currencyCode, TagVS tag, Boolean isTimeLimited,  CooinServer cooinServer) {
        Map<String, Cooin> cooinsMap = new HashMap<String, Cooin>();
        BigDecimal numCooins = requestAmount.divide(cooinsValue);
        for(int i = 0; i < numCooins.intValue(); i++) {
            Cooin cooin = new Cooin(cooinServer.getServerURL(), cooinsValue, currencyCode, tag);
            cooin.setIsTimeLimited(isTimeLimited);
            cooinsMap.put(cooin.getHashCertVS(), cooin);
        }
        return cooinsMap;
    }

    public JSONObject getRequestDataToSignJSON() {
        Map smimeContentMap = new HashMap();
        smimeContentMap.put("operation", TypeVS.COOIN_REQUEST.toString());
        smimeContentMap.put("subject", subject);
        smimeContentMap.put("serverURL", cooinServer.getServerURL());
        smimeContentMap.put("totalAmount", requestAmount.toString());
        smimeContentMap.put("currencyCode", currencyCode);
        smimeContentMap.put("isTimeLimited", isTimeLimited);
        smimeContentMap.put("tag", tag);
        smimeContentMap.put("UUID", UUID.randomUUID().toString());
        JSONObject requestJSON = (JSONObject) JSONSerializer.toJSON(smimeContentMap);
        return requestJSON;
    }

    public List<Map> getCooinCSRList () {
        return cooinCSRList;
    }

    public Cooin initCooin(String signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(
                signedCsr.getBytes());
        if(certificates.isEmpty()) throw new ExceptionVS("Unable to init Cooin. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        JSONObject certExtensionData = CertUtils.getCertExtensionData(x509Certificate, ContextVS.COOIN_OID);
        Cooin cooin = cooinsMap.get(certExtensionData.getString("hashCertVS")).setState(Cooin.State.OK);
        cooin.initSigner(signedCsr.getBytes());
        return cooin;
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
        for(Cooin cooin : cooinsMap.values()) {
            cooin.setTag(tagVS);
        }
    }

    public JSONArray getCooinCSRRequest() {
        return (JSONArray) JSONSerializer.toJSON(getCooinCSRList());
    }

    public TransactionVS getTransactionVS(String subject, Map<CooinAccount, BigDecimal> accountFromMovements) {
        TransactionVS transaction = new TransactionVS();
        transaction.setAmount(requestAmount);
        transaction.setState(TransactionVS.State.OK);
        transaction.setCurrencyCode(currencyCode);
        transaction.setTag(tagVS);
        transaction.setSubject(subject);
        transaction.setMessageSMIME(messageSMIME);
        transaction.setType(TransactionVS.Type.COOIN_REQUEST);
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