package org.votingsystem.test.currency;

import com.google.common.collect.Sets;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.UserDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.CurrencyCode;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.test.dto.TransactionPlanDto;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.KeyGenerator;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;


public class SendTransactionFromBank {

    private static Logger log =  Logger.getLogger(SendTransactionFromBank.class.getName());

    private static CurrencyServer currencyServer;
    private static List<CurrencyCode> availableCurrencyCodes =
            Collections.unmodifiableList(Arrays.asList(CurrencyCode.values()));
    private static List<String> availableTags =  Collections.unmodifiableList(
            Arrays.asList("HOME", "WEALTH", "ENERGY", "FOOD", "CULTURE", "TRANSPORT"));

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        currencyServer = TestUtils.fetchCurrencyServer();
        runMultipleTransactions();
        System.exit(0);
    }


    private static void runSingleTransaction() throws Exception {
        File transactionsPlan = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("transactionsPlan/bank.json"));
        TransactionPlanDto transactionPlanDto = JSON.getMapper().readValue(transactionsPlan, TransactionPlanDto.class);
        transactionPlanDto.setCurrencyServer(currencyServer);
        transactionPlanDto.runTransactionsVS();
        log.info("Transaction report:" + transactionPlanDto.getReport());
        log.info("currencyResultMap: " + transactionPlanDto.getBankBalance());
    }

    private static void runMultipleTransactions() throws Exception {
        int numTransactions = 10000;
        Long fromUserId = 3L;
        Long toUserId = 2L;
        int amountLimit = 100;
        for(int i = 0; i < numTransactions; i++) {
            TransactionDto transaction = new TransactionDto();
            transaction.setOperation(TypeVS.FROM_BANK);
            transaction.setType(Transaction.Type.FROM_BANK);
            transaction.setSubject("SendTransactionFromBank - runMultipleTransactions - " + DateUtils.getDateStr(new Date()));
            transaction.setCurrencyCode(availableCurrencyCodes.get(
                    KeyGenerator.INSTANCE.getNextRandomInt(availableCurrencyCodes.size())));
            Set<String> tags = Sets.newHashSet(availableTags.get(
                    KeyGenerator.INSTANCE.getNextRandomInt(availableTags.size())));
            transaction.setTags(tags);
            transaction.setAmount(new BigDecimal(KeyGenerator.INSTANCE.getNextRandomInt(amountLimit)));
            UserDto fromUser = TestUtils.getUser(fromUserId, currencyServer);
            transaction.setFromUser(fromUser);
            UserDto toUser = TestUtils.getUser(toUserId, currencyServer);
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
                    currencyServer.getTransactionServiceURL());
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        }
    }


}