package org.votingsystem.client.util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    public Notification() {}

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

    public Notification setMessage(String message) {
        this.message = message;
        return this;
    }

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public Notification setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
        return this;
    }

    public String getUUID() {
        return UUID;
    }

    public Notification setUUID(String UUID) {
        this.UUID = UUID;
        return this;
    }

    public static Notification getPlainWalletNotEmptyNotification(List<Cooin> cooinList) {
        Notification notification = new Notification();
        return notification.setMessage(MsgUtils.getPlainWalletNotEmptyMsg(Cooin.getCurrencyMap(
                cooinList))).setTypeVS(TypeVS.COOIN_IMPORT);
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
