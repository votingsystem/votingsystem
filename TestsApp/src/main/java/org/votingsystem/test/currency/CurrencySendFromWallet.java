package org.votingsystem.test.currency;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.CurrencyServer;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.CurrencyTransactionBatch;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.io.File;
import java.io.FilenameFilter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CurrencySendFromWallet {


    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(CurrencySendFromWallet.class);
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        File walletFir = new File(ContextVS.getInstance().getProperty("walletDir"));
        File[] currencyFiles = walletFir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String fileName) { return !fileName.startsWith("EXPENDED_"); }
        });
        if(currencyFiles != null || currencyFiles.length == 0) throw new ExceptionVS(" --- Empty wallet ---");
        //we have al the Currency initialized, now we can make de transactions
        CurrencyTransactionBatch transactionBatch = new CurrencyTransactionBatch();
        transactionBatch.addCurrency(currencyFiles[0]);
        Map requestJSON =  transactionBatch.getTransactionVSRequest(TypeVS.CURRENCY_SEND,
                Payment.ANONYMOUS_SIGNED_TRANSACTION, "First Currency Transaction",
                "ES0878788989450000000007", new BigDecimal(9), "EUR", "WILDTAG", false, currencyServer.getTimeStampServiceURL());
        ResponseVS responseVS = HttpHelper.getInstance().sendData(requestJSON.toString().getBytes(),
                ContentTypeVS.JSON, currencyServer.getCurrencyTransactionServiceURL());
        log.info("Currency Transaction result: " + responseVS.getStatusCode());
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        Map<String, Object> responseMap = new ObjectMapper().readValue(
                responseVS.getMessage(), new TypeReference<HashMap<String, Object>>() {});
        log.info("Transaction result:" + responseMap);
        transactionBatch.validateTransactionVSResponse(responseMap, currencyServer.getTrustAnchors());
        System.exit(0);

    }

}