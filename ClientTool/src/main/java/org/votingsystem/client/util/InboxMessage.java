package org.votingsystem.client.util;

import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import java.security.PrivateKey;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InboxMessage<T> {

    private static Logger log = Logger.getLogger(InboxMessage.class);

    public enum State {LAPSED, PENDING, PROCESSED, REMOVED}

    private WebSocketMessage webSocketMessage;
    private TypeVS typeVS;
    private String message;
    private String from;
    private Date date;
    private State state = State.PENDING;
    private String UUID;
    private String messageID = java.util.UUID.randomUUID().toString();
    private T data;
    private Boolean isEncrypted = false;
    private Boolean isTimeLimited = false;

    public InboxMessage() { }

    public InboxMessage(String from, Date date) {
        this.from = from;
        this.date = date;
        this.UUID = java.util.UUID.randomUUID().toString();
    }

    public InboxMessage(WebSocketMessage webSocketMessage) {
        load(webSocketMessage);
    }

    public String getMessageID() {
        return messageID;
    }

    private void load(WebSocketMessage webSocketMessage) {
        this.webSocketMessage = webSocketMessage;
        this.typeVS = webSocketMessage.getOperation();
        this.message = webSocketMessage.getMessage();
        this.from = webSocketMessage.getFrom();
        this.isEncrypted = webSocketMessage.isEncrypted();
        this.isTimeLimited = webSocketMessage.isTimeLimited();
        if(webSocketMessage.getDate() == null) webSocketMessage.setDate(Calendar.getInstance().getTime());
        this.date = webSocketMessage.getDate();
        this.UUID = webSocketMessage.getUUID();
    }

    public InboxMessage(JSONObject jsonObject) throws ParseException {
        if(jsonObject.has("typeVS")) typeVS = TypeVS.valueOf(jsonObject.getString("typeVS"));
        if(jsonObject.has("operation")) typeVS = TypeVS.valueOf(jsonObject.getString("operation"));
        if(jsonObject.has("date")) date = DateUtils.getDateFromString(jsonObject.getString("date"));
        if(jsonObject.has("messageID")) messageID = jsonObject.getString("messageID");
        try {
            load(new WebSocketMessage(jsonObject.getJSONObject("webSocketMessage")));
        } catch(Exception ex) { log.error(ex.getMessage(), ex);}
        if(jsonObject.has("message")) message = jsonObject.getString("message");
        UUID = jsonObject.getString("UUID");
    }

    public Date getDate() {
        return date;
    }

    public InboxMessage setDate(Date date) {
        this.date = date;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public InboxMessage setMessage(String message) {
        this.message = message;
        return this;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public InboxMessage setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
        return this;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public State getState() {
        return state;
    }

    public InboxMessage setState(State state) {
        this.state = state;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public InboxMessage setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public boolean isTimeLimited() {
        return isTimeLimited;
    }

    public WebSocketMessage getWebSocketMessage() {
        return webSocketMessage;
    }

    public void setWebSocketMessage(WebSocketMessage webSocketMessage) {
        this.webSocketMessage = webSocketMessage;
    }

    public void decryptMessage(PrivateKey privateKey) throws Exception {
        webSocketMessage.decryptMessage(privateKey);
        load(webSocketMessage);
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        result.put("from", from);
        result.put("date", DateUtils.getISODateStr(date));
        result.put("message", message);
        result.put("typeVS", typeVS.toString());
        if(webSocketMessage != null) result.put("webSocketMessage", webSocketMessage.getMessageJSON());
        result.put("messageID", messageID);
        result.put("UUID", getUUID());
        return result;
    }

}
