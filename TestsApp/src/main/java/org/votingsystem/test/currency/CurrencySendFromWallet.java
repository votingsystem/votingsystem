package org.votingsystem.test.currency;


import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.util.currency.Payment;

import java.io.File;
import java.io.FilenameFilter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CurrencySendFromWallet {

    private static Logger log =  Logger.getLogger(CurrencySendFromWallet.class.getName());

    public static void main(String[] args) throws Exception {
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        File walletDir = new File(ContextVS.getInstance().getProperty("walletDir"));
        log.info("walletDir: " + walletDir.getAbsolutePath());
        File[] currencyFiles = walletDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String fileName) {
                return !fileName.startsWith("EXPENDED_"); }
        });
        if(currencyFiles == null || currencyFiles.length == 0) throw new ExceptionVS(" --- Empty wallet ---");
        //we have al the currencies with its anonymous signed cert, now we can make de transactions
        CurrencyDto currencyDto = JSON.getMapper().readValue(currencyFiles[0], CurrencyDto.class);
        CurrencyBatch transactionBatch = new CurrencyBatch();
        transactionBatch.addCurrency(currencyDto.deSerialize());
        CurrencyBatchDto dto =  transactionBatch.getTransactionVSRequest(TypeVS.CURRENCY_SEND,
                Payment.CURRENCY_BATCH, "First Currency Transaction",
                "ES0878788989450000000007", new BigDecimal(9), "EUR", "WILDTAG", false, currencyServer.getTimeStampServiceURL());
        byte[] dtoBytes = JSON.getMapper().writeValueAsBytes(dto);
        dto = JSON.getMapper().readValue(dtoBytes, CurrencyBatchDto.class);


        ResponseVS responseVS = HttpHelper.getInstance().sendData(JSON.getMapper().writeValueAsBytes(dto),
                ContentTypeVS.JSON, currencyServer.getCurrencyTransactionServiceURL());
        log.info("Currency Transaction result: " + responseVS.getStatusCode());
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        Map<String, String> responseMap = JSON.getMapper().readValue(
                responseVS.getMessage(), new TypeReference<HashMap<String, Object>>() {});
        log.info("Transaction result:" + responseMap);
        transactionBatch.validateTransactionVSResponse(responseMap, currencyServer.getTrustAnchors());
        System.exit(0);
    }

}