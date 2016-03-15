package org.votingsystem.test.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.currency.GroupDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.test.util.TransactionCounter;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentType;
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
public class TransactionPlanDto {

    private static Logger log = Logger.getLogger(TransactionPlanDto.class.getName());


    private Interval timePeriod;
    private GroupDto groupDto;
    @JsonIgnore private CurrencyServer currencyServer;
    private List<TransactionDto> bankList = new ArrayList<>();
    private List<TransactionDto> groupList = new ArrayList<>();
    private List<TransactionDto> userList = new ArrayList<>();
    private Map<String, Map<String, BigDecimal>> bankBalance;
    private Map<String, Map<String, BigDecimal>> groupBalance;


    public TransactionPlanDto() {  }

    public Map<String, Map<String, BigDecimal>> runBankTransactions() throws Exception {
        setBankBalance(new HashMap<>());
        for(TransactionDto transaction : getBankList()) {
            transaction.setType(Transaction.Type.FROM_BANK);
            UserDto fromUser = TestUtils.getUser(transaction.getFromUser().getId(), currencyServer);
            transaction.setFromUser(fromUser);
            UserDto toUser = TestUtils.getUser(transaction.getToUser().getId(), currencyServer);
            transaction.setToUser(toUser);
            transaction.loadBankTransaction(UUID.randomUUID().toString());
            if(User.Type.BANK != transaction.getFromUser().getType()) throw new ExceptionVS("User: " +
                    transaction.getFromUser().getNIF() + " type is '" +
                    transaction.getFromUser().getType().toString() + "' not a 'BANK'");

            SignatureService signatureService = SignatureService.getUserSignatureService(
                        transaction.getFromUser().getNIF(), User.Type.BANK);
            CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(
                    JSON.getMapper().writeValueAsBytes(transaction));
            ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentType.JSON_SIGNED,
                    getCurrencyServer().getTransactionServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            updateCurrencyMap(bankBalance, transaction);
        }
        return bankBalance;
    }

    public Map<String, Map<String, BigDecimal>> runGroupTransactions() throws Exception {
        groupBalance = new HashMap<>();
        for(TransactionDto transaction : getGroupList()) {
            transaction.setFromUserIBAN(groupDto.getIBAN());
            transaction.setUUID(UUID.randomUUID().toString());
            UserDto representative = groupDto.getRepresentative();
            SignatureService signatureService = SignatureService.getUserSignatureService(
                    representative.getNIF(), User.Type.USER);
            CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(
                    JSON.getMapper().writeValueAsBytes(transaction));
            ResponseVS responseVS = HttpHelper.getInstance().sendData(cmsMessage.toPEM(), ContentType.JSON_SIGNED,
                    getCurrencyServer().getTransactionServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            updateCurrencyMap(groupBalance, transaction);
        }
        return groupBalance;
    }

    public void runTransactionsVS() throws Exception {
        runBankTransactions();
        runGroupTransactions();
    }

    public static Map<String, Map<String, BigDecimal>> updateCurrencyMap(
            Map<String, Map<String, BigDecimal>> currencyMap, TransactionDto transaction) {
        if(currencyMap.containsKey(transaction.getCurrencyCode())) {
            if(currencyMap.get(transaction.getCurrencyCode()).containsKey(transaction.getTagName())) {
                BigDecimal newAmount = ((BigDecimal)currencyMap.get(transaction.getCurrencyCode()).get(
                        transaction.getTagName())).add(transaction.getAmount());
                currencyMap.get(transaction.getCurrencyCode()).put(transaction.getTagName(), newAmount);
            } else currencyMap.get(transaction.getCurrencyCode()).put(transaction.getTagName(), transaction.getAmount());
        } else {
            Map tagMap = new HashMap<>();
            tagMap.put(transaction.getTagName(), transaction.getAmount());
            currencyMap.put(transaction.getCurrencyCode(), tagMap);
        }
        return currencyMap;
    }

    public Interval getTimePeriod() {
        return timePeriod;
    }

    public List<TransactionDto> getTransacionList() {
        List<TransactionDto> result = new ArrayList<>();
        result.addAll(getBankList());
        result.addAll(getGroupList());
        result.addAll(getUserList());
        return result;
    }

    @JsonIgnore
    public Map getReport() {
        List<TransactionDto> transactionsVSList = getTransacionList();
        Map<String, TransactionCounter> resultMap = new HashMap<>();
        for(TransactionDto transaction : transactionsVSList) {
            if(resultMap.containsKey(transaction.getOperation().toString()))
                resultMap.get(transaction.getType().toString()).addTransaction(transaction.getAmount());
            else resultMap.put(transaction.getOperation().toString(), new TransactionCounter(transaction));
        }
        return resultMap;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal result = BigDecimal.ZERO;
        for(TransactionDto transaction : bankList) {
            result.add(transaction.getAmount());
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

    public List<TransactionDto> getBankList() {
        return bankList;
    }

    public void setBankList(List<TransactionDto> bankList) {
        this.bankList = bankList;
    }

    public List<TransactionDto> getGroupList() {
        return groupList;
    }

    public void setGroupList(List<TransactionDto> groupList) {
        this.groupList = groupList;
    }

    public List<TransactionDto> getUserList() {
        return userList;
    }

    public void setUserList(List<TransactionDto> userList) {
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