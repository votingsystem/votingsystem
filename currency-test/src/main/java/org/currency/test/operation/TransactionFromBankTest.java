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
        transaction.setUUID(UUID.randomUUID().toString());
        transaction.setSubject("Test transaction").setAmount(new BigDecimal(50)).setCurrencyCode(CurrencyCode.JPY);
        transaction.setToUserIBAN("ES3578788989450000000006");
        //MockDNIe mockDNIe = new MockDNIe("ExternalBank");
        MockDNIe mockDNIe = new MockDNIe("MiscBank");
        byte[] signedBytes =  new XAdESSignature().sign(new XML().getMapper().writeValueAsBytes(transaction),
                mockDNIe.getJksSignatureToken(), new TSPHttpSource(Constants.TIMESTAMP_SERVICE_URL));
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(signedBytes, MediaType.XML,
                CurrencyOperation.TRANSACTION_FROM_BANK.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID));
        log.info("statusCode: " + responseDto.getStatusCode() + " - Message: " + responseDto.getMessage());
    }

    public void validateSignedDocument(byte[] signedDocumentBytes) {
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("signedXML", new String(signedDocumentBytes)));
        urlParameters.add(new BasicNameValuePair("withTimeStampValidation", "true"));
        ResponseDto responseDto = HttpConn.getInstance().doPostForm(
                OperationType.VALIDATE_SIGNED_DOCUMENT.getUrl(Constants.ID_PROVIDER_ENTITY_ID), urlParameters);
        log.info("response: " + responseDto.getStatusCode() + " - " + responseDto.getMessage());
    }

}