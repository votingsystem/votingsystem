package org.votingsystem.test.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.CurrencyCode;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.currency.Wallet;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CurrencyRequest {

    private static Logger log =  Logger.getLogger(CurrencyRequest.class.getName());

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        String NIF = "07553172H";
        Wallet wallet = new Wallet(NIF.toCharArray());
        SignatureService signatureService = SignatureService.getUserSignatureService(
                "Currency_" + NIF, User.Type.USER);
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer();
        BigDecimal totalAmount = new BigDecimal(10);
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setAmount(totalAmount);
        transactionDto.setCurrencyCode(CurrencyCode.EUR);
        transactionDto.setTags(Sets.newHashSet("WEALTH"));
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
            wallet.addToWallet(Sets.newHashSet(requestDto.getCurrencyMap().values()));
        } else {
            log.log(Level.SEVERE," --- ERROR --- " + responseVS.getMessage());
        }
        System.exit(0);
    }

}

