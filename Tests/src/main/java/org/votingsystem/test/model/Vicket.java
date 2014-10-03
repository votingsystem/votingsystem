package org.votingsystem.test.model;

import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertificationRequestVS;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Vicket implements Serializable  {

    private static Logger log = Logger.getLogger(Vicket.class);

    public static final long serialVersionUID = 1L;

    public enum State { OK, REJECTED, CANCELLED, EXPENDED, LAPSED;}

    private TransactionVS transaction;
    private transient SMIMEMessageWrapper receipt;
    private transient SMIMEMessageWrapper cancellationReceipt;
    private CertificationRequestVS certificationRequest;
    private byte[] receiptBytes;
    private byte[] cancellationReceiptBytes;
    private String originHashCertVS;
    private String hashCertVSBase64;
    private BigDecimal amount;
    private String subject;
    private State state;
    private Date cancellationDate;
    private String currencyCode;
    private String url;
    private String vicketServerURL;

    public Vicket() {}

    public Vicket(String vicketServerURL, BigDecimal amount, String currencyCode, TypeVS typeVS) {
        this.amount = amount;
        this.vicketServerURL = vicketServerURL;
        this.currencyCode = currencyCode;
        try {
            setOriginHashCertVS(UUID.randomUUID().toString());
            setHashCertVSBase64(CMSUtils.getHashBase64(getOriginHashCertVS(), ContextVS.VOTING_DATA_DIGEST));
            certificationRequest = CertificationRequestVS.getVicketRequest(
                    ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM,
                    ContextVS.PROVIDER, vicketServerURL, getHashCertVSBase64(), amount.toString(), this.currencyCode);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }


    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getVicketServerURL() {
        return vicketServerURL;
    }

    public void setVicketServerURL(String vicketServerURL) {
        this.vicketServerURL = vicketServerURL;
    }

    public String getOriginHashCertVS() {
        return originHashCertVS;
    }

    public void setOriginHashCertVS(String originHashCertVS) {
        this.originHashCertVS = originHashCertVS;
    }


    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }


    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public CertificationRequestVS getCertificationRequest() {
        return certificationRequest;
    }

    public void setCertificationRequest(CertificationRequestVS certificationRequest) {
        this.certificationRequest = certificationRequest;
    }

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public void setHashCertVSBase64(String hashCertVSBase64) {
        this.hashCertVSBase64 = hashCertVSBase64;
    }

    public static Map checkSubject(String subjectDN) {
        String currency = null;
        String amount = null;
        String vicketServerURL = null;
        if (subjectDN.contains("CURRENCY:")) currency = subjectDN.split("CURRENCY:")[1].split(",")[0];
        if (subjectDN.contains("AMOUNT:")) amount = subjectDN.split("AMOUNT:")[1].split(",")[0];
        if (subjectDN.contains("vicketServerURL:")) vicketServerURL = subjectDN.split("vicketServerURL:")[1].split(",")[0];
        Map resultMap = new HashMap();
        resultMap.put("currency", currency);
        resultMap.put("amount", amount);
        resultMap.put("vicketServerURL", vicketServerURL);
        return resultMap;
    }

}
