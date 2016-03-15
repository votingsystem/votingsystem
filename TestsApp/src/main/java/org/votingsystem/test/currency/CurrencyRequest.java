package org.votingsystem.test.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.StringUtils;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CurrencyRequest {

    private static Logger log =  Logger.getLogger(CurrencyRequest.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SignatureService signatureService = SignatureService.getUserSignatureService(
                "Currency_07553172H", User.Type.USER);
        User fromUser = signatureService.getUser();
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        BigDecimal totalAmount = new BigDecimal(10);
        String curencyCode = "EUR";
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setAmount(totalAmount);
        transactionDto.setCurrencyCode(curencyCode);
        transactionDto.setTags(Sets.newHashSet("ENERGY"));
        transactionDto.setTimeLimited(true);
        CurrencyRequestDto requestDto = CurrencyRequestDto.CREATE_REQUEST(transactionDto, totalAmount,
                ContextVS.getInstance().getCurrencyServer().getServerURL());
        Map<String, Object> mapToSend = new HashMap<>();
        byte[] requestBytes = JSON.getMapper().writeValueAsBytes(requestDto.getRequestCSRSet());
        mapToSend.put(ContextVS.CSR_FILE_NAME, requestBytes);
        byte[] contentToSign =  JSON.getMapper().writeValueAsBytes(requestDto);
        CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(contentToSign);
        mapToSend.put(ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME, cmsMessage.toPEM());
        ResponseVS responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend, currencyServer.getCurrencyRequestServiceURL());
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            ResultListDto<String> currencyCertsDto = (ResultListDto<String>) responseVS.getMessage(
                    new TypeReference<ResultListDto<String>>(){});
            requestDto.loadCurrencyCerts(currencyCertsDto.getResultList());
            saveCurrencyToDir(requestDto.getCurrencyMap().values());
        } else {
            log.log(Level.SEVERE," --- ERROR --- " + responseVS.getMessage());
        }
        System.exit(0);
    }

    public static void saveCurrencyToDir(Collection<Currency> currencyCollection) throws Exception {
        String walletPath = ContextVS.getInstance().getProperty("walletDir");
        for(Currency currency : currencyCollection) {
            CurrencyDto currencyDto = CurrencyDto.serialize(currency);
            File currencyFile = new File(walletPath + StringUtils.toHex(currencyDto.getHashCertVS()) +
                    ContextVS.SERIALIZED_OBJECT_EXTENSION);
            currencyFile.getParentFile().mkdirs();
            currencyFile.createNewFile();
            JSON.getMapper().writeValue(currencyFile, currencyDto);
            log.info("stored currency: " + currencyFile.getAbsolutePath());
        }
    }
}

