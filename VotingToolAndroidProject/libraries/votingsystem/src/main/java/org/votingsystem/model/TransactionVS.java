package org.votingsystem.model;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.lib.R;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.DateUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;


public class TransactionVS  implements Serializable {

    public static final long serialVersionUID = 1L;

    public enum Type {VICKET_REQUEST, USER_ALLOCATION, USER_ALLOCATION_INPUT, VICKET_CANCELLATION,
        VICKET_SEND;}

    public enum State { OK, REPEATED, CANCELLED;}

    private Long id;
    private Long localId;
    private String messageSMIMEURL;
    private String subject;
    private BigDecimal amount = null;

    private transient SMIMEMessageWrapper messageSMIME;
    private byte[] messageSMIMEBytes;
    private transient SMIMEMessageWrapper cancellationSMIME;
    private byte[] cancellationSMIMEBytes;

    private TransactionVS transactionParent;
    private CurrencyVS currencyVS;

    private UserVS fromUserVS;
    private UserVS toUserVS;

    private List<Vicket> vickets;
    private Type type;

    private Date validTo;
    private Date dateCreated;
    private Date lastUpdated;


    public TransactionVS() {}

    public TransactionVS(Type type, List<Vicket> vickets) {
        this.type = type;
        this.vickets = vickets;
    }

    public TransactionVS(Type type, Date dateCreated,  List<Vicket> vickets, BigDecimal amount,
                 CurrencyVS currencyVS) {
        this.type = type;
        this.vickets = vickets;
        this.amount = amount;
        this.currencyVS = currencyVS;
        this.dateCreated = dateCreated;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserVS getFromUserVS() {
        return fromUserVS;
    }

    public void setFromUserVS(UserVS fromUserVS) {
        this.fromUserVS = fromUserVS;
    }

    public UserVS getToUserVS() {
        return toUserVS;
    }

    public void setToUserVS(UserVS toUserVS) {
        this.toUserVS = toUserVS;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionVS getTransactionParent() {
        return transactionParent;
    }

    public void setTransactionParent(TransactionVS transactionParent) {
        this.transactionParent = transactionParent;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public CurrencyVS getCurrencyVS() {
        return currencyVS;
    }

    public void setCurrencyVS(CurrencyVS currencyVS) {
        this.currencyVS = currencyVS;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public SMIMEMessageWrapper getMessageSMIME() {
        if(messageSMIME == null && messageSMIMEBytes != null) {
            try {
                messageSMIME = new SMIMEMessageWrapper(null,
                        new ByteArrayInputStream(messageSMIMEBytes), null);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return messageSMIME;
    }

    public void setMessageSMIME(SMIMEMessageWrapper messageSMIME) {
        this.messageSMIME = messageSMIME;
    }

    public SMIMEMessageWrapper getCancellationSMIME() {
        if(cancellationSMIME == null && cancellationSMIMEBytes != null) {
            try {
                cancellationSMIME = new SMIMEMessageWrapper(null,
                        new ByteArrayInputStream(cancellationSMIMEBytes), null);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return cancellationSMIME;
    }

    public void setCancellationSMIME(SMIMEMessageWrapper cancellationSMIME) {
        this.cancellationSMIME = cancellationSMIME;
    }

    public String getMessageSMIMEURL() {
        return messageSMIMEURL;
    }

    public void setMessageSMIMEURL(String messageSMIMEURL) {
        this.messageSMIMEURL = messageSMIMEURL;
    }

    public byte[] getMessageSMIMEBytes() {
        return messageSMIMEBytes;
    }

    public void setMessageSMIMEBytes(byte[] messageSMIMEBytes) {
        this.messageSMIMEBytes = messageSMIMEBytes;
    }

    public byte[] getCancellationSMIMEBytes() {
        return cancellationSMIMEBytes;
    }

    public void setCancellationSMIMEBytes(byte[] cancellationSMIMEBytes) {
        this.cancellationSMIMEBytes = cancellationSMIMEBytes;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    public int getIconId(Context context) {
        switch(type) {
            case VICKET_CANCELLATION:
            case USER_ALLOCATION_INPUT:
                return R.drawable.edit_redo_24;
            case VICKET_REQUEST:
                return R.drawable.edit_undo_24;
            case VICKET_SEND:
                return R.drawable.euro_24;
            default:
                return R.drawable.pending;
        }
    }

    public TypeVS getTypeVS() {
        switch(getType()){
            case VICKET_REQUEST:
                return TypeVS.VICKET_REQUEST;
            case USER_ALLOCATION_INPUT:
                return TypeVS.USER_ALLOCATION_INPUT;
            default: return null;
        }
    }

    public String getDescription(Context context) {
        switch(type) {
            case VICKET_CANCELLATION:
                return context.getString(R.string.vicket_cancellation);
            case USER_ALLOCATION_INPUT:
                return context.getString(R.string.account_input);
            case VICKET_REQUEST:
                return context.getString(R.string.account_output);
            case VICKET_SEND:
                return context.getString(R.string.vicket_send);
            default:
                return type.toString();
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(getMessageSMIME() != null) s.writeObject(messageSMIME.getBytes());
            else s.writeObject(null);
            if(getCancellationSMIME() != null) s.writeObject(cancellationSMIME.getBytes());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

    }

    public List<Vicket> getVickets() {
        return vickets;
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        messageSMIMEBytes = (byte[]) s.readObject();
        cancellationSMIMEBytes = (byte[]) s.readObject();
    }

    public static TransactionVS parse(JSONObject jsonData) throws ParseException, JSONException {
        TransactionVS transactionVS = new TransactionVS();
        transactionVS.setId(jsonData.getLong("id"));
        if(jsonData.has("fromUserVS")) {
            JSONObject fromUserVSJSON = jsonData.getJSONObject("fromUserVS");
            UserVS fromUserVS = new UserVS();
            fromUserVS.setFullName(fromUserVSJSON.getString("name"));
            fromUserVS.setNif(fromUserVSJSON.getString("nif"));
            transactionVS.setFromUserVS(fromUserVS);
        }
        if(jsonData.has("toUserVS")) {
            JSONObject toUserVSJSON = jsonData.getJSONObject("toUserVS");
            UserVS toUserVS = new UserVS();
            toUserVS.setFullName(toUserVSJSON.getString("name"));
            toUserVS.setNif(toUserVSJSON.getString("nif"));
            transactionVS.setToUserVS(toUserVS);
        }
        transactionVS.setSubject(jsonData.getString("subject"));
        transactionVS.setCurrencyVS(CurrencyVS.valueOf(jsonData.getString("currency")));
        transactionVS.setDateCreated(DateUtils.getDateFromString(jsonData.getString("dateCreated")));
        if(jsonData.has("validTo")) transactionVS.setValidTo(
                DateUtils.getDateFromString(jsonData.getString("validTo")));
        transactionVS.setType(Type.valueOf(jsonData.getString("type")));
        transactionVS.setAmount(new BigDecimal(jsonData.getString("amount")));
        transactionVS.setMessageSMIMEURL(jsonData.getString("messageSMIMEURL"));
        return transactionVS;
    }

}