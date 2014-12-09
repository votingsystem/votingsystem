package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.callable.MessageTimeStamper;
import org.votingsystem.android.callable.SignedMapSender;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.Utils;
import org.votingsystem.android.util.WalletUtils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Cooin;
import org.votingsystem.model.CooinAccountsInfo;
import org.votingsystem.model.CooinBatch;
import org.votingsystem.model.CooinServer;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TimestampException;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinService extends IntentService {

    public static final String TAG = CooinService.class.getSimpleName();

    public CooinService() { super(TAG); }

    private AppContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {
        contextVS = (AppContextVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        Uri uriData =  arguments.getParcelable(ContextVS.URI_KEY);;
        BigDecimal amount = (BigDecimal) arguments.getSerializable(ContextVS.VALUE_KEY);
        String currencyCode = (String) arguments.getSerializable(ContextVS.CURRENCY_KEY);
        String tagVS = (String) arguments.getSerializable(ContextVS.TAG_KEY);
        String password = arguments.getString(ContextVS.PIN_KEY);
        byte[] serializedCooin = arguments.getByteArray(ContextVS.COOIN_KEY);
        tagVS = tagVS == null? TagVS.WILDTAG:tagVS;
        TransactionVS transactionVS = (TransactionVS) intent.getSerializableExtra(ContextVS.TRANSACTION_KEY);
        try {
            switch(operation) {
                case COOIN_ACCOUNTS_INFO:
                    updateUserInfo(serviceCaller);
                    break;
                case COOIN_REQUEST:
                    cooinRequest(serviceCaller, new CooinBatch(transactionVS.getAmount(),
                            transactionVS.getAmount(), transactionVS.getCurrencyCode(), tagVS,
                            transactionVS.isTimeLimited(), contextVS.getCooinServer()), password);
                    break;
                case COOIN_SEND:
                    CooinBatch cooinBatch = new CooinBatch(transactionVS.getAmount(),
                            transactionVS.getAmount(), transactionVS.getCurrencyCode(), tagVS,
                            transactionVS.isTimeLimited(), contextVS.getCooinServer());
                    requestAndSendCooin(serviceCaller, cooinBatch, password);
                    break;
                case COOIN_CANCEL:
                    Cooin cooin = (Cooin) ObjectUtils.deSerializeObject(serializedCooin);
                    ResponseVS responseVS = cancelCooin(cooin);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        cooin.setCancellationReceipt(responseVS.getSMIME());
                        cooin.setState(Cooin.State.CANCELLED);
                        responseVS.setCaption(getString(R.string.cooin_cancellation_msg_subject));
                        responseVS.setNotificationMessage(getString(R.string.cooin_cancellation_msg));
                        responseVS.setIconId(R.drawable.accept_22);
                    } else {
                        responseVS.setCaption(getString(R.string.cooin_cancellation_error_msg_subject));
                        responseVS.setNotificationMessage(getString(R.string.cooin_cancellation_error_msg_subject));
                        responseVS.setIconId(R.drawable.cancel_22);
                        if(responseVS.getContentType() == ContentTypeVS.JSON_SIGNED) {
                            SMIMEMessage signedMessage = responseVS.getSMIME();
                            LOGD(TAG + ".cancelCooin", "error JSON response: " + signedMessage.getSignedContent());
                            JSONObject jsonResponse = new JSONObject(signedMessage.getSignedContent());
                            operation = TypeVS.valueOf(jsonResponse.getString("operation"));
                            if(TypeVS.COOIN_CANCEL == operation) {
                                cooin.setCancellationReceipt(responseVS.getSMIME());
                                cooin.setState(Cooin.State.LAPSED);
                                responseVS.setCaption(getString(R.string.cooin_cancellation_msg_subject));
                                responseVS.setNotificationMessage(getString(R.string.cooin_cancellation_msg));
                                responseVS.setIconId(R.drawable.accept_22);
                            }
                        }
                    }
                    broadCastResponse(Utils.getBroadcastResponse(operation, serviceCaller,
                            responseVS, this));
                    break;
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            broadCastResponse(Utils.getBroadcastResponse(operation, serviceCaller,
                    ResponseVS.getExceptionResponse(ex, this), this));
        }
    }

    private ResponseVS cancelCooin(Cooin cooin) {
        CooinServer cooinServer = contextVS.getCooinServer();
        ResponseVS responseVS = contextVS.signMessage(cooinServer.getNameNormalized(),
                cooin.getCancellationRequest().toString(),
                getString(R.string.cooin_cancellation_msg_subject));
        try {
            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                    ContentTypeVS.JSON_SIGNED, cooinServer.getTransactionVSServiceURL());
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, contextVS);
        } finally {
            return responseVS;
        }
    }

    private ResponseVS cancelCooins(Collection<Cooin> sendedCooins) {
        CooinServer cooinServer = contextVS.getCooinServer();
        ResponseVS responseVS = null;
        try {
            List<String> cancellationList = new ArrayList<String>();
            for(Cooin cooin : sendedCooins) {
                responseVS = contextVS.signMessage(cooinServer.getNameNormalized(),
                        cooin.getCancellationRequest().toString(),
                        getString(R.string.cooin_cancellation_msg_subject), contextVS.getTimeStampServiceURL());
                cancellationList.add(new String(Base64.encode(responseVS.getSMIME().getBytes())));
            }
            Map requestMap = new HashMap();
            requestMap.put("cooinCancellationList", cancellationList);
            responseVS = HttpHelper.sendData(new JSONObject(requestMap).toString().getBytes(),
                    ContentTypeVS.JSON, cooinServer.getCooinBatchCancellationServiceURL());
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            return responseVS;
        }
    }

    private void requestAndSendCooin(String serviceCaller, CooinBatch cooinBatch,
            String password) throws Exception {
        ResponseVS responseVS = null;
        try {
            responseVS = cooinRequest(serviceCaller, cooinBatch, password);
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            broadCastResponse(responseVS);
        }
    }

    private void sendCooinFromWallet(BigDecimal requestAmount, String currencyCode,
              String subject, String toUserName, String toUserIBAN, String tagVS) {
        ResponseVS responseVS = null;
        String message = null;
        String caption = null;
        byte[] decryptedMessageBytes = null;
        Map <Long,Cooin> sendedCooinsMap = new HashMap<Long, Cooin>();
        try {
            CooinAccountsInfo userInfo = PrefUtils.getCooinAccountsInfo(contextVS);
            BigDecimal available = userInfo.getAvailableForTagVS(currencyCode, tagVS);
            if(available.compareTo(requestAmount) < 0) {
                throw new Exception(getString(R.string.insufficient_cash_msg, currencyCode,
                        requestAmount.toString(), available.toString()));
            }
            CooinServer cooinServer = contextVS.getCooinServer();
            BigDecimal cooinAmount = new BigDecimal(10);
            int numCooins = requestAmount.divide(cooinAmount).intValue();
            List<Cooin> cooinsToSend = new ArrayList<Cooin>();
            for(int i = 0; i < numCooins; i++) {
                LOGD(TAG + ".sendCooinFromWallet", " === TODO FETCH COOIN FROM WALLET === ");
            }
            List<String> smimeCooinList = new ArrayList<String>();
            for(Cooin cooin : cooinsToSend) {
                String textToSign = cooin.getTransaction(toUserName, toUserIBAN, tagVS, null).toString();
                SMIMEMessage smimeMessage = cooin.getCertificationRequest().getSMIME(
                        cooin.getHashCertVS(), StringUtils.getNormalized(toUserName),
                        textToSign, subject, null);
                smimeMessage.getSigner().getCertificate().getSerialNumber();
                MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, contextVS);
                responseVS = timeStamper.call();
                if(ResponseVS.SC_OK != responseVS.getStatusCode())
                    throw new TimestampException(responseVS.getMessage());
                smimeCooinList.add(new String(Base64.encode(smimeMessage.getBytes())));
                sendedCooinsMap.put(cooin.getCertificationRequest().getCertificate().
                        getSerialNumber().longValue(), cooin);
            }
            KeyPair keyPair = sendedCooinsMap.values().iterator().next().
                    getCertificationRequest().getKeyPair();
            String publicKeyStr = new String(Base64.encode(keyPair.getPublic().getEncoded()));
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                Map mapToSend = new HashMap();
                mapToSend.put("amount", requestAmount.toString());
                mapToSend.put("currency", currencyCode);
                mapToSend.put("operation", TypeVS.COOIN_SEND.toString());
                mapToSend.put("cooins", smimeCooinList);
                mapToSend.put("publicKey", publicKeyStr);
                String textToSign = new JSONObject(mapToSend).toString();
                responseVS = HttpHelper.sendData(textToSign.getBytes(), ContentTypeVS.JSON,
                        cooinServer.getCooinTransactionServiceURL());
                if(responseVS.getContentType()!= null && responseVS.getContentType().isEncrypted()) {
                    decryptedMessageBytes = Encryptor.decryptCMS(keyPair.getPrivate(),
                            responseVS.getMessageBytes());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        JSONObject jsonResponse = new JSONObject(new String(decryptedMessageBytes));
                        JSONArray jsonArray = jsonResponse.getJSONArray("cooins");
                        Date dateCreated = null;
                        for(int i = 0; i < jsonArray.length(); i ++) {
                            SMIMEMessage smimeMessage = new SMIMEMessage(
                                    new ByteArrayInputStream(Base64.decode(jsonArray.getString(i))));
                            dateCreated = smimeMessage.getSigner().getTimeStampToken().
                                    getTimeStampInfo().getGenTime();
                            Cooin sendedCooin = sendedCooinsMap.get(smimeMessage.getSigner().getCertificate().
                                    getSerialNumber().longValue());
                            sendedCooin.setReceiptBytes(smimeMessage.getBytes());
                            sendedCooin.setState(Cooin.State.EXPENDED);

                        }
                        List<Cooin> cooinList = new ArrayList<Cooin>();
                        cooinList.addAll(sendedCooinsMap.values());
                        TransactionVS cooinSendTransaction = new TransactionVS(TransactionVS.Type.
                                COOIN_SEND, dateCreated, cooinList, requestAmount, currencyCode);
                        TransactionVSContentProvider.addTransaction(contextVS,
                                cooinSendTransaction, contextVS.getCurrentWeekLapseId());
                    }
                }
            }
        } catch(TimestampException ex) {
            responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP).setCaption(
                    getString(R.string.timestamp_service_error_caption)).setNotificationMessage(
                    ex.getMessage());
        } catch(Exception ex) {
            ex.printStackTrace();
            cancelCooins(sendedCooinsMap.values());
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            if(ResponseVS.SC_OK != responseVS.getStatusCode() &&
                    ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                LOGD(TAG + ".cancelCooinRepeated", "cancelCooinRepeated");
                try {
                    JSONObject responseJSON = new JSONObject(new String(decryptedMessageBytes, ContextVS.UTF_8));
                    String base64EncodedCooinRepeated = responseJSON.getString("messageSMIME");
                    SMIMEMessage smimeMessage = new SMIMEMessage(
                            new ByteArrayInputStream(Base64.decode(base64EncodedCooinRepeated)));
                    Long repeatedCertSerialNumber = smimeMessage.getSigner().getCertificate().
                            getSerialNumber().longValue();
                    Cooin repeatedCooin = sendedCooinsMap.get(repeatedCertSerialNumber);
                    repeatedCooin.setState(Cooin.State.EXPENDED);
                } catch(Exception ex) {
                    ex.printStackTrace();
                    responseVS = ResponseVS.getExceptionResponse(getString(R.string.exception_lbl),
                            ex.getMessage());
                }
                caption = getString(R.string.cooin_send_error_caption);
                message = getString(R.string.cooin_repeated_error_msg);
            } else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                caption = getString(R.string.cooin_send_error_caption);
                message = getString(R.string.cooin_expended_send_error_msg);
            } else {
                for(Cooin cooin:sendedCooinsMap.values()) {
                    LOGD(TAG + ".sendCooinFromWallet", " ==== TODO - UPDATE COOIN STATE");
                }
                caption = getString(R.string.cooin_send_ok_caption);
                message = getString(R.string.cooin_send_ok_msg, requestAmount.toString(),
                        currencyCode, subject, toUserName);
            }
            responseVS.setCaption(caption).setNotificationMessage(message);
            broadCastResponse(responseVS);
        }
    }

    private ResponseVS cooinRequest(String serviceCaller, CooinBatch cooinBatch, String password){
        CooinServer cooinServer = cooinBatch.getCooinServer();
        ResponseVS responseVS = null;
        try {
            LOGD(TAG + ".cooinRequest", "Amount: " + cooinBatch.getTotalAmount().toPlainString());
            String messageSubject = getString(R.string.cooin_request_msg_subject);
            String fromUser = contextVS.getUserVS().getNif();
            String requestDataFileName = ContextVS.COOIN_REQUEST_DATA_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED.getName();
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            JSONArray cooinCSRList = new JSONArray(cooinBatch.getCooinCSRList());
            mapToSend.put(ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.JSON.getName(),
                    cooinCSRList.toString().getBytes());
            SignedMapSender signedMapSender = new SignedMapSender(fromUser,
                    cooinServer.getNameNormalized(), cooinBatch.getRequestDataToSignJSON().toString(),
                    mapToSend, messageSubject, null, cooinServer.getCooinRequestServiceURL(),
                    requestDataFileName, (AppContextVS)getApplicationContext());
            responseVS = signedMapSender.call();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONObject responseJSON = new JSONObject (new String(responseVS.getMessageBytes(), "UTF-8"));
                cooinBatch.initCooins(responseJSON.getJSONArray("issuedCooins"));
                responseVS.setCaption(getString(R.string.cooin_request_ok_caption)).setNotificationMessage(
                        getString(R.string.cooin_request_ok_msg, cooinBatch.getTotalAmount(),
                        cooinBatch.getCurrencyCode()));
                WalletUtils.saveCooinList(cooinBatch.getCooinsMap().values(), password, contextVS);
                updateUserInfo(serviceCaller);
            } else responseVS.setCaption(getString(
                    R.string.cooin_request_error_caption));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this).setIconId(R.drawable.cancel_22);
        } finally {
            contextVS.broadcastResponse(
                    responseVS.setTypeVS(TypeVS.COOIN_REQUEST).setServiceCaller(serviceCaller));
            return responseVS;
        }
    }

    private void updateUserInfo(String serviceCaller) {
        LOGD(TAG + ".updateUserInfo", "updateUserInfo");
        ResponseVS responseVS = null;
        try {
            String targetService = contextVS.getCooinServer().getUserInfoServiceURL(
                    contextVS.getUserVS().getNif());
            responseVS = HttpHelper.getData(targetService, ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                CooinAccountsInfo accountsInfo = CooinAccountsInfo.parse(
                        responseVS.getMessageJSON());
                PrefUtils.putCooinAccountsInfo(accountsInfo, DateUtils.getCurrentWeekPeriod(),
                        contextVS);
                TransactionVSContentProvider.updateUserVSTransactionVSList(contextVS, accountsInfo);
            } else responseVS.setCaption(getString(R.string.error_lbl));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                responseVS.setNotificationMessage(getString(R.string.user_info_updated));
            responseVS.setServiceCaller(serviceCaller).setTypeVS(TypeVS.COOIN_ACCOUNTS_INFO);
            broadCastResponse(responseVS);
        }
    }

    private void broadCastResponse(ResponseVS responseVS) {
        contextVS.showNotification(responseVS);
        contextVS.broadcastResponse(responseVS);
    }

}