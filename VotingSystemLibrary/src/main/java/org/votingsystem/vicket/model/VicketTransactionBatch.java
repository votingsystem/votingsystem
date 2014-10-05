package org.votingsystem.vicket.model;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.bouncycastle.util.encoders.Base64;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VicketTransactionBatch {

    List<Vicket> vicketList;

    public VicketTransactionBatch(String vicketsArrayStr) throws Exception {
        JSONArray vicketsArray = (JSONArray) JSONSerializer.toJSON(vicketsArrayStr);
        vicketList = new ArrayList<Vicket>();
        for(int i = 0; i < vicketsArray.size(); i++) {
            SMIMEMessage smimeMessage = new SMIMEMessage(new ByteArrayInputStream(
                    Base64.decode(vicketsArray.getString(i).getBytes())));
            try {
                vicketList.add(new Vicket(smimeMessage));
            } catch(Exception ex) {
                throw new ExceptionVS("Error on vicket number: '" + i + "' - " + ex.getMessage(), ex);
            }
        }
    }

    public List<Vicket> getVicketList() {
        return vicketList;
    }

    public void setVicketList(List<Vicket> vicketList) {
        this.vicketList = vicketList;
    }

}
