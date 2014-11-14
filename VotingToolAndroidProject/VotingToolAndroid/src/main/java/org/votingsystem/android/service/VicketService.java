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
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.android.callable.SignedMapSender;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.Utils;
import org.votingsystem.android.util.WalletUtils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVSTransactionVSListInfo;
import org.votingsystem.model.Vicket;
import org.votingsystem.model.VicketBatch;
import org.votingsystem.model.VicketServer;
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
import java.util.Calendar;
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
public class VicketService extends IntentService {

    public static final String TAG = VicketService.class.getSimpleName();

    public VicketService() { super(TAG); }

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
        byte[] serializedVicket = arguments.getByteArray(ContextVS.VICKET_KEY);
        tagVS = tagVS == null? TagVS.WILDTAG:tagVS;
        TransactionVS transactionVS = (TransactionVS) intent.getSerializableExtra(ContextVS.TRANSACTION_KEY);
        try {
            switch(operation) {
                case VICKET_USER_INFO:
                    updateUserInfo();
                    break;
                case VICKET_REQUEST:
                    vicketRequest(serviceCaller, new VicketBatch(transactionVS.getAmount(),
                            transactionVS.getAmount(), transactionVS.getCurrencyCode(), tagVS,
                            transactionVS.isTimeLimited(), contextVS.getVicketServer()), password);
                    break;
                case VICKET_SEND:
                    VicketBatch vicketBatch = new VicketBatch(transactionVS.getAmount(),
                            transactionVS.getAmount(), transactionVS.getCurrencyCode(), tagVS,
                            transactionVS.isTimeLimited(), contextVS.getVicketServer());
                    requestAndSendVicket(serviceCaller, vicketBatch, password);
                    break;
                case VICKET_CANCEL:
                    Vicket vicket = (Vicket) ObjectUtils.deSerializeObject(serializedVicket);
                    ResponseVS responseVS = cancelVicket(vicket);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        vicket.setCancellationReceipt(responseVS.getSMIME());
                        vicket.setState(Vicket.State.CANCELLED);
                        responseVS.setCaption(getString(R.string.vicket_cancellation_msg_subject));
                        responseVS.setNotificationMessage(getString(R.string.vicket_cancellation_msg));
                        responseVS.setIconId(R.drawable.accept_22);
                    } else {
                        responseVS.setCaption(getString(R.string.vicket_cancellation_error_msg_subject));
                        responseVS.setNotificationMessage(getString(R.string.vicket_cancellation_error_msg_subject));
                        responseVS.setIconId(R.drawable.cancel_22);
                        if(responseVS.getContentType() == ContentTypeVS.JSON_SIGNED) {
                            SMIMEMessage signedMessage = responseVS.getSMIME();
                            LOGD(TAG + ".cancelVicket(...)", "error JSON response: " + signedMessage.getSignedContent());
                            JSONObject jsonResponse = new JSONObject(signedMessage.getSignedContent());
                            operation = TypeVS.valueOf(jsonResponse.getString("operation"));
                            if(TypeVS.VICKET_CANCEL == operation) {
                                vicket.setCancellationReceipt(responseVS.getSMIME());
                                vicket.setState(Vicket.State.LAPSED);
                                responseVS.setCaption(getString(R.string.vicket_cancellation_msg_subject));
                                responseVS.setNotificationMessage(getString(R.string.vicket_cancellation_msg));
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

    private ResponseVS cancelVicket(Vicket vicket) {
        VicketServer vicketServer = contextVS.getVicketServer();
        SMIMESignedSender signedSender = new SMIMESignedSender(contextVS.getUserVS().getNif(),
                vicketServer.getNameNormalized(), vicketServer.getVicketCancelServiceURL(),
                vicket.getCancellationRequest().toString(), ContentTypeVS.JSON_SIGNED,
                getString(R.string.vicket_cancellation_msg_subject), vicketServer.getCertificate(),
                (AppContextVS)getApplicationContext());
        return signedSender.call();
    }

    private ResponseVS cancelVickets(Collection<Vicket> sendedVickets) {
        VicketServer vicketServer = contextVS.getVicketServer();
        ResponseVS responseVS = null;
        try {
            List<String> cancellationList = new ArrayList<String>();
            for(Vicket vicket : sendedVickets) {
                responseVS = contextVS.signMessage(vicketServer.getNameNormalized(),
                        vicket.getCancellationRequest().toString(),
                        getString(R.string.vicket_cancellation_msg_subject), contextVS.getTimeStampServiceURL());
                cancellationList.add(new String(Base64.encode(responseVS.getSMIME().getBytes())));
            }
            Map requestMap = new HashMap();
            requestMap.put("vicketCancellationList", cancellationList);
            responseVS = HttpHelper.sendData(new JSONObject(requestMap).toString().getBytes(),
                    ContentTypeVS.JSON, vicketServer.getVicketBatchCancellationServiceURL());
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            return responseVS;
        }
    }

    private void requestAndSendVicket(String serviceCaller, VicketBatch vicketBatch,
            String password) throws Exception {
        ResponseVS responseVS = null;
        try {
            responseVS = vicketRequest(serviceCaller, vicketBatch, password);
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            broadCastResponse(responseVS);
        }
    }

    private void sendVicketFromWallet(BigDecimal requestAmount, String currencyCode,
              String subject, String toUserName, String toUserIBAN, String tagVS) {
        ResponseVS responseVS = null;
        String message = null;
        String caption = null;
        byte[] decryptedMessageBytes = null;
        Map <Long,Vicket> sendedVicketsMap = new HashMap<Long, Vicket>();
        try {
            UserVSTransactionVSListInfo userInfo = PrefUtils.getUserVSTransactionVSListInfo(contextVS);
            BigDecimal available = userInfo.getAvailableForTagVS(currencyCode, tagVS);
            if(available.compareTo(requestAmount) < 0) {
                throw new Exception(getString(R.string.insufficient_cash_msg, currencyCode,
                        requestAmount.toString(), available.toString()));
            }
            VicketServer vicketServer = contextVS.getVicketServer();
            BigDecimal vicketAmount = new BigDecimal(10);
            int numVickets = requestAmount.divide(vicketAmount).intValue();
            List<Vicket> vicketsToSend = new ArrayList<Vicket>();
            for(int i = 0; i < numVickets; i++) {
                LOGD(TAG + ".sendVicketFromWallet", " === TODO FETCH VICKET FROM WALLET === ");
            }
            List<String> smimeVicketList = new ArrayList<String>();
            for(Vicket vicket : vicketsToSend) {
                String textToSign = vicket.getTransaction(toUserName, toUserIBAN, tagVS, null).toString();
                SMIMEMessage smimeMessage = vicket.getCertificationRequest().getSMIME(
                        vicket.getHashCertVS(), StringUtils.getNormalized(toUserName),
                        textToSign, subject, null);
                smimeMessage.getSigner().getCertificate().getSerialNumber();
                MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, contextVS);
                responseVS = timeStamper.call();
                if(ResponseVS.SC_OK != responseVS.getStatusCode())
                    throw new TimestampException(responseVS.getMessage());
                smimeVicketList.add(new String(Base64.encode(smimeMessage.getBytes())));
                sendedVicketsMap.put(vicket.getCertificationRequest().getCertificate().
                        getSerialNumber().longValue(), vicket);
            }
            KeyPair keyPair = sendedVicketsMap.values().iterator().next().
                    getCertificationRequest().getKeyPair();
            String publicKeyStr = new String(Base64.encode(keyPair.getPublic().getEncoded()));
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                Map mapToSend = new HashMap();
                mapToSend.put("amount", requestAmount.toString());
                mapToSend.put("currency", currencyCode);
                mapToSend.put("operation", TypeVS.VICKET_SEND.toString());
                mapToSend.put("vickets", smimeVicketList);
                mapToSend.put("publicKey", publicKeyStr);
                String textToSign = new JSONObject(mapToSend).toString();
                responseVS = HttpHelper.sendData(textToSign.getBytes(), ContentTypeVS.JSON,
                        vicketServer.getVicketTransactionServiceURL());
                if(responseVS.getContentType()!= null && responseVS.getContentType().isEncrypted()) {
                    decryptedMessageBytes = Encryptor.decryptCMS(keyPair.getPrivate(),
                            responseVS.getMessageBytes());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        JSONObject jsonResponse = new JSONObject(new String(decryptedMessageBytes));
                        JSONArray jsonArray = jsonResponse.getJSONArray("vickets");
                        Date dateCreated = null;
                        for(int i = 0; i < jsonArray.length(); i ++) {
                            SMIMEMessage smimeMessage = new SMIMEMessage(
                                    new ByteArrayInputStream(Base64.decode(jsonArray.getString(i))));
                            dateCreated = smimeMessage.getSigner().getTimeStampToken().
                                    getTimeStampInfo().getGenTime();
                            Vicket sendedVicket = sendedVicketsMap.get(smimeMessage.getSigner().getCertificate().
                                    getSerialNumber().longValue());
                            sendedVicket.setReceiptBytes(smimeMessage.getBytes());
                            sendedVicket.setState(Vicket.State.EXPENDED);

                        }
                        List<Vicket> vicketList = new ArrayList<Vicket>();
                        vicketList.addAll(sendedVicketsMap.values());
                        TransactionVS vicketSendTransaction = new TransactionVS(TransactionVS.Type.
                                VICKET_SEND, dateCreated, vicketList, requestAmount, currencyCode);
                        TransactionVSContentProvider.addTransaction(contextVS,
                                vicketSendTransaction, contextVS.getCurrentWeekLapseId());
                    }
                }
            }
        } catch(TimestampException ex) {
            responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP).setCaption(
                    getString(R.string.timestamp_service_error_caption)).setNotificationMessage(
                    ex.getMessage());
        } catch(Exception ex) {
            ex.printStackTrace();
            cancelVickets(sendedVicketsMap.values());
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            if(ResponseVS.SC_OK != responseVS.getStatusCode() &&
                    ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                LOGD(TAG + ".cancelVicketRepeated(...)", "cancelVicketRepeated");
                try {
                    JSONObject responseJSON = new JSONObject(new String(decryptedMessageBytes, ContextVS.UTF_8));
                    String base64EncodedVicketRepeated = responseJSON.getString("messageSMIME");
                    SMIMEMessage smimeMessage = new SMIMEMessage(
                            new ByteArrayInputStream(Base64.decode(base64EncodedVicketRepeated)));
                    Long repeatedCertSerialNumber = smimeMessage.getSigner().getCertificate().
                            getSerialNumber().longValue();
                    Vicket repeatedVicket = sendedVicketsMap.get(repeatedCertSerialNumber);
                    repeatedVicket.setState(Vicket.State.EXPENDED);
                } catch(Exception ex) {
                    ex.printStackTrace();
                    responseVS = ResponseVS.getExceptionResponse(getString(R.string.exception_lbl),
                            ex.getMessage());
                }
                caption = getString(R.string.vicket_send_error_caption);
                message = getString(R.string.vicket_repeated_error_msg);
            } else if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                caption = getString(R.string.vicket_send_error_caption);
                message = getString(R.string.vicket_expended_send_error_msg);
            } else {
                for(Vicket vicket:sendedVicketsMap.values()) {
                    LOGD(TAG + ".sendVicketFromWallet(...)", " ==== TODO - UPDATE VICKET STATE");
                }
                caption = getString(R.string.vicket_send_ok_caption);
                message = getString(R.string.vicket_send_ok_msg, requestAmount.toString(),
                        currencyCode, subject, toUserName);
            }
            responseVS.setCaption(caption).setNotificationMessage(message);
            broadCastResponse(responseVS);
        }
    }

    private ResponseVS vicketRequest(String serviceCaller, VicketBatch vicketBatch,String password){
        VicketServer vicketServer = vicketBatch.getVicketServer();
        String caption = null;
        String message = null;
        int iconId = R.drawable.cancel_22;
        ResponseVS responseVS = null;
        try {
            String messageSubject = getString(R.string.vicket_request_msg_subject);
            String fromUser = contextVS.getUserVS().getNif();
            String requestDataFileName = ContextVS.VICKET_REQUEST_DATA_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED.getName();
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            JSONArray vicketCSRList = new JSONArray(vicketBatch.getVicketCSRList());
            mapToSend.put(ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.JSON.getName(),
                    vicketCSRList.toString().getBytes());
            SignedMapSender signedMapSender = new SignedMapSender(fromUser,
                    vicketServer.getNameNormalized(), vicketBatch.getRequestDataToSignJSON().toString(),
                    mapToSend, messageSubject, null, vicketServer.getVicketRequestServiceURL(),
                    requestDataFileName, (AppContextVS)getApplicationContext());
            responseVS = signedMapSender.call();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONObject responseJSON = new JSONObject (new String(responseVS.getMessageBytes(), "UTF-8"));
                vicketBatch.initVickets(responseJSON.getJSONArray("issuedVickets"));
                responseVS.setCaption(getString(R.string.vicket_request_ok_caption)).setNotificationMessage(
                        getString(R.string.vicket_request_ok_msg, vicketBatch.getTotalAmount(),
                        vicketBatch.getCurrencyCode()));
                WalletUtils.saveVicketList(vicketBatch.getVicketsMap().values(), password, this);
            } else responseVS.setCaption(getString(
                    R.string.vicket_request_error_caption));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this).setIconId(R.drawable.cancel_22);
        } finally {
            contextVS.broadcastResponse(
                    responseVS.setTypeVS(TypeVS.VICKET_REQUEST).setServiceCaller(serviceCaller));
            return responseVS;
        }
    }

    private void updateUserInfo() {
        LOGD(TAG + ".updateUserInfo(...)", "updateUserInfo");
        VicketServer vicketServer = contextVS.getVicketServer();
        String msgSubject = getString(R.string.vicket_user_info_request_msg_subject);
        ResponseVS responseVS = null;
        try {
            JSONObject userInfoRequestJSON = Vicket.getUserVSAccountInfoRequest(
                    contextVS.getUserVS().getNif());
            SMIMESignedSender smimeSignedSender = new SMIMESignedSender(contextVS.getUserVS().getNif(),
                    vicketServer.getNameNormalized(), vicketServer.getUserInfoServiceURL(),
                    userInfoRequestJSON.toString(), ContentTypeVS.JSON_SIGNED,
                    msgSubject, vicketServer.getCertificate(), contextVS);
            responseVS = smimeSignedSender.call();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                String responseStr = responseVS.getMessage();
                UserVSTransactionVSListInfo userInfo = UserVSTransactionVSListInfo.parse(
                        new JSONObject(responseStr));
                PrefUtils.putUserVSTransactionVSListInfo(contextVS, userInfo,
                        DateUtils.getWeekPeriod(Calendar.getInstance()));
                TransactionVSContentProvider.updateUserVSTransactionVSList(contextVS, userInfo);
            } else {
                responseVS.setCaption(getString(R.string.error_lbl));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            if(ResponseVS.SC_OK == responseVS.getStatusCode())
                responseVS.setNotificationMessage(getString(R.string.user_info_updated));
            broadCastResponse(responseVS);
        }
    }

    private void broadCastResponse(ResponseVS responseVS) {
        contextVS.showNotification(responseVS);
        contextVS.broadcastResponse(responseVS);
    }

}