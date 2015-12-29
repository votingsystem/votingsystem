package org.votingsystem.client.webextension.task;

import com.fasterxml.jackson.core.type.TypeReference;
import javafx.concurrent.Task;
import org.votingsystem.client.webextension.OperationVS;
import org.votingsystem.client.webextension.service.BrowserSessionService;
import org.votingsystem.client.webextension.service.InboxService;
import org.votingsystem.client.webextension.util.InboxMessage;
import org.votingsystem.client.webextension.util.MsgUtils;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.currency.MapUtils;
import org.votingsystem.util.currency.Wallet;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CurrencyRequestTask extends Task<ResponseVS> {

    private static Logger log = Logger.getLogger(CurrencyRequestTask.class.getSimpleName());

    private char[] password;
    private OperationVS operationVS;

    public CurrencyRequestTask(OperationVS operationVS, char[] password) throws Exception {
        this.operationVS = operationVS;
        this.password = password;
    }

    @Override protected ResponseVS call() throws Exception {
        log.info("sendCurrencyRequest");
        TransactionVSDto transactionVSDto = operationVS.getDocumentToSign(TransactionVSDto.class);
        CurrencyRequestDto requestDto = CurrencyRequestDto.CREATE_REQUEST(transactionVSDto,
                transactionVSDto.getAmount(), operationVS.getTargetServer().getServerURL());
        Map<String, Object> mapToSend = new HashMap<>();
        byte[] requestBytes = JSON.getMapper().writeValueAsBytes(requestDto.getRequestCSRSet());
        mapToSend.put(ContextVS.CSR_FILE_NAME, requestBytes);
        String textToSign =  JSON.getMapper().writeValueAsString(requestDto);
        SMIMEMessage smimeMessage = BrowserSessionService.getSMIME(null, operationVS.getReceiverName(), textToSign,
                password, operationVS.getSignedMessageSubject());
        updateMessage(operationVS.getSignedMessageSubject());
        mapToSend.put(ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME, smimeMessage.getBytes());
        ResponseVS responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                ((CurrencyServer)operationVS.getTargetServer()).getCurrencyRequestServiceURL());
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            ResultListDto<String> resultListDto = (ResultListDto<String>) responseVS.getMessage(
                    new TypeReference<ResultListDto<String>>(){});
            requestDto.loadCurrencyCerts(resultListDto.getResultList());
            Wallet.saveToPlainWallet(requestDto.getCurrencyMap().values());
            responseVS = new ResponseVS(responseVS.getStatusCode(), resultListDto.getMessage());
            InboxMessage inboxMessage = new InboxMessage(ContextVS.getMessage("systemLbl"), new Date());
            inboxMessage.setMessage(MsgUtils.getPlainWalletNotEmptyMsg(MapUtils.getCurrencyMap(
                    requestDto.getCurrencyMap().values()))).setTypeVS(TypeVS.CURRENCY_IMPORT);
            InboxService.getInstance().newMessage(inboxMessage, false);
        }
        operationVS.processResult(responseVS);
        return responseVS;
    }
}
