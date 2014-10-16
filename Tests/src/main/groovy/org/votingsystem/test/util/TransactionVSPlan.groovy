package org.votingsystem.test.util

import net.sf.json.JSONArray
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.UserVS
import org.votingsystem.model.VicketServer;
import org.votingsystem.util.FileUtils
import org.votingsystem.vicket.model.TransactionVS;

import java.io.File;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSPlan {

    private static Logger log = Logger.getLogger(TransactionVSPlan.class);

    private VicketServer vicketServer;
    private List<TransactionVS> bankVSTransacionList = new ArrayList<>();
    private List<TransactionVS> groupVSTransacionList = new ArrayList<>();
    private List<TransactionVS> userVSTransacionList = new ArrayList<>();

    public TransactionVSPlan(File transactionVSPlanFile, VicketServer vicketServer) {
        this.vicketServer = vicketServer;
        JSONObject transactionVSPlanJSON = JSONSerializer.toJSON(transactionVSPlanFile.text);
        JSONArray bankVSTransacionArray = transactionVSPlanJSON.getJSONArray("bankVSList");
        for(int i = 0; i < bankVSTransacionArray.size(); i++) {
            JSONObject transactionJSON = bankVSTransacionArray.get(i);
            UserVS fromUserVS = TestUtils.getUserVS(transactionJSON.getLong("fromUserVSId"), vicketServer)
            UserVS toUserVS = TestUtils.getUserVS(transactionJSON.getLong("toUserVSId"), vicketServer)
            TransactionVS transactionVS = TransactionVS.parse(transactionJSON)
            transactionVS.setFromUserVS(fromUserVS)
            transactionVS.setToUserVS(toUserVS)
            bankVSTransacionList.add(transactionVS);
        }
        JSONArray groupVSTransacionArray = transactionVSPlanJSON.getJSONArray("groupVSList");
        for(int i = 0; i < groupVSTransacionArray.size(); i++) {
            JSONObject transactionJSON = groupVSTransacionArray.get(i);
            UserVS fromUserVS = TestUtils.getUserVS(transactionJSON.getLong("fromUserVSId"), vicketServer)
            TransactionVS transactionVS = TransactionVS.parse(transactionJSON)
            if(transactionJSON.getJSONArray("toUserVSList").size() > 0){
                JSONArray toUserVSArray = transactionJSON.getJSONArray("toUserVSList");
                List<String> toUserVSList = new ArrayList<>();
                for(int j = 0; j < toUserVSArray.size(); j++){
                    UserVS toUserVS = TestUtils.getUserVS(toUserVSArray.getLong(j), vicketServer);
                    toUserVSList.add(toUserVS.getIBAN())
                }
                transactionVS.setToUserVSList(toUserVSList);
            }
            transactionVS.setFromUserVS(fromUserVS)
            groupVSTransacionList.add(transactionVS);
        }
        JSONArray userVSTransacionArray = transactionVSPlanJSON.getJSONArray("userVSList");
        for(int i = 0; i < userVSTransacionArray.size(); i++) {
            userVSTransacionList.add(TransactionVS.parse(userVSTransacionArray.get(i)));
        }
    }

    public List<TransactionVS> getBankVSTransacionList() {
        return bankVSTransacionList;
    }

    public List<TransactionVS> getGroupVSTransacionList() {
        return groupVSTransacionList;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal result = BigDecimal.ZERO;
        for(TransactionVS transactionVS : bankVSTransacionList) {
            result.add(transactionVS.getAmount());
        }
        return result;
    }

}