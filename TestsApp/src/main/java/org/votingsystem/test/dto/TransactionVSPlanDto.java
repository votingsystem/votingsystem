package org.votingsystem.test.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.GroupVSDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.test.util.TransactionVSCounter;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.Interval;
import org.votingsystem.util.JSON;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionVSPlanDto {

    private static Logger log = Logger.getLogger(TransactionVSPlanDto.class.getName());


    private Interval timePeriod;
    private GroupVSDto groupVSDto;
    @JsonIgnore private CurrencyServer currencyServer;
    private List<TransactionVSDto> bankVSList = new ArrayList<>();
    private List<TransactionVSDto> groupVSList = new ArrayList<>();
    private List<TransactionVSDto> userVSList = new ArrayList<>();
    private Map<String, Map<String, BigDecimal>> bankVSBalance;
    private Map<String, Map<String, BigDecimal>> groupVSBalance;


    public TransactionVSPlanDto() {  }

    public Map<String, Map<String, BigDecimal>> runBankVSTransactions() throws Exception {
        setBankVSBalance(new HashMap<>());
        for(TransactionVSDto transactionVS : getBankVSList()) {
            transactionVS.setType(TransactionVS.Type.FROM_BANKVS);
            UserVSDto fromUserVS = TestUtils.getUserVS(transactionVS.getFromUserVS().getId(), currencyServer);
            transactionVS.setFromUserVS(fromUserVS);
            UserVSDto toUserVS = TestUtils.getUserVS(transactionVS.getToUserVS().getId(), currencyServer);
            transactionVS.setToUserVS(toUserVS);
            transactionVS.loadBankVSTransaction(UUID.randomUUID().toString());
            if(UserVS.Type.BANKVS != transactionVS.getFromUserVS().getType()) throw new ExceptionVS("UserVS: " +
                    transactionVS.getFromUserVS().getNIF() + " type is '" +
                    transactionVS.getFromUserVS().getType().toString() + "' not a 'BANKVS'");

            SignatureService signatureService = SignatureService.getUserVSSignatureService(
                        transactionVS.getFromUserVS().getNIF(), UserVS.Type.BANKVS);
            CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(
                    JSON.getMapper().writeValueAsBytes(transactionVS));
            ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                    getCurrencyServer().getTransactionVSServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            updateCurrencyMap(bankVSBalance, transactionVS);
        }
        return bankVSBalance;
    }

    public Map<String, Map<String, BigDecimal>> runGroupVSTransactions() throws Exception {
        groupVSBalance = new HashMap<>();
        for(TransactionVSDto transactionVS : getGroupVSList()) {
            transactionVS.setFromUserIBAN(groupVSDto.getIBAN());
            transactionVS.setUUID(UUID.randomUUID().toString());
            UserVSDto representative = groupVSDto.getRepresentative();
            SignatureService signatureService = SignatureService.getUserVSSignatureService(
                    representative.getNIF(), UserVS.Type.USER);
            CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(
                    JSON.getMapper().writeValueAsBytes(transactionVS));
            ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                    getCurrencyServer().getTransactionVSServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            updateCurrencyMap(groupVSBalance, transactionVS);
        }
        return groupVSBalance;
    }

    public void runTransactionsVS() throws Exception {
        runBankVSTransactions();
        runGroupVSTransactions();
    }

    public static Map<String, Map<String, BigDecimal>> updateCurrencyMap(
            Map<String, Map<String, BigDecimal>> currencyMap, TransactionVSDto transactionVS) {
        if(currencyMap.containsKey(transactionVS.getCurrencyCode())) {
            if(currencyMap.get(transactionVS.getCurrencyCode()).containsKey(transactionVS.getTagName())) {
                BigDecimal newAmount = ((BigDecimal)currencyMap.get(transactionVS.getCurrencyCode()).get(
                        transactionVS.getTagName())).add(transactionVS.getAmount());
                currencyMap.get(transactionVS.getCurrencyCode()).put(transactionVS.getTagName(), newAmount);
            } else currencyMap.get(transactionVS.getCurrencyCode()).put(transactionVS.getTagName(), transactionVS.getAmount());
        } else {
            Map tagMap = new HashMap<>();
            tagMap.put(transactionVS.getTagName(), transactionVS.getAmount());
            currencyMap.put(transactionVS.getCurrencyCode(), tagMap);
        }
        return currencyMap;
    }

    public Interval getTimePeriod() {
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
            if(resultMap.containsKey(transactionVS.getOperation().toString()))
                resultMap.get(transactionVS.getType().toString()).addTransaction(transactionVS.getAmount());
            else resultMap.put(transactionVS.getOperation().toString(), new TransactionVSCounter(transactionVS));
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

    public void setTimePeriod(Interval timePeriod) {
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