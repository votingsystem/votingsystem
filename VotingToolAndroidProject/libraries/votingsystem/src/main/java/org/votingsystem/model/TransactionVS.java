package org.votingsystem.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.model.UserVS;
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


public class TransactionVS  implements Serializable {

    public static final long serialVersionUID = 1L;

    public enum Type { USER_INPUT, USER_OUTPUT, SYSTEM_INPUT, SYSTEM_OUTPUT, USER_ALLOCATION;}

    public enum State { OK, REPEATED, CANCELLED;}

    private Long id;
    private String messageSMIMEURL;
    private BigDecimal amount = null;

    private transient SMIMEMessageWrapper messageSMIME;
    private byte[] messageSMIMEBytes;
    private transient SMIMEMessageWrapper cancellationSMIME;
    private byte[] cancellationSMIMEBytes;

    private TransactionVS transactionParent;

    private UserVS fromUserVS;
    private UserVS toUserVS;

    private Type type;

    private Date validTo;
    private Date dateCreated;
    private Date lastUpdated;


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

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(messageSMIME != null) s.writeObject(messageSMIME.getBytes());
            else s.writeObject(null);
            if(cancellationSMIME != null) s.writeObject(cancellationSMIME.getBytes());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        messageSMIMEBytes = (byte[]) s.readObject();
        cancellationSMIMEBytes = (byte[]) s.readObject();
    }

    public static TransactionVS parse(JSONObject jsonData) throws ParseException, JSONException {
        TransactionVS transactionVS = new TransactionVS();
        if(jsonData.has("fromUserVS")) {
            JSONObject fromUserVSJSON = jsonData.getJSONObject("fromUserVS");
            UserVS fromUserVS = new UserVS();
            fromUserVS.setFullName(fromUserVSJSON.getString("name"));
            fromUserVS.setNif(fromUserVSJSON.getString("nif"));
            transactionVS.setFromUserVS(fromUserVS);
        }
        transactionVS.setDateCreated(DateUtils.getDateFromString(jsonData.getString("dateCreated")));
        if(jsonData.has("validTo")) transactionVS.setValidTo(
                DateUtils.getDateFromString(jsonData.getString("validTo")));
        transactionVS.setType(Type.valueOf(jsonData.getString("type")));
        transactionVS.setAmount(new BigDecimal(jsonData.getString("amount")));
        transactionVS.setMessageSMIMEURL(jsonData.getString("messageSMIMEURL"));
        return transactionVS;
    }

}