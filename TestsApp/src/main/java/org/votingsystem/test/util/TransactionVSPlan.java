package org.votingsystem.test.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.TimePeriod;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSPlan {

    private static Logger log = Logger.getLogger(TransactionVSPlan.class.getSimpleName());


    private TimePeriod timePeriod;
    private CurrencyServer currencyServer;
    private List<TransactionVS> bankVSTransacionList = new ArrayList<>();
    private List<TransactionVS> groupVSTransacionList = new ArrayList<>();
    private List<TransactionVS> userVSTransacionList = new ArrayList<>();


    public TransactionVSPlan() {  }

    public TransactionVSPlan(File transactionVSPlanFile, CurrencyServer currencyServer) throws Exception {
        this.currencyServer = currencyServer;
        Map<String, Object> transactionVSPlanJSON = new ObjectMapper().readValue(
                transactionVSPlanFile, new TypeReference<HashMap<String, Object>>() {});
        List bankVSTransacionList = (List) transactionVSPlanJSON.get("bankVSList");
        for(int i = 0; i < bankVSTransacionList.size(); i++) {
            Map transactionMap = (Map) bankVSTransacionList.get(i);
            UserVS fromUserVS = TestUtils.getUserVS(((Number)transactionMap.get("fromUserVSId")).longValue(), currencyServer);
            UserVS toUserVS = TestUtils.getUserVS(((Number)transactionMap.get("toUserVSId")).longValue(), currencyServer);
            TransactionVS transactionVS = TransactionVS.parse(transactionMap);
            transactionVS.setFromUserVS(fromUserVS);
            transactionVS.setToUserVS(toUserVS);
            bankVSTransacionList.add(transactionVS);
        }
        List groupVSTransacionList = (List) transactionVSPlanJSON.get("groupVSList");
        for(int i = 0; i < groupVSTransacionList.size(); i++) {
            Map transactionMap = (Map) groupVSTransacionList.get(i);
            UserVS fromUserVS = TestUtils.getUserVS(((Number)transactionMap.get("fromUserVSId")).longValue(), currencyServer);
            TransactionVS transactionVS = TransactionVS.parse(transactionMap);
            if(transactionMap.get("toUserVSList") instanceof List){
                List paramList = (List) transactionMap.get("toUserVSList");
                List<String> toUserVSList = new ArrayList<>();
                for(int j = 0; j < paramList.size(); j++){
                    UserVS toUserVS = TestUtils.getUserVS(((Number)paramList.get(j)).longValue(), currencyServer);
                    toUserVSList.add(toUserVS.getIBAN());
                }
                transactionVS.setToUserVSList(toUserVSList);
            }
            transactionVS.setFromUserVS(fromUserVS);
            groupVSTransacionList.add(transactionVS);
        }
        List userVSTransacionList = (List) transactionVSPlanJSON.get("userVSList");
        for(int i = 0; i < userVSTransacionList.size(); i++) {
            userVSTransacionList.add(TransactionVS.parse((Map) userVSTransacionList.get(i)));
        }
    }

    public Map runBankVSTransactions(String smimeMessageSubject) throws Exception {
        Map currencyResultMap = new HashMap<>();
        for(TransactionVS transactionVS :bankVSTransacionList) {
            if(UserVS.Type.BANKVS != transactionVS.getFromUserVS().getType()) throw new ExceptionVS("UserVS: " +
                    transactionVS.getFromUserVS().getNif() + " type is '" +
                    transactionVS.getFromUserVS().getType().toString() + "' not a 'BANKVS'");
            SignatureService signatureService = SignatureService.getUserVSSignatureService(
                    transactionVS.getFromUserVS().getNif(), UserVS.Type.BANKVS);

            SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(transactionVS.getFromUserVS().getNif(),
                    currencyServer.getName(),
                    new ObjectMapper().writeValueAsString(TransactionVSUtils.getBankVSTransactionVS(transactionVS))
                    , smimeMessageSubject);
            ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    currencyServer.getTransactionVSServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            updateCurrencyMap(currencyResultMap, transactionVS);
        }
        return currencyResultMap;
    }

    public Map runGroupVSTransactions(String smimeMessageSubject) throws Exception {
        Map currencyResultMap = new HashMap<>();
        for(TransactionVS transactionVS : groupVSTransacionList) {
            UserVS representative = ((GroupVS)transactionVS.getFromUserVS()).getRepresentative();
            SignatureService signatureService = SignatureService.getUserVSSignatureService(
                    representative.getNif(), UserVS.Type.USER);
            
            SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(representative.getNif(),
                    currencyServer.getName(), new ObjectMapper().writeValueAsString(TransactionVSUtils.getGroupVSTransactionVS(
                            transactionVS, transactionVS.getFromUserVS())), smimeMessageSubject);
            ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    currencyServer.getTransactionVSServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            updateCurrencyMap(currencyResultMap, transactionVS);
        }
        return currencyResultMap;
    }

    public Map runTransactionsVS(String smimeMessageSubject) throws Exception {
        Map bankVSCurrencyResult = runBankVSTransactions(smimeMessageSubject);
        Map groupVSCurrencyResult = runGroupVSTransactions(smimeMessageSubject);
        Map result = new HashMap<>();
        result.put("bankVSCurrencyResult", bankVSCurrencyResult);
        result.put("groupVSCurrencyResult", groupVSCurrencyResult);
        return result;
    }

    public static Map updateCurrencyMap(Map<String, Map> currencyMap, TransactionVS transactionVS) {
        if(currencyMap.containsKey(transactionVS.getCurrencyCode())) {
            if(currencyMap.get(transactionVS.getCurrencyCode()).containsKey(transactionVS.getTag().getName())) {
                BigDecimal newAmount = ((BigDecimal)currencyMap.get(transactionVS.getCurrencyCode()).get(
                        transactionVS.getTag().getName())).add(transactionVS.getAmount());
                currencyMap.get(transactionVS.getCurrencyCode()).put(transactionVS.getTag().getName(), newAmount);
            } else currencyMap.get(transactionVS.getCurrencyCode()).put(transactionVS.getTag().getName(), transactionVS.getAmount());
        } else {
            Map tagMap = new HashMap<>();
            tagMap.put(transactionVS.getTag().getName(), transactionVS.getAmount());
            currencyMap.put(transactionVS.getCurrencyCode(), tagMap);
        }
        return currencyMap;
    }

    public TimePeriod getTimePeriod() {
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
        Map<String, TransactionVSCounter> resultMap = new HashMap<>();
        for(TransactionVS transactionVS : transactionsVSList) {
            if(resultMap.containsKey(transactionVS.getType().toString()))
                resultMap.get(transactionVS.getType().toString()).addTransaction(transactionVS.getAmount());
            else resultMap.put(transactionVS.getType().toString(), new TransactionVSCounter(transactionVS));
        }
        return resultMap;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal result = BigDecimal.ZERO;
        for(TransactionVS transactionVS : bankVSTransacionList) {
            result.add(transactionVS.getAmount());
        }
        return result;
    }

}