package org.votingsystem.test.util

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.cooin.model.TransactionVS

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSPlan {

    private static Logger log = Logger.getLogger(TransactionVSPlan.class);


    private DateUtils.TimePeriod timePeriod;
    private CooinServer cooinServer;
    private List<TransactionVS> bankVSTransacionList = new ArrayList<>();
    private List<TransactionVS> groupVSTransacionList = new ArrayList<>();
    private List<TransactionVS> userVSTransacionList = new ArrayList<>();


    public TransactionVSPlan() {

    }

    public TransactionVSPlan(File transactionVSPlanFile, CooinServer cooinServer) {
        this.cooinServer = cooinServer;
        JSONObject transactionVSPlanJSON = JSONSerializer.toJSON(transactionVSPlanFile.text);
        JSONArray bankVSTransacionArray = transactionVSPlanJSON.getJSONArray("bankVSList");
        for(int i = 0; i < bankVSTransacionArray.size(); i++) {
            JSONObject transactionJSON = bankVSTransacionArray.get(i);
            UserVS fromUserVS = TestUtils.getUserVS(transactionJSON.getLong("fromUserVSId"), cooinServer)
            UserVS toUserVS = TestUtils.getUserVS(transactionJSON.getLong("toUserVSId"), cooinServer)
            TransactionVS transactionVS = TransactionVS.parse(transactionJSON)
            transactionVS.setFromUserVS(fromUserVS)
            transactionVS.setToUserVS(toUserVS)
            bankVSTransacionList.add(transactionVS);
        }
        JSONArray groupVSTransacionArray = transactionVSPlanJSON.getJSONArray("groupVSList");
        for(int i = 0; i < groupVSTransacionArray.size(); i++) {
            JSONObject transactionJSON = groupVSTransacionArray.get(i);
            UserVS fromUserVS = TestUtils.getUserVS(transactionJSON.getLong("fromUserVSId"), cooinServer)
            TransactionVS transactionVS = TransactionVS.parse(transactionJSON)
            if(transactionJSON.getJSONArray("toUserVSList").size() > 0){
                JSONArray toUserVSArray = transactionJSON.getJSONArray("toUserVSList");
                List<String> toUserVSList = new ArrayList<>();
                for(int j = 0; j < toUserVSArray.size(); j++){
                    UserVS toUserVS = TestUtils.getUserVS(toUserVSArray.getLong(j), cooinServer);
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

    public Map runBankVSTransactions(String smimeMessageSubject) {
        Map currencyResultMap = [:]
        for(TransactionVS transactionVS :bankVSTransacionList) {
            if(UserVS.Type.BANKVS != transactionVS.fromUserVS.type) throw new ExceptionVS("UserVS: " +
                    transactionVS.fromUserVS.nif + " type is '" + transactionVS.fromUserVS.type.toString() +
                    "' not a 'BANKVS'");

            SignatureService signatureService = SignatureService.getUserVSSignatureService(
                    transactionVS.fromUserVS.nif, UserVS.Type.BANKVS)
            SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(transactionVS.fromUserVS.nif,
                    cooinServer.getNameNormalized(), JSONSerializer.toJSON(
                    TransactionVSUtils.getBankVSTransactionVS(transactionVS)).toString(), smimeMessageSubject)
            ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    cooinServer.getTransactionVSServiceURL())
            if(ResponseVS.SC_OK != responseVS.statusCode) throw new ExceptionVS(responseVS.getMessage())
            updateCurrencyMap(currencyResultMap, transactionVS)
        }
        return currencyResultMap
    }

    public Map runGroupVSTransactions(String smimeMessageSubject) {
        Map currencyResultMap = [:]
        for(TransactionVS transactionVS : groupVSTransacionList) {
            UserVS representative = ((GroupVS)transactionVS.fromUserVS).representative
            SignatureService signatureService = SignatureService.getUserVSSignatureService(
                    representative.nif, UserVS.Type.GROUP)
            SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(representative.nif,
                    cooinServer.getNameNormalized(), JSONSerializer.toJSON(
                    TransactionVSUtils.getGroupVSTransactionVS(transactionVS, transactionVS.fromUserVS)).toString(),
                    smimeMessageSubject)
            ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    cooinServer.getTransactionVSServiceURL())
            if(ResponseVS.SC_OK != responseVS.statusCode) throw new ExceptionVS(responseVS.getMessage())
            updateCurrencyMap(currencyResultMap, transactionVS)
        }
        return currencyResultMap
    }

    public Map runTransactionsVS(String smimeMessageSubject) {
        Map bankVSCurrencyResult = runBankVSTransactions(smimeMessageSubject)
        Map groupVSCurrencyResult = runGroupVSTransactions(smimeMessageSubject)
        return [bankVSCurrencyResult:bankVSCurrencyResult, groupVSCurrencyResult:groupVSCurrencyResult]
    }

    public static Map updateCurrencyMap(Map currencyMap, TransactionVS transactionVS) {
        if(currencyMap[transactionVS.currencyCode]) {
            if(currencyMap[transactionVS.currencyCode][transactionVS.tag.name]) {
                currencyMap[transactionVS.currencyCode][(transactionVS.tag.name)] =
                        currencyMap[transactionVS.currencyCode][(transactionVS.tag.name)].add(transactionVS.amount)
            } else currencyMap[transactionVS.currencyCode][(transactionVS.tag.name)] = transactionVS.amount
        } else currencyMap[(transactionVS.currencyCode)] = [(transactionVS.tag.name):transactionVS.amount]
        return currencyMap;
    }

    public DateUtils.TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public List<TransactionVS> getBankVSTransacionList() {
        return bankVSTransacionList;
    }

    public List<TransactionVS> getGroupVSTransacionList() {
        return groupVSTransacionList;
    }

    public List<TransactionVS> getUserVSTransacionList() {
        return userVSTransacionList;
    }

    public List<TransactionVS> getTransacionList() {
        List<TransactionVS> result = new ArrayList<>();
        result.addAll(getBankVSTransacionList());
        result.addAll(getGroupVSTransacionList());
        result.addAll(getUserVSTransacionList());
        return result;
    }


    public Map getReport() {
        List<TransactionVS> transactionsVSList = getTransacionList();
        Map<String, TransactionVSCounter> resultMap = new HashMap<>()
        for(TransactionVS transactionVS : transactionsVSList) {
            if(resultMap.containsKey(transactionVS.getType().toString()))
                resultMap.get(transactionVS.getType().toString()).addTransaction(transactionVS.amount)
            else resultMap.put(transactionVS.getType().toString(), new TransactionVSCounter(transactionVS));
        }
        return resultMap
    }

    public BigDecimal getTotalAmount() {
        BigDecimal result = BigDecimal.ZERO;
        for(TransactionVS transactionVS : bankVSTransacionList) {
            result.add(transactionVS.getAmount());
        }
        return result;
    }

}