package org.currency.test.operation;


import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.currency.Wallet;

import java.util.logging.Logger;

public class WalletTest {

    private static Logger log =  Logger.getLogger(WalletTest.class.getName());

    public WalletTest() {}


    public static void main(String[] args) throws Exception {
        new WalletTest().test();
        System.exit(0);
    }


    public void test() throws Exception {
        MockDNIe mockDNIe = new MockDNIe("08888888D");
        Wallet wallet = new Wallet("", mockDNIe.getNif().toCharArray());
        /*
        log.info("walletDir: " + walletDir.getAbsolutePath());
        File[] currencyFiles = walletDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String fileName) {
                return !(fileName.startsWith("EXPENDED_") || fileName.startsWith("leftOver_")); }
        });

        if(currencyFiles == null || currencyFiles.length == 0) throw new ExceptionVS(" --- Empty wallet ---");
        //we have al the currencies with its anonymous signed cert, now we can make de transactions
        File currencyFile = currencyFiles[0];
        CurrencyDto currencyDto = new JSON().getMapper().readValue(currencyFile, CurrencyDto.class);
        CurrencyBatchDto currencyBatchDto =  CurrencyBatchDto.NEW("First Currency Transaction",
                "ES4678788989450000000002", new BigDecimal(9), CurrencyCode.EUR, "HIDROGENO", true,
                Arrays.asList(currencyDto.deSerialize()), currencyServer.getServerURL());

        ResponseVS responseVS = HttpHelper.getInstance().sendData(new JSON().getMapper().writeValueAsBytes(currencyBatchDto),
                ContentType.JSON, currencyServer.getCurrencyTransactionServiceURL());
        log.info("Currency Transaction result: " + responseVS.getStatusCode());
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
        CurrencyBatchResponseDto responseDto = new JSON().getMapper().readValue(responseVS.getMessage(),
                CurrencyBatchResponseDto.class);
        currencyBatchDto.validateResponse(responseDto, currencyServer.getTrustAnchors());
        currencyFile.renameTo(new File(currencyFile.getParent() + File.separator + "EXPENDED_" + currencyFile.getName()));

        String walletPath = ContextVS.getInstance().getProperty("walletDir");
        currencyDto = CurrencyDto.serialize(currencyBatchDto.getLeftOverCurrency());
        new File(walletPath).mkdirs();
        currencyFile = new File(walletPath + "leftOver_" + StringUtils.toHex(currencyDto.getHashCertVS())  +
                ContextVS.SERIALIZED_OBJECT_EXTENSION);
        new JSON().getMapper().writeValue(currencyFile, currencyDto);*/
        System.exit(0);
    }

}