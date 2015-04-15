package org.votingsystem.test.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.test.util.TransactionVSCounter;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TimePeriod;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionVSPlanDto {

    private static Logger log = Logger.getLogger(TransactionVSPlanDto.class.getSimpleName());


    private TimePeriod timePeriod;
    private GroupVSDto groupVSDto;
    @JsonIgnore private CurrencyServer currencyServer;
    private List<TransactionVSDto> bankVSList = new ArrayList<>();
    private List<TransactionVSDto> groupVSList = new ArrayList<>();
    private List<TransactionVSDto> userVSList = new ArrayList<>();
    private Map<String, Map<String, BigDecimal>> bankVSBalance;
    private Map<String, Map<String, BigDecimal>> groupVSBalance;


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

    public Map<String, Map<String, BigDecimal>> runBankVSTransactions(String smimeMessageSubject) throws Exception {
        setBankVSBalance(new HashMap<>());
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
            updateCurrencyMap(getBankVSBalance(), transactionVS);
        }
        return bankVSBalance;
    }

    public Map<String, Map<String, BigDecimal>> runGroupVSTransactions(String smimeMessageSubject) throws Exception {
        groupVSBalance = new HashMap<>();
        for(TransactionVSDto transactionVS : getGroupVSList()) {
            transactionVS.setUUID(UUID.randomUUID().toString());
            UserVSDto representative = groupVSDto.getRepresentative();
            SignatureService signatureService = SignatureService.getUserVSSignatureService(
                    representative.getNIF(), UserVS.Type.USER);
            SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(representative.getNIF(),
                    getCurrencyServer().getName(), JSON.getMapper().writeValueAsString(transactionVS), smimeMessageSubject);
            ResponseVS responseVS = HttpHelper.getInstance().sendData(smimeMessage.getBytes(), ContentTypeVS.JSON_SIGNED,
                    getCurrencyServer().getTransactionVSServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            updateCurrencyMap(groupVSBalance, transactionVS);
        }
        return groupVSBalance;
    }

    public void runTransactionsVS(String smimeMessageSubject) throws Exception {
        runBankVSTransactions(smimeMessageSubject);
        runGroupVSTransactions(smimeMessageSubject);
    }

    public static Map<String, Map<String, BigDecimal>> updateCurrencyMap(
            Map<String, Map<String, BigDecimal>> currencyMap, TransactionVSDto transactionVS) {
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

    @JsonIgnore
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

    public Map<String, Map<String, BigDecimal>> getBankVSBalance() {
        return bankVSBalance;
    }

    public void setBankVSBalance(Map<String, Map<String, BigDecimal>> bankVSBalance) {
        this.bankVSBalance = bankVSBalance;
    }

    public Map<String, Map<String, BigDecimal>> getGroupVSBalance() {
        return groupVSBalance;
    }

    public void setGroupVSBalance(Map<String, Map<String, BigDecimal>> groupVSBalance) {
        this.groupVSBalance = groupVSBalance;
    }

    public GroupVSDto getGroupVSDto() {
        return groupVSDto;
    }

    public void setGroupVSDto(GroupVSDto groupVSDto) {
        this.groupVSDto = groupVSDto;
    }
}