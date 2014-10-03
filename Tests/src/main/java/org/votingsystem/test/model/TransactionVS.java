package org.votingsystem.test.model;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVS  implements Serializable {

    public static final long serialVersionUID = 1L;

    public enum Type { VICKET_REQUEST, VICKET_SEND, VICKET_CANCELLATION, FROM_BANKVS, FROM_USERVS,
        FROM_GROUP_TO_MEMBER_GROUP, FROM_GROUP_TO_MEMBER, FROM_GROUP_TO_ALL_MEMBERS, VICKET_INIT_PERIOD;}

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
    private String currencyCode;

    private UserVS fromUserVS;
    private UserVS sender;
    private UserVS toUserVS;
    private List<String> toUserIBAN;

    private List<Vicket> vickets;
    private List<VicketTagVS> tagVSList;
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
                 String currencyCode) {
        this.type = type;
        this.vickets = vickets;
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

    public List<VicketTagVS> getTagVSList() {
        return tagVSList;
    }

    public VicketTagVS getTagVS() {
        return tagVSList.iterator().next();
    }

    public void setTagVSList(List<VicketTagVS> tagVSList) {
        this.tagVSList = tagVSList;
    }

    public SMIMEMessageWrapper getMessageSMIME() {
        if(messageSMIME == null && messageSMIMEBytes != null) {
            try {
                messageSMIME = new SMIMEMessageWrapper(new ByteArrayInputStream(messageSMIMEBytes));
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
                cancellationSMIME = new SMIMEMessageWrapper(new ByteArrayInputStream(cancellationSMIMEBytes));
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


    public List<String> getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(List<String> toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public TypeVS getTypeVS() {
        switch(getType()){
            case VICKET_REQUEST:
                return TypeVS.VICKET_REQUEST;
            default: return null;
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

    public static TransactionVS parse(OperationVS operationVS) throws Exception, ExceptionVS {
        TransactionVS transactionVS = new TransactionVS();
        JSONObject documentToSign = (JSONObject) JSONSerializer.toJSON(operationVS.getDocumentToSignMap());
        transactionVS.setAmount(new BigDecimal(documentToSign.getDouble("amount")));
        JSONArray tagArray = documentToSign.getJSONArray("tags");
        VicketTagVS tagVS = null;
        if(tagArray != null && tagArray.size() > 0) {
            Object tagObject = tagArray.get(0);
            String tagName = tagObject instanceof String ? (String) tagObject :((JSONObject)tagObject).getString("name");
            tagVS = new VicketTagVS(tagName);
        } else tagVS = new VicketTagVS(VicketTagVS.WILDTAG);
        transactionVS.setTagVSList(Arrays.asList(tagVS));
        transactionVS.setCurrencyCode(documentToSign.getString("currency"));
        transactionVS.setSubject(documentToSign.getString("subject"));

        JSONArray receptorsArray = documentToSign.getJSONArray("toUserIBAN");
        List<String> toUserIBAN = new ArrayList<String>();
        for(int i = 0;  i < receptorsArray.size(); i++) {
            toUserIBAN.add((String) receptorsArray.get(i));
        }
        transactionVS.setToUserIBAN(toUserIBAN);
        UserVS toUserVS = new UserVS();
        toUserVS.setName(documentToSign.getString("toUser"));
        if(operationVS.getType() == TypeVS.TRANSACTIONVS_FROM_USERVS) {
            if(toUserIBAN.size() != 1) throw new ExceptionVS("TRANSACTIONVS_FROM_USERVS must have " +
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
        transactionVS.setDateCreated(DateUtils.getDateFromString(jsonData.getString("dateCreated"),
                "dd MMM yyyy' 'HH:mm"));
        if(jsonData.has("validTo")) transactionVS.setValidTo(
                DateUtils.getDateFromString(jsonData.getString("validTo"), "dd MMM yyyy' 'HH:mm"));
        transactionVS.setType(Type.valueOf(jsonData.getString("type")));
        transactionVS.setAmount(new BigDecimal(jsonData.getString("amount")));
        transactionVS.setMessageSMIMEURL(jsonData.getString("messageSMIMEURL"));
        if(jsonData.has("tags")) transactionVS.setTagVSList(VicketTagVS.parse(jsonData.getJSONArray("tags"))); ;
        return transactionVS;
    }

    public JSONObject transactionFromUserVSJSON(String fromUserIBAN) throws Exception {
        Map mapToSend = new HashMap();
        mapToSend.put("operation", TypeVS.TRANSACTIONVS_FROM_USERVS.toString());
        mapToSend.put("fromUserIBAN", fromUserIBAN);
        mapToSend.put("subject", subject);
        mapToSend.put("toUser", toUserVS.getName());
        mapToSend.put("toUserIBAN", Arrays.asList(toUserVS.getIBAN()));
        mapToSend.put("tags", Arrays.asList(getTagVS().getName()));
        mapToSend.put("amount", amount.toString());
        mapToSend.put("currency", currencyCode);
        mapToSend.put("UUID", UUID.randomUUID().toString());
        return (JSONObject) JSONSerializer.toJSON(mapToSend);
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
        if(tagVSList != null && !tagVSList.isEmpty()) {
            JSONArray jsonArray = new JSONArray();
            for(VicketTagVS tag:tagVSList) {
                jsonArray.add(tag.toJSON());
            }
            jsonData.put("tags", jsonArray);
        }
        return jsonData;
    }

    public static List<TransactionVS> parseList(JSONArray transactionArray) throws Exception {
        List<TransactionVS> result = new ArrayList<TransactionVS>();
        for(int i = 0; i < transactionArray.size(); i++) {
            result.add(TransactionVS.parse((JSONObject) transactionArray.get(i)));
        }
        return result;
    }

    @Override
    public String toString() {
        String tagVS = null;
        if(tagVSList != null && !tagVSList.isEmpty()) tagVS = tagVSList.iterator().next().getName();
        return  "[TransactionVS - subject: '" + subject + "' - amount: '" + amount + "'" +
            " - currencyCode: " + currencyCode + " - tagVS: '" + tagVS + " - toUser: '" +
                toUserVS.getName() + "' - toUserIBAN: '" + toUserVS.getIBAN() +"']";
    }

}