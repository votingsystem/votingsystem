package org.votingsystem.test.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.currency.GroupVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.test.util.TransactionVSCounter;
import org.votingsystem.test.util.TransactionVSUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
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
public class TransactionVSPlanDto {

    private static Logger log = Logger.getLogger(TransactionVSPlanDto.class.getSimpleName());


    private TimePeriod timePeriod;
    private CurrencyServer currencyServer;
    private List<TransactionVSDto> bankVSList = new ArrayList<>();
    private List<TransactionVSDto> groupVSList = new ArrayList<>();
    private List<TransactionVSDto> userVSList = new ArrayList<>();


    public TransactionVSPlanDto() {  }

    public TransactionVSPlanDto(File transactionVSPlanFile, CurrencyServer currencyServer) throws Exception {
        this.setCurrencyServer(currencyServer);
        Map<String, Object> transactionVSPlanJSON = new ObjectMapper().readValue(
                transactionVSPlanFile, new TypeReference<HashMap<String, Object>>() {});
        List bankVSTransacionList = (List) transactionVSPlanJSON.get("bankVSList");
        for(int i = 0; i < bankVSTransacionList.size(); i++) {
            Map transactionMap = (Map) bankVSTransacionList.get(i);
            UserVS fromUserVS = TestUtils.getUserVS(((Number) transactionMap.get("fromUserVSId")).longValue(), currencyServer);
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

    public static Logger getLog() {
        return log;
    }

    public static void setLog(Logger log) {
        TransactionVSPlanDto.log = log;
    }

    public Map runBankVSTransactions(String smimeMessageSubject) throws Exception {
        Map currencyResultMap = new HashMap<>();
        for(TransactionVSDto transactionVS : getBankVSList()) {
            if(UserVS.Type.BANKVS != transactionVS.getFromUserVS().getType()) throw new ExceptionVS("UserVS: " +
                    transactionVS.getFromUserVS().getNIF() + " type is '" +
                    transactionVS.getFromUserVS().getType().toString() + "' not a 'BANKVS'");
            SignatureService signatureService = SignatureService.getUserVSSignatureService(
                    transactionVS.getFromUserVS().getNIF(), UserVS.Type.BANKVS);

            SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(transactionVS.getFromUserVS().getNIF(),
                    getCurrencyServer().getName(), JSON.getMapper().writeValueAsString(transactionVS), smimeMessageSubject);
            ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    getCurrencyServer().getTransactionVSServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            updateCurrencyMap(currencyResultMap, transactionVS);
        }
        return currencyResultMap;
    }

    public Map runGroupVSTransactions(String smimeMessageSubject) throws Exception {
        Map currencyResultMap = new HashMap<>();
        for(TransactionVSDto transactionVS : getGroupVSList()) {

            UserVS representative = transactionVS.getGroupVS().getRepresentative();
            SignatureService signatureService = SignatureService.getUserVSSignatureService(
                    representative.getNif(), UserVS.Type.USER);
            
            SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(representative.getNif(),
                    getCurrencyServer().getName(), JSON.getMapper().writeValueAsString(transactionVS), smimeMessageSubject);
            ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    getCurrencyServer().getTransactionVSServiceURL());
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

    public static Map updateCurrencyMap(Map<String, Map<String, BigDecimal>> currencyMap, TransactionVSDto transactionVS) {
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



    public List<TransactionVSDto> getTransacionList() {
        List<TransactionVSDto> result = new ArrayList<>();
        result.addAll(getBankVSList());
        result.addAll(getGroupVSList());
        result.addAll(getUserVSList());
        return result;
    }


    public Map getReport() {
        List<TransactionVSDto> transactionsVSList = getTransacionList();
        Map<String, TransactionVSCounter> resultMap = new HashMap<>();
        for(TransactionVSDto transactionVS : transactionsVSList) {
            if(resultMap.containsKey(transactionVS.getType().toString()))
                resultMap.get(transactionVS.getType().toString()).addTransaction(transactionVS.getAmount());
            else resultMap.put(transactionVS.getType().toString(), new TransactionVSCounter(transactionVS));
        }
        return resultMap;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal result = BigDecimal.ZERO;
        for(TransactionVSDto transactionVS : bankVSList) {
            result.add(transactionVS.getAmount());
        }
        return result;
    }

    public void setTimePeriod(TimePeriod timePeriod) {
        this.timePeriod = timePeriod;
    }

    public CurrencyServer getCurrencyServer() {
        return currencyServer;
    }

    public void setCurrencyServer(CurrencyServer currencyServer) {
        this.currencyServer = currencyServer;
    }

    public List<TransactionVSDto> getBankVSList() {
        return bankVSList;
    }

    public void setBankVSList(List<TransactionVSDto> bankVSList) {
        this.bankVSList = bankVSList;
    }

    public List<TransactionVSDto> getGroupVSList() {
        return groupVSList;
    }

    public void setGroupVSList(List<TransactionVSDto> groupVSList) {
        this.groupVSList = groupVSList;
    }

    public List<TransactionVSDto> getUserVSList() {
        return userVSList;
    }

    public void setUserVSList(List<TransactionVSDto> userVSList) {
        this.userVSList = userVSList;
    }
}