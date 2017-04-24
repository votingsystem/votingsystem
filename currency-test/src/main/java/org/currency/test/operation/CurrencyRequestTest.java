package org.currency.test.operation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.crypto.TSPHttpSource;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.currency.Wallet;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.model.User;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.testlib.pkcs7.CMSSignatureBuilder;
import org.votingsystem.util.Constants;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;
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
        new CurrencyRequestTest().sendRequest();
        System.exit(0);
    }

    public void sendRequest() throws Exception {
        Wallet wallet = new Wallet(".", "local-demo".toCharArray());
        log.info("WalletDirPath: " + wallet.getWalletDirPath());
        MockDNIe mockDNIe = new MockDNIe("08888888D");

        BigDecimal totalAmount = new BigDecimal(12);
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setAmount(totalAmount);
        transactionDto.setCurrencyCode(CurrencyCode.JPY);
        CurrencyRequestDto requestDto = CurrencyRequestDto.CREATE_REQUEST(transactionDto, totalAmount,
                org.currency.test.Constants.CURRENCY_SERVICE_ENTITY_ID);
        Map<String, byte[]> fileMap = new HashMap<>();
        //byte[] requestBytes = JSON.getMapper().writeValueAsBytes(requestDto.getRequestCSRSet());
        byte[] csrRequestBytes = XML.getMapper().writeValueAsBytes(requestDto.getRequestCSRSet());
        fileMap.put(Constants.CSR_CURRENCY_FILE_NAME, csrRequestBytes);
        byte[] contentToSign =  XML.getMapper().writeValueAsBytes(requestDto);
        byte[] signedDocumentBytes = XAdESSignature.sign(contentToSign, mockDNIe.getJksSignatureToken(),
                new TSPHttpSource(org.currency.test.Constants.TIMESTAMP_SERVICE_URL));
        fileMap.put(Constants.CURRENCY_REQUEST_FILE_NAME, signedDocumentBytes);
        //CMSSignatureBuilder signatureService = new CMSSignatureBuilder(mockDNIe);
        //byte[] contentToSign =  JSON.getMapper().writeValueAsBytes(requestDto);
        //CMSSignedMessage cmsMessage = signatureService.signDataWithTimeStamp(contentToSign,
        //        org.currency.test.Constants.TIMESTAMP_SERVICE_URL);
        //fileMap.put(Constants.CURRENCY_REQUEST_FILE_NAME, cmsMessage.toPEM());
        fileMap.put(Constants.CURRENCY_REQUEST_FILE_NAME, signedDocumentBytes);
        String serviceURL = CurrencyOperation.CURRENCY_REQUEST.getUrl(org.currency.test.Constants.CURRENCY_SERVICE_ENTITY_ID);
        ResponseDto responseDto = HttpConn.getInstance().doPostMultipartRequest(fileMap, serviceURL);

        if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
            ResultListDto<String> currencyCertsDto = (ResultListDto<String>) responseDto.getMessage(
                    new TypeReference<ResultListDto<String>>(){});
            requestDto.loadCurrencyCerts(currencyCertsDto.getResultList());
            wallet.addToWallet(Sets.newHashSet(requestDto.getCurrencyMap().values()));
        } else {
            log.log(Level.SEVERE," --- ERROR --- " + responseDto.getMessage());
        }
        System.exit(0);
    }

}

