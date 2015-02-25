package org.votingsystem.android.util;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.callable.MessageTimeStamper;
import org.votingsystem.model.Cooin;
import org.votingsystem.model.TransactionRequest;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TimestampException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinBundle {

    private BigDecimal amount;
    private BigDecimal wildTagAmount;
    private List<Cooin> tagCooinList;
    private List<Cooin> wildTagCooinList;
    private String currencyCode;
    private String tagVS;
    private Cooin leftOverCooin;

    public CooinBundle(BigDecimal amount, List<Cooin> tagCooinList, String currencyCode,
                       String tagVS) {
        this.amount = amount;
        this.tagCooinList = tagCooinList;
        this.currencyCode = currencyCode;
        this.tagVS = tagVS;
    }

    public CooinBundle(BigDecimal amount, String currencyCode, List<Cooin> tagCooinList,
                       String tag) {
        this.tagVS = tag;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.tagCooinList = tagCooinList;
        Collections.sort(this.tagCooinList, cooinComparator);
    }

    public Set<Cooin> getCooinSet() {
        Set<Cooin> result = new HashSet<>(tagCooinList);
        if(wildTagCooinList != null) result.addAll(wildTagCooinList);
        return result;
    }

    public static CooinBundle load(List<Cooin> cooinList) throws ExceptionVS {
        String tagVS = null;
        String currencyCode = null;
        BigDecimal amount = BigDecimal.ZERO;
        for(Cooin cooin: cooinList) {
            if(tagVS == null) tagVS = cooin.getSignedTagVS();
            else if(!tagVS.equals(cooin.getSignedTagVS())) throw new ExceptionVS("bundle with mixed" +
                    "tags: " + tagVS + ", " + cooin.getSignedTagVS());
            if(currencyCode == null) currencyCode = cooin.getCurrencyCode();
            else if(!currencyCode.equals(cooin.getCurrencyCode())) throw new ExceptionVS(
                    "bundle with mixed curency codes : " + currencyCode + ", " + cooin.getCurrencyCode());
            amount = amount.add(cooin.getAmount());
        }
        return new CooinBundle(amount, cooinList, currencyCode, tagVS);
    }

    public List<Cooin> getTagCooinList() {
        return tagCooinList;
    }

    public void setTagCooinList(List<Cooin> tagCooinList) {
        this.tagCooinList = tagCooinList;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTagVS() {
        return tagVS;
    }

    public void setTagVS(String tagVS) {
        this.tagVS = tagVS;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public List<Cooin> getWildTagCooinList() {
        return wildTagCooinList;
    }

    public void setWildTagCooinList(List<Cooin> wildTagCooinList) {
        this.wildTagCooinList = wildTagCooinList;
    }

    public BigDecimal getWildTagAmount() {
        return wildTagAmount;
    }

    public void setWildTagAmount(BigDecimal wildTagAmount) {
        this.wildTagAmount = wildTagAmount;
    }

    public BigDecimal getTotalAmount() {
        if(amount != null) {
            if(wildTagAmount != null) return amount.add(wildTagAmount);
            else return amount;
        } else if(wildTagAmount != null) {
            return wildTagAmount;
        } else return BigDecimal.ZERO;
    }

    public Cooin getLeftOverCooin(BigDecimal requestAmount, String cooinServerURL)
            throws Exception {
        BigDecimal bundleAmount = getTotalAmount();
        if(bundleAmount.compareTo(requestAmount) == 0) return null;
        if(leftOverCooin == null && requestAmount.compareTo(bundleAmount) < 0) {
            leftOverCooin = new Cooin(cooinServerURL, bundleAmount.subtract(requestAmount),
                    currencyCode, tagVS, TypeVS.COOIN);
        }
        return leftOverCooin;
    }

    public JSONArray getTransactionData(TransactionRequest transactionRequest,
                                        AppContextVS contextVS) throws Exception {
        JSONArray result = new JSONArray();
        JSONObject transactionData = transactionRequest.getAnonymousSignedTransaction(false);
        List<Cooin> transactionCooins = new ArrayList<>(tagCooinList);
        if(wildTagCooinList != null) transactionCooins.addAll(wildTagCooinList);
        ResponseVS responseVS = null;
        for(Cooin cooin : transactionCooins) {
            SMIMEMessage smimeMessage = cooin.getCertificationRequest().getSMIME(
                    cooin.getHashCertVS(), StringUtils.getNormalized(
                            transactionRequest.getToUserName()), transactionData.toString(),
                    transactionRequest.getSubject(), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, contextVS);
            responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode())
                throw new TimestampException(responseVS.getMessage());
            result.put(new String(Base64.encode(smimeMessage.getBytes())));
        }
        return result;
    }

    public void updateWallet(Cooin leftOverCooin, AppContextVS contextVS) throws Exception {
        Set<Cooin> cooinListToRemove = getCooinSet();
        Wallet.removeCooinList(cooinListToRemove, contextVS);
        if(leftOverCooin != null) Wallet.updateWallet(
                new HashSet<Cooin>(Arrays.asList(leftOverCooin)),contextVS);
    }

    private static Comparator<Cooin> cooinComparator = new Comparator<Cooin>() {
        public int compare(Cooin c1, Cooin c2) {
            return c1.getAmount().compareTo(c2.getAmount());
        }
    };
}
