package org.votingsystem.cooin.util;

import org.codehaus.groovy.grails.web.json.JSONObject;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.cooin.model.CooinTransactionBatch;
import org.codehaus.groovy.grails.web.json.JSONArray;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinUtils {

    public static CooinTransactionBatch getCooinTransactionBatch(JSONObject jsonObject) throws Exception {
        List<Cooin> cooinList = new ArrayList<Cooin>();
        JSONArray jsonArray = jsonObject.getJSONArray("cooins");
        String csrCooin = null;
        if(jsonObject.containsKey("csrCooin")) csrCooin = jsonObject.getString("csrCooin");
        for(int i = 0; i < jsonArray.length(); i++) {
            SMIMEMessage smimeMessage = new SMIMEMessage(new ByteArrayInputStream(
                    Base64.getDecoder().decode(jsonArray.getString(i).getBytes())));
            try {
                cooinList.add(new Cooin(smimeMessage));
            } catch(Exception ex) {
                throw new ExceptionVS("Error on cooin : '" + i + "' - " + ex.getMessage(), ex);
            }
        }
        return new CooinTransactionBatch(cooinList, csrCooin);
    }

}
