package org.votingsystem.test.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyRequestBatch;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.Wallet;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CurrencyRequest {

    public static void main(String[] args) throws Exception {
        Logger log = TestUtils.init(CurrencyRequest.class);
        SignatureService signatureService = SignatureService.getUserVSSignatureService("07553172H", UserVS.Type.USER);
        UserVS fromUserVS = signatureService.getUserVS();
        CurrencyServer currencyServer = TestUtils.fetchCurrencyServer(ContextVS.getInstance().getProperty("currencyServerURL"));
        ContextVS.getInstance().setDefaultServer(currencyServer);
        BigDecimal totalAmount = new BigDecimal(10);
        String curencyCode = "EUR";
        TagVS tag = new TagVS("HYDROGEN");
        Boolean isTimeLimited = true;
        CurrencyRequestBatch currencyBatch = new CurrencyRequestBatch(totalAmount, totalAmount, curencyCode, tag,
                isTimeLimited, ContextVS.getInstance().getCurrencyServer());
        String messageSubject = "TEST_CURRENCY_REQUEST_DATA_MSG_SUBJECT";
        Map<String, Object> mapToSend = new HashMap<String, Object>();
        byte[] requestBytes = new ObjectMapper().writeValueAsString(currencyBatch.getCurrencyCSRList()).getBytes();
        mapToSend.put(ContextVS.CSR_FILE_NAME, requestBytes);
        String signatureContent =  new ObjectMapper().writeValueAsString(currencyBatch.getRequestDataToSignMap());
        SMIMEMessage smimeMessage = signatureService.getSMIMETimeStamped(fromUserVS.getNif(),
                currencyServer.getName(),signatureContent, messageSubject);
        mapToSend.put(ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME, smimeMessage.getBytes());
        ResponseVS responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend, currencyServer.getCurrencyRequestServiceURL());
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            Map responseMap = new ObjectMapper().readValue(responseVS.getMessageBytes(),
                    new TypeReference<HashMap<String, Object>>() {});
            currencyBatch.initCurrency((String) responseMap.get("issuedCurrency"));
            Wallet.saveCurrencyToDir(currencyBatch.getCurrencyMap().values(), ContextVS.getInstance().getProperty("walletDir"));
        } else {
            log.log(Level.SEVERE," --- ERROR --- " + responseVS.getMessage());
        }
        System.exit(0);
    }

}

