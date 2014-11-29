package org.votingsystem.model;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.lib.R;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVS  implements Serializable {

    public static final long serialVersionUID = 1L;

    public enum Type { COOIN_REQUEST, COOIN_SEND, COOIN_CANCELLATION, FROM_BANKVS, FROM_USERVS,
        FROM_GROUP_TO_MEMBER_GROUP, FROM_GROUP_TO_MEMBER, FROM_GROUP_TO_ALL_MEMBERS, COOIN_INIT_PERIOD;}

    public enum State { OK, REPEATED, CANCELLED;}

    private Long id;
    private Long localId;
    private String messageSMIMEURL;
    private String subject;
    private BigDecimal amount = null;
    private transient SMIMEMessage messageSMIME;
    private byte[] messageSMIMEBytes;
    private transient SMIMEMessage cancellationSMIME;
    private byte[] cancellationSMIMEBytes;
    private Boolean isTimeLimited;
    private TransactionVS transactionParent;
    private String currencyCode;
    private UserVS fromUserVS;
    private UserVS sender;
    private UserVS toUserVS;
    private List<String> toUserIBAN;
    private List<Cooin> cooins;
    private TagVS tagVS = new TagVS(TagVS.WILDTAG);
    private Type type;
    private Date validTo;
    private Date dateCreated;
    private Date lastUpdated;

    public TransactionVS() {}

    public TransactionVS(BigDecimal amount, String currencyCode) {
        this.amount = amount;
        this.currencyCode = currencyCode;
    }

    public TransactionVS(BigDecimal amount, String currencyCode, TagVS tagVS, boolean isTimeLimited){
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.tagVS = tagVS;
        this.isTimeLimited = isTimeLimited;
    }

    public TransactionVS(Type type, List<Cooin> cooins) {
        this.type = type;
        this.cooins = cooins;
    }

    public TransactionVS(Type type, Date dateCreated,  List<Cooin> cooins, BigDecimal amount,
                 String currencyCode) {
        this.type = type;
        this.cooins = cooins;
        this.amount = amount;
        this.currencyCode = currencyCode;
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public UserVS getSender() {
        return sender;
    }

    public void setSender(UserVS sender) {
        this.sender = sender;
    }

    public TagVS getTagVS() {
        return tagVS;
    }

    public List<String> getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(List<String> toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public Boolean isTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

    public void setTagVS(TagVS tagVS) {
        this.tagVS = tagVS;
    }

    public SMIMEMessage getMessageSMIME() {
        if(messageSMIME == null && messageSMIMEBytes != null) {
            try {
                messageSMIME = new SMIMEMessage(new ByteArrayInputStream(messageSMIMEBytes));
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return messageSMIME;
    }

    public void setMessageSMIME(SMIMEMessage messageSMIME) {
        this.messageSMIME = messageSMIME;
    }

    public SMIMEMessage getCancellationSMIME() {
        if(cancellationSMIME == null && cancellationSMIMEBytes != null) {
            try {
                cancellationSMIME = new SMIMEMessage(new ByteArrayInputStream(cancellationSMIMEBytes));
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return cancellationSMIME;
    }

    public void setCancellationSMIME(SMIMEMessage cancellationSMIME) {
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
            case COOIN_CANCELLATION:
                return R.drawable.edit_redo_24;
            case COOIN_REQUEST:
                return R.drawable.edit_undo_24;
            case COOIN_SEND:
                return R.drawable.fa_money_24;
            default:
                return R.drawable.pending;
        }
    }

    public TypeVS getTypeVS() {
        switch(getType()){
            case COOIN_REQUEST:
                return TypeVS.COOIN_REQUEST;
            default: return null;
        }
    }

    public String getDescription(Context context) {
        switch(type) {
            case COOIN_CANCELLATION:
                return context.getString(R.string.cooin_cancellation);
            case FROM_GROUP_TO_ALL_MEMBERS:
            case FROM_GROUP_TO_MEMBER:
            case FROM_GROUP_TO_MEMBER_GROUP:
                return context.getString(R.string.account_input);
            case COOIN_REQUEST:
                return context.getString(R.string.account_output);
            case COOIN_SEND:
                return context.getString(R.string.cooin_send);
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

    public List<Cooin> getCooins() {
        return cooins;
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        messageSMIMEBytes = (byte[]) s.readObject();
        cancellationSMIMEBytes = (byte[]) s.readObject();
    }

    public static TransactionVS parse(Uri uriData) {
        TransactionVS transactionVS = new TransactionVS();
        transactionVS.setAmount(new BigDecimal(uriData.getQueryParameter("amount")));
        TagVS tagVS = null;
        if(uriData.getQueryParameter("tagVS") != null) tagVS = new TagVS(uriData.getQueryParameter("tagVS"));
        else tagVS = new TagVS(TagVS.WILDTAG);
        transactionVS.setTagVS(tagVS);
        transactionVS.setCurrencyCode(uriData.getQueryParameter("currencyCode"));
        transactionVS.setSubject(uriData.getQueryParameter("subject"));
        UserVS toUserVS = new UserVS();
        toUserVS.setName(uriData.getQueryParameter("toUser"));
        toUserVS.setIBAN(uriData.getQueryParameter("toUserIBAN"));
        transactionVS.setToUserVS(toUserVS);
        return transactionVS;
    }

    public static TransactionVS parse(OperationVS operationVS) throws JSONException, ExceptionVS {
        TransactionVS transactionVS = new TransactionVS();
        JSONObject documentToSign = operationVS.getDocumentToSignJSON();
        transactionVS.setAmount(new BigDecimal(documentToSign.getDouble("amount")));
        JSONArray tagArray = documentToSign.getJSONArray("tags");
        TagVS tagVS = null;
        if(tagArray != null && tagArray.length() > 0) {
            tagVS = new TagVS(((JSONObject)tagArray.get(0)).getString("name"));
        } else tagVS = new TagVS(TagVS.WILDTAG);
        transactionVS.setTagVS(tagVS);
        transactionVS.setCurrencyCode(documentToSign.getString("currency"));
        transactionVS.setSubject(documentToSign.getString("subject"));

        JSONArray receptorsArray = documentToSign.getJSONArray("toUserIBAN");
        List<String> toUserIBAN = new ArrayList<String>();
        for(int i = 0;  i < receptorsArray.length(); i++) {
            toUserIBAN.add((String) receptorsArray.get(i));
        }
        transactionVS.setToUserIBAN(toUserIBAN);
        UserVS toUserVS = new UserVS();
        toUserVS.setName(documentToSign.getString("toUser"));
        if(operationVS.getTypeVS() == TypeVS.FROM_USERVS) {
            if(toUserIBAN.size() != 1) throw new ExceptionVS("FROM_USERVS must have " +
                    "'one' receptor and it has '" + toUserIBAN.size() + "'");
            toUserVS.setIBAN(toUserIBAN.iterator().next());
        }
        transactionVS.setToUserVS(toUserVS);
        UserVS fromUserVS = new UserVS();
        fromUserVS.setName(documentToSign.getString("fromUser"));
        fromUserVS.setIBAN(documentToSign.getString("fromUserIBAN"));
        transactionVS.setFromUserVS(fromUserVS);
        return transactionVS;
    }

    public static TransactionVS parse(JSONObject jsonData) throws Exception {
        TransactionVS transactionVS = new TransactionVS();
        transactionVS.setId(jsonData.getLong("id"));
        if(jsonData.has("fromUserVS")) {
            JSONObject fromUserJSON = jsonData.getJSONObject("fromUserVS");
            transactionVS.setFromUserVS(UserVS.parse(fromUserJSON));
            if(fromUserJSON.has("sender")) {
                JSONObject senderJSON = fromUserJSON.getJSONObject("sender");
                UserVS sender = new UserVS();
                sender.setIBAN(senderJSON.getString("fromUserIBAN"));
                sender.setName(senderJSON.getString("fromUser"));
                transactionVS.setSender(sender);
            }
        }
        if(jsonData.has("toUserVS")) {
            transactionVS.setToUserVS(UserVS.parse(jsonData.getJSONObject("toUserVS")));
        }
        transactionVS.setSubject(jsonData.getString("subject"));
        transactionVS.setCurrencyCode(jsonData.getString("currency"));
        transactionVS.setDateCreated(DateUtils.getDayWeekDate(jsonData.getString("dateCreated")));
        if(jsonData.has("validTo")) transactionVS.setValidTo(
                DateUtils.getDayWeekDate(jsonData.getString("validTo")));
        transactionVS.setType(Type.valueOf(jsonData.getString("type")));
        transactionVS.setAmount(new BigDecimal(jsonData.getString("amount")));
        transactionVS.setMessageSMIMEURL(jsonData.getString("messageSMIMEURL"));
        if(jsonData.has("tag")) transactionVS.setTagVS(new TagVS(jsonData.getString("tag")));
        return transactionVS;
    }

    public JSONObject transactionFromUserVSJSON(String fromUserIBAN) throws Exception {
        Map mapToSend = new HashMap();
        mapToSend.put("operation", TypeVS.FROM_USERVS.toString());
        mapToSend.put("fromUserIBAN", fromUserIBAN);
        mapToSend.put("subject", subject);
        mapToSend.put("toUser", toUserVS.getName());
        mapToSend.put("toUserIBAN", Arrays.asList(toUserVS.getIBAN()));
        mapToSend.put("tags", Arrays.asList(getTagVS().getName()));
        mapToSend.put("amount", amount.toString());
        mapToSend.put("currency", currencyCode);
        mapToSend.put("UUID", UUID.randomUUID().toString());
        return new JSONObject(mapToSend);
    }

    public JSONObject toJSON() throws Exception {
        JSONObject jsonData = new JSONObject();
        jsonData.put("id", this.id);
        if(fromUserVS != null) {
            if(sender != null) {
                JSONObject sederJSON = new JSONObject();
                sederJSON.put("fromUserIBAN", sender.getIBAN());
                sederJSON.put("fromUser", sender.getName());
            }
            JSONObject userJSON = fromUserVS.toJSON();
            jsonData.put("fromUserVS", userJSON);
        }
        if(toUserVS != null) {
            jsonData.put("toUserVS", toUserVS.toJSON());
        }
        jsonData.put("subject", subject);
        jsonData.put("currency", currencyCode);
        if(dateCreated != null)
            jsonData.put("dateCreated", DateUtils.getDateStr(dateCreated, "dd MMM yyyy' 'HH:mm"));
        if(validTo != null)
            jsonData.put("validTo", DateUtils.getDateStr(validTo, "dd MMM yyyy' 'HH:mm"));
        if(type != null) jsonData.put("type", type.toString());
        if(amount != null) jsonData.put("amount", amount.toString());
        jsonData.put("messageSMIMEURL", messageSMIMEURL);
        jsonData.put("tag", tagVS.getName());
        return jsonData;
    }

    public static List<TransactionVS> parseList(JSONArray transactionArray) throws Exception {
        List<TransactionVS> result = new ArrayList<TransactionVS>();
        for(int i = 0; i < transactionArray.length(); i++) {
            result.add(TransactionVS.parse((JSONObject) transactionArray.get(i)));
        }
        return result;
    }

    @Override
    public String toString() {
        return  "[TransactionVS - subject: '" + subject + "' - amount: '" + amount + "'" +
            " - currencyCode: " + currencyCode + " - tagVS: '" + tagVS.getName() + " - toUser: '" +
                toUserVS.getName() + "' - toUserIBAN: '" + toUserVS.getIBAN() +"']";
    }

}