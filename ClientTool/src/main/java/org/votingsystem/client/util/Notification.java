package org.votingsystem.client.util;

import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import java.text.ParseException;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Notification {

    private static Logger log = Logger.getLogger(Notification.class);

    private TypeVS typeVS;
    private String message;
    private Date date;
    private String UUID;

    public Notification(JSONObject jsonObject) throws ParseException {
        typeVS = TypeVS.valueOf(jsonObject.getString("typeVS"));
        message = jsonObject.getString("message");
        date = DateUtils.getDayWeekDate(jsonObject.getString("date"));
        UUID = jsonObject.getString("UUID");
    }

    public Date getDate() {
        return date;
    }

    public Notification setDate(Date date) {
        this.date = date;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public String getUUID() {
        return UUID;
    }

    public Notification setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        result.put("date", DateUtils.getDayWeekDateStr(date));
        result.put("message", message);
        result.put("typeVS", typeVS.toString());
        result.put("UUID", getUUID());
        return result;
    }

}
