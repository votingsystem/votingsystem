package org.votingsystem.test.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.currency.GroupDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
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
    private GroupDto groupDto;
    @JsonIgnore private CurrencyServer currencyServer;
    private List<TransactionVSDto> bankList = new ArrayList<>();
    private List<TransactionVSDto> groupList = new ArrayList<>();
    private List<TransactionVSDto> userList = new ArrayList<>();
    private Map<String, Map<String, BigDecimal>> bankBalance;
    private Map<String, Map<String, BigDecimal>> groupBalance;


    public TransactionVSPlanDto() {  }

    public Map<String, Map<String, BigDecimal>> runBankTransactions() throws Exception {
        setBankBalance(new HashMap<>());
        for(TransactionVSDto transactionVS : getBankList()) {
            transactionVS.setType(TransactionVS.Type.FROM_BANK);
            UserDto fromUser = TestUtils.getUser(transactionVS.getFromUser().getId(), currencyServer);
            transactionVS.setFromUser(fromUser);
            UserDto toUser = TestUtils.getUser(transactionVS.getToUser().getId(), currencyServer);
            transactionVS.setToUser(toUser);
            transactionVS.loadBankTransaction(UUID.randomUUID().toString());
            if(User.Type.BANK != transactionVS.getFromUser().getType()) throw new ExceptionVS("User: " +
                    transactionVS.getFromUser().getNIF() + " type is '" +
                    transactionVS.getFromUser().getType().toString() + "' not a 'BANK'");

            SignatureService signatureService = SignatureService.getUserSignatureService(
                        transactionVS.getFromUser().getNIF(), User.Type.BANK);
            CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(
                    JSON.getMapper().writeValueAsBytes(transactionVS));
            ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                    getCurrencyServer().getTransactionVSServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            updateCurrencyMap(bankBalance, transactionVS);
        }
        return bankBalance;
    }

    public Map<String, Map<String, BigDecimal>> runGroupTransactions() throws Exception {
        groupBalance = new HashMap<>();
        for(TransactionVSDto transactionVS : getGroupList()) {
            transactionVS.setFromUserIBAN(groupDto.getIBAN());
            transactionVS.setUUID(UUID.randomUUID().toString());
            UserDto representative = groupDto.getRepresentative();
            SignatureService signatureService = SignatureService.getUserSignatureService(
                    representative.getNIF(), User.Type.USER);
            CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(
                    JSON.getMapper().writeValueAsBytes(transactionVS));
            ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentTypeVS.JSON_SIGNED,
                    getCurrencyServer().getTransactionVSServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            updateCurrencyMap(groupBalance, transactionVS);
        }
        return groupBalance;
    }

    public void runTransactionsVS() throws Exception {
        runBankTransactions();
        runGroupTransactions();
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
        result.addAll(getBankList());
        result.addAll(getGroupList());
        result.addAll(getUserList());
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
        for(TransactionVSDto transactionVS : bankList) {
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

    public List<TransactionVSDto> getBankList() {
        return bankList;
    }

    public void setBankList(List<TransactionVSDto> bankList) {
        this.bankList = bankList;
    }

    public List<TransactionVSDto> getGroupList() {
        return groupList;
    }

    public void setGroupList(List<TransactionVSDto> groupList) {
        this.groupList = groupList;
    }

    public List<TransactionVSDto> getUserList() {
        return userList;
    }

    public void setUserList(List<TransactionVSDto> userList) {
        this.userList = userList;
    }

    public Map<String, Map<String, BigDecimal>> getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(Map<String, Map<String, BigDecimal>> bankBalance) {
        this.bankBalance = bankBalance;
    }

    public Map<String, Map<String, BigDecimal>> getGroupBalance() {
        return groupBalance;
    }

    public void setGroupBalance(Map<String, Map<String, BigDecimal>> groupBalance) {
        this.groupBalance = groupBalance;
    }

    public GroupDto getGroupDto() {
        return groupDto;
    }

    public void setGroupDto(GroupDto groupDto) {
        this.groupDto = groupDto;
    }
}