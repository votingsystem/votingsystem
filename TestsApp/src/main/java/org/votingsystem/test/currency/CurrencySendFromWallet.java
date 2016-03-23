package org.votingsystem.test.currency;


import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyBatchResponseDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.CurrencyCode;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.io.File;
import java.io.FilenameFilter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.logging.Logger;

public class CurrencySendFromWallet {

    private static Logger log =  Logger.getLogger(CurrencySendFromWallet.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer();
        File walletDir = new File(ContextVS.getInstance().getProperty("walletDir"));
        log.info("walletDir: " + walletDir.getAbsolutePath());
        File[] currencyFiles = walletDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String fileName) {
                return !(fileName.startsWith("EXPENDED_") || fileName.startsWith("leftOver_")); }
        });
        if(currencyFiles == null || currencyFiles.length == 0) throw new ExceptionVS(" --- Empty wallet ---");
        //we have al the currencies with its anonymous signed cert, now we can make de transactions
        File currencyFile = currencyFiles[0];
        CurrencyDto currencyDto = JSON.getMapper().readValue(currencyFile, CurrencyDto.class);
        CurrencyBatchDto currencyBatchDto =  CurrencyBatchDto.NEW("First Currency Transaction",
                "ES4678788989450000000002", new BigDecimal(9), CurrencyCode.EUR, "HIDROGENO", true,
                Arrays.asList(currencyDto.deSerialize()), currencyServer.getServerURL());

        ResponseVS responseVS = HttpHelper.getInstance().sendData(JSON.getMapper().writeValueAsBytes(currencyBatchDto),
                ContentType.JSON, currencyServer.getCurrencyTransactionServiceURL());
        log.info("Currency Transaction result: " + responseVS.getStatusCode());
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        CurrencyBatchResponseDto responseDto = JSON.getMapper().readValue(responseVS.getMessage(),
                CurrencyBatchResponseDto.class);
        currencyBatchDto.validateResponse(responseDto, currencyServer.getTrustAnchors());
        currencyFile.renameTo(new File(currencyFile.getParent() + File.separator + "EXPENDED_" + currencyFile.getName()));

        String walletPath = ContextVS.getInstance().getProperty("walletDir");
        currencyDto = CurrencyDto.serialize(currencyBatchDto.getLeftOverCurrency());
        new File(walletPath).mkdirs();
        currencyFile = new File(walletPath + "leftOver_" + StringUtils.toHex(currencyDto.getHashCertVS())  +
                ContextVS.SERIALIZED_OBJECT_EXTENSION);
        JSON.getMapper().writeValue(currencyFile, currencyDto);
        System.exit(0);
    }

}