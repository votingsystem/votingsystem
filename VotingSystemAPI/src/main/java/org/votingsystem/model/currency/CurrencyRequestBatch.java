package org.votingsystem.model.currency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.model.BatchRequest;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.TypeVS;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**

 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("CurrencyRequestBatch")
public class CurrencyRequestBatch extends BatchRequest implements Serializable  {

    private static Logger log = Logger.getLogger(CurrencyRequestBatch.class.getSimpleName());

    public static final long serialVersionUID = 1L;

    @OneToOne private MessageSMIME messageSMIME;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;
    @Column(name="isTimeLimited") private Boolean isTimeLimited;


    @Transient private Map<String, Currency> currencyMap;
    @Transient private CurrencyServer currencyServer;
    @Transient private BigDecimal requestAmount;
    @Transient private BigDecimal currencyValue;
    @Transient private String currencyCode;
    @Transient private List<Map> currencyCSRList;
    @Transient private String tag;
    @Transient private String subject;

    public CurrencyRequestBatch() {}

    public CurrencyRequestBatch(byte[] currencyBatchRequest, MessageSMIME messageSMIME, String localServer) throws Exception {
        this.messageSMIME = messageSMIME;
        SMIMEMessage smimeMessage = messageSMIME.getSMIME();
        ObjectNode currencyRequest = (ObjectNode) new ObjectMapper().readTree(smimeMessage.getSignedContent());
        if(TypeVS.CURRENCY_REQUEST != TypeVS.valueOf(currencyRequest.get("operation").asText())) throw new ExceptionVS(
                "Request operation '" + currencyRequest.get("operation").asText() + "' doesn't match CURRENCY_REQUEST");
        this.requestAmount = new BigDecimal(currencyRequest.get("totalAmount").asText());
        this.currencyCode = currencyRequest.get("currencyCode").asText();
        this.subject = currencyRequest.get("subject").asText();
        this.isTimeLimited = currencyRequest.get("isTimeLimited").asBoolean();
        this.tag = currencyRequest.get("tag").asText();
        CurrencyServer currencyServer = new CurrencyServer();
        currencyServer.setServerURL(currencyRequest.get("serverURL").asText());
        if(!localServer.equals(currencyServer.getServerURL())) throw new ExceptionVS("The server from request '" +
                currencyServer.getServerURL() + "' doesn't match local server '" + localServer + "'");
        ArrayNode  currencyCSRArray = (ArrayNode) new ObjectMapper().readTree(new String(currencyBatchRequest, "UTF-8"));
        Iterator<JsonNode> ite = currencyCSRArray.elements();
        BigDecimal csrRequestAmount = BigDecimal.ZERO;
        currencyMap = new HashMap<String, Currency>();
        while (ite.hasNext()) {
            JsonNode currencyRequestJSON = ite.next();
            BigDecimal currencyValue = new BigDecimal(currencyRequestJSON.get("currencyValue").asText());
            String currencyCode = currencyRequestJSON.get("currencyCode").asText();
            String csrTagVS = currencyRequestJSON.get("tag").asText();
            if(!this.tag.equals(csrTagVS)) throw new ExceptionVS("Request is for tag '" + this.tag +
                    "' and request number '" + currencyMap.size() + "' is for tag '" + csrTagVS + "'");
            PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(
                    currencyRequestJSON.get("csr").asText().getBytes());
            Currency currency = new Currency(csr);
            if(currencyValue.compareTo(currency.getAmount()) != 0 || !currencyCode.equals(currency.getCurrencyCode()) ||
                    !csrTagVS.equals(currency.getCertTagVS())) throw new ExceptionVS(
                    "Currency CSR request number '" + currencyMap.size() + "' with ERRORS. JSON request: '" + currencyRequestJSON.toString() +
                            "'. Cert extension data: '" + currency.getCertExtensionData().toString() + "'");
            if (!localServer.equals(currency.getCurrencyServerURL()))  throw new ExceptionVS("Currency signed server URL '" +
                    currency.getCurrencyServerURL() + "' doesn't match local server URL '" + localServer + "'");
            csrRequestAmount = csrRequestAmount.add(currency.getAmount());
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        if(requestAmount.compareTo(csrRequestAmount) != 0) throw new ExceptionVS("Currency batch request with errors. " +
            "Amount signed '" + requestAmount.toString() + "' - total amount in CSRs '" + csrRequestAmount.toString() + "'");
    }

    public CurrencyRequestBatch(BigDecimal requestAmount, BigDecimal currencyValue, String currencyCode, TagVS tagVS,
                                Boolean isTimeLimited, CurrencyServer currencyServer) throws Exception {
        this.setRequestAmount(requestAmount);
        this.setCurrencyServer(currencyServer);
        this.setCurrencyCode(currencyCode);
        this.isTimeLimited = isTimeLimited;
        this.tagVS = tagVS == null ? new TagVS(TagVS.WILDTAG):tagVS;
        this.tag = this.tagVS.getName();
        this.currencyValue = currencyValue;
        this.currencyMap = getCurrencyBatch(requestAmount,currencyValue, currencyCode, tagVS, isTimeLimited, currencyServer);
        currencyCSRList = new ArrayList<Map>();
        for(Currency currency : currencyMap.values()) {
            currencyCSRList.add(currency.getCSRDataMap());
        }
    }

    public Map<String, Currency> getCurrencyMap() {
        return currencyMap;
    }

    public List<String> getIssuedCurrencyListPEM() throws IOException {
        List<String> result = new ArrayList<String>();
        for(Currency currency : currencyMap.values()) {
            result.add(new String(currency.getIssuedCertPEM(), "UTF-8"));
        }
        return result;
    }

    public void initCurrency(List<String> issuedCurrencyArray) throws Exception {
        log.info("CurrencyRequest - Num IssuedCurrency: " + issuedCurrencyArray.size());
        if(issuedCurrencyArray.size() != currencyMap.size()) {
            log.log(Level.SEVERE, "CurrencyRequest(...) - ERROR - Num currency requested: " + currencyMap.size() +
                    " - num. currency received: " + issuedCurrencyArray.size());
        }
        for(int i = 0; i < issuedCurrencyArray.size(); i++) {
            Currency currency = initCurrency(issuedCurrencyArray.get(i));
            currencyMap.replace(currency.getHashCertVS(), currency);
        }
    }

    public void setCurrencyMap(Map<String, Currency> currencyMap) {
        this.currencyMap = currencyMap;
    }

    public CurrencyServer getCurrencyServer() {
        return currencyServer;
    }

    public void setCurrencyServer(CurrencyServer currencyServer) {
        this.currencyServer = currencyServer;
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

    public BigDecimal getCurrencyValue() {
        return currencyValue;
    }

    public void setCurrencyValue(BigDecimal currencyValue) {
        this.currencyValue = currencyValue;
    }

    public static Map<String, Currency> getCurrencyBatch(BigDecimal requestAmount, BigDecimal currencyValue,
                 String currencyCode, TagVS tag, Boolean isTimeLimited,  CurrencyServer currencyServer) {
        Map<String, Currency> currencyMap = new HashMap<String, Currency>();
        BigDecimal numCurrency = requestAmount.divide(currencyValue);
        for(int i = 0; i < numCurrency.intValue(); i++) {
            Currency currency = new Currency(currencyServer.getServerURL(), currencyValue, currencyCode, tag);
            currency.setIsTimeLimited(isTimeLimited);
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        return currencyMap;
    }

    public Map getRequestDataToSignMap() {
        Map smimeContentMap = new HashMap();
        smimeContentMap.put("operation", TypeVS.CURRENCY_REQUEST.toString());
        smimeContentMap.put("subject", subject);
        smimeContentMap.put("serverURL", currencyServer.getServerURL());
        smimeContentMap.put("totalAmount", requestAmount.toString());
        smimeContentMap.put("currencyCode", currencyCode);
        smimeContentMap.put("isTimeLimited", isTimeLimited);
        smimeContentMap.put("tag", tag);
        smimeContentMap.put("UUID", UUID.randomUUID().toString());
        return smimeContentMap;
    }

    public List<Map> getCurrencyCSRList () {
        return currencyCSRList;
    }

    public Currency initCurrency(String signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(
                signedCsr.getBytes());
        if(certificates.isEmpty()) throw new ExceptionVS("Unable to init Currency. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        Map<String, String> certExtensionData = CertUtils.getCertExtensionData(x509Certificate, ContextVS.CURRENCY_OID);
        Currency currency = currencyMap.get(certExtensionData.get("hashCertVS")).setState(Currency.State.OK);
        currency.initSigner(signedCsr.getBytes());
        return currency;
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
        for(Currency currency : currencyMap.values()) {
            currency.setTag(tagVS);
        }
    }

    public TransactionVS getTransactionVS(String subject, Map<CurrencyAccount, BigDecimal> accountFromMovements) {
        TransactionVS transaction = new TransactionVS();
        transaction.setAmount(requestAmount);
        transaction.setState(TransactionVS.State.OK);
        transaction.setCurrencyCode(currencyCode);
        transaction.setTag(tagVS);
        transaction.setSubject(subject);
        transaction.setMessageSMIME(messageSMIME);
        transaction.setType(TransactionVS.Type.CURRENCY_REQUEST);
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