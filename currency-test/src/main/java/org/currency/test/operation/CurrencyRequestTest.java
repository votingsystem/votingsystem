package org.currency.test.operation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import org.currency.test.Constants;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.TSPHttpSource;
import org.votingsystem.currency.Wallet;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.xades.XAdESSignature;
import org.votingsystem.xml.XML;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CurrencyRequestTest extends BaseTest {

    private static Logger log =  Logger.getLogger(CurrencyRequestTest.class.getName());

    public CurrencyRequestTest() {}


    public static void main(String[] args) throws Exception {
        new CurrencyRequestTest().test();
        System.exit(0);
    }

    public void test() throws Exception {
        MockDNIe mockDNIe = new MockDNIe("08888888D");
        Wallet wallet = new Wallet("", mockDNIe.getNif().toCharArray());
        BigDecimal totalAmount = new BigDecimal(12);
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setAmount(totalAmount);
        transactionDto.setCurrencyCode(CurrencyCode.EUR);

        CurrencyRequestDto requestDto = CurrencyRequestDto.CREATE_REQUEST(transactionDto, totalAmount,
                Constants.CURRENCY_SERVICE_ENTITY_ID);
        Map<String, byte[]> mapToSend = new HashMap<>();
        byte[] requestBytes = new XML().getMapper().writeValueAsBytes(requestDto.getRequestCSRSet());
        mapToSend.put(org.votingsystem.util.Constants.CSR_CURRENCY_FILE_NAME, requestBytes);

        byte[] signedBytes =  new XAdESSignature().sign(new XML().getMapper().writeValueAsBytes(requestDto),
                mockDNIe.getJksSignatureToken(), new TSPHttpSource(Constants.TIMESTAMP_SERVICE_URL));

        mapToSend.put(org.votingsystem.util.Constants.CURRENCY_REQUEST_FILE_NAME, signedBytes);
        ResponseDto response = HttpConn.getInstance().doPostMultipartRequest(mapToSend,
                CurrencyOperation.CURRENCY_REQUEST.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID));
        if(ResponseDto.SC_OK == response.getStatusCode()) {
            ResultListDto<String> currencyCertsDto = (ResultListDto<String>) response.getMessage(
                    new TypeReference<ResultListDto<String>>(){});
            requestDto.loadCurrencyCerts(currencyCertsDto.getResultList());
            wallet.addToWallet(Sets.newHashSet(requestDto.getCurrencyMap().values()));
        } else {
            response = response.getErrorResponse();
            log.log(Level.SEVERE," --- ERROR --- " + response.getMessage());
        }
    }

}