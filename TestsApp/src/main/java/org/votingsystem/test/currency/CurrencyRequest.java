package org.votingsystem.test.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CurrencyRequest {

    private static Logger log =  Logger.getLogger(CurrencyRequest.class.getName());

    public static void main(String[] args) throws Exception {
        ContextVS.getInstance().initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SignatureService signatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER);
        UserVS fromUserVS = signatureService.getUserVS();
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        BigDecimal totalAmount = new BigDecimal(10);
        String curencyCode = "EUR";
        TagVS tag = new TagVS("HIDROGENO");
        TransactionVSDto transactionVSDto = new TransactionVSDto();
        transactionVSDto.setAmount(totalAmount);
        transactionVSDto.setCurrencyCode(curencyCode);
        transactionVSDto.setTag(tag);
        transactionVSDto.setTimeLimited(true);
        CurrencyRequestDto requestDto = CurrencyRequestDto.CREATE_REQUEST(transactionVSDto, totalAmount,
                ContextVS.getInstance().getCurrencyServer().getServerURL());
        String messageSubject = "TEST_CURRENCY_REQUEST_DATA_MSG_SUBJECT";
        Map<String, Object> mapToSend = new HashMap<>();
        byte[] requestBytes = JSON.getMapper().writeValueAsBytes(requestDto.getRequestCSRSet());
        mapToSend.put(ContextVS.CSR_FILE_NAME, requestBytes);
        String textToSign =  JSON.getMapper().writeValueAsString(requestDto);
        SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(fromUserVS.getNif(),
                currencyServer.getName(), textToSign, messageSubject);
        mapToSend.put(ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME, smimeMessage.getBytes());
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
            new File(walletPath).mkdirs();
            File currencyFile = new File(walletPath + currencyDto.getHashCertVS() + ContextVS.SERIALIZED_OBJECT_EXTENSION);
            JSON.getMapper().writeValue(currencyFile, currencyDto);
            log.info("stored currency: " + currencyFile.getAbsolutePath());
        }
    }
}

