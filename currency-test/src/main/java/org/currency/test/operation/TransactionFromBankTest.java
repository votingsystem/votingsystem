package org.currency.test.operation;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.currency.test.Constants;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.TSPHttpSource;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.OperationType;
import org.votingsystem.xades.XAdESSignature;
import org.votingsystem.xml.XML;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;


public class TransactionFromBankTest extends BaseTest {

    private static Logger log =  Logger.getLogger(TransactionFromBankTest.class.getName());


    private static List<CurrencyCode> availableCurrencyCodes =
            Collections.unmodifiableList(Arrays.asList(CurrencyCode.values()));

    public TransactionFromBankTest() {}


    public static void main(String[] args) throws Exception {
        new TransactionFromBankTest().runSingleTransaction();
        System.exit(0);
    }


    private void runSingleTransaction() throws Exception {
        TransactionDto transaction = new TransactionDto(new OperationTypeDto(CurrencyOperation.TRANSACTION_FROM_BANK,
                Constants.CURRENCY_SERVICE_ENTITY_ID));
        transaction.setOperation(new OperationTypeDto(CurrencyOperation.TRANSACTION_FROM_BANK, Constants.CURRENCY_SERVICE_ENTITY_ID))
                .setUUID(UUID.randomUUID().toString());
        transaction.setSubject("Test transaction").setAmount(new BigDecimal(4)).setCurrencyCode(CurrencyCode.CNY);
        transaction.setToUserIBAN("ES4078788989450000000013");
        /*UserDto fromUser = TestUtils.getUser(transaction.getFromUser().getId(), currencyServer);
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
        updateCurrencyMap(bankBalance, transaction);*/




        MockDNIe mockDNIe = new MockDNIe("ExternalBank");
        //MockDNIe mockDNIe = new MockDNIe("08888888D");
        byte[] signedBytes =  XAdESSignature.sign(XML.getMapper().writeValueAsBytes(transaction),
                mockDNIe.getJksSignatureToken(), new TSPHttpSource(Constants.TIMESTAMP_SERVICE_URL));

        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(signedBytes, MediaType.XML,
                CurrencyOperation.TRANSACTION_FROM_BANK.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID));
        log.info("statusCode: " + responseDto.getStatusCode() + " - Message: " + responseDto.getMessage());
        /*File transactionsPlan = FileUtils.getFileFromBytes(ContextVS.getInstance().getResourceBytes("transactionsPlan/bank.json"));
        TransactionPlanDto transactionPlanDto = JSON.getMapper().readValue(transactionsPlan, TransactionPlanDto.class);
        transactionPlanDto.setCurrencyServer(currencyServer);
        transactionPlanDto.runTransactions();
        log.info("Transaction report:" + transactionPlanDto.getReport());
        log.info("currencyResultMap: " + transactionPlanDto.getBankBalance());*/
    }

    public void validateSignedDocument(byte[] signedDocumentBytes) {
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("signedXML", new String(signedDocumentBytes)));
        urlParameters.add(new BasicNameValuePair("withTimeStampValidation", "true"));
        ResponseDto responseDto = HttpConn.getInstance().doPostForm(
                OperationType.VALIDATE_SIGNED_DOCUMENT.getUrl(Constants.ID_PROVIDER_ENTITY_ID), urlParameters);
        log.info("response: " + responseDto.getStatusCode() + " - " + responseDto.getMessage());
    }

/*
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