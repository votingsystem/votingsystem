package org.currency.test.operation;

import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyCode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;


public class TransactionFromBankTest extends BaseTest {

    private static Logger log =  Logger.getLogger(TransactionFromBankTest.class.getName());


    private static List<CurrencyCode> availableCurrencyCodes =
            Collections.unmodifiableList(Arrays.asList(CurrencyCode.values()));

    public TransactionFromBankTest() {}

/*    public static void main(String[] args) throws Exception {
        new TransactionFromBankTest().runSingleTransaction();
        System.exit(0);
    }


    private static void runSingleTransaction() throws Exception {
        File transactionsPlan = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("transactionsPlan/bank.json"));
        TransactionPlanDto transactionPlanDto = JSON.getMapper().readValue(transactionsPlan, TransactionPlanDto.class);
        transactionPlanDto.setCurrencyServer(currencyServer);
        transactionPlanDto.runTransactions();
        log.info("Transaction report:" + transactionPlanDto.getReport());
        log.info("currencyResultMap: " + transactionPlanDto.getBankBalance());
    }

    private static void runMultipleTransactions() throws Exception {
        int numTransactions = 10;
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

*/
}