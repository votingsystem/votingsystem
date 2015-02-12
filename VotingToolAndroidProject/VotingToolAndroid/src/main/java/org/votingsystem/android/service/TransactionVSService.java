package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.WalletActivity;
import org.votingsystem.android.callable.MessageTimeStamper;
import org.votingsystem.android.callable.SignedMapSender;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.util.MsgUtils;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.Utils;
import org.votingsystem.android.util.Wallet;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Cooin;
import org.votingsystem.model.CooinAccountsInfo;
import org.votingsystem.model.CooinBatch;
import org.votingsystem.model.CooinServer;
import org.votingsystem.model.TransactionRequest;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TimestampException;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSService extends IntentService {

    public static final String TAG = TransactionVSService.class.getSimpleName();

    public static final long FOUR_MINUTES = 60 * 4 * 1000;

    public TransactionVSService() { super(TAG); }

    private AppContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {
        contextVS = (AppContextVS) getApplicationContext();
        if(contextVS.getCooinServer() == null) {
            LOGD(TAG + ".updateUserInfo", "missing connection to Cooin Server");
            Toast.makeText(contextVS, contextVS.getString(R.string.server_connection_error_msg,
                    contextVS.getCooinServerURL()), Toast.LENGTH_LONG).show();
            return;
        }
        final Bundle arguments = intent.getExtras();
        TypeVS operation = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        String pin = arguments.getString(ContextVS.PIN_KEY);
        TransactionVS transactionVS = (TransactionVS) intent.getSerializableExtra(ContextVS.TRANSACTION_KEY);
        TransactionRequest transactionRequest = null;
        if(arguments.getString(ContextVS.JSON_DATA_KEY) != null) {
            try {
                JSONObject transactionRequestJSON =
                        new JSONObject(arguments.getString(ContextVS.JSON_DATA_KEY));
                transactionRequest = TransactionRequest.parse(transactionRequestJSON);
            } catch (Exception ex) { ex.printStackTrace(); }
        }
        try {
            switch(operation) {
                case COOIN_ACCOUNTS_INFO:
                    updateUserInfo(serviceCaller);
                    break;
                case COOIN_CHECK:
                    checkCooins(serviceCaller);
                    break;
                case COOIN_REQUEST:
                    cooinRequest(serviceCaller, CooinBatch.getRequestBatch(transactionVS,
                            contextVS.getCooinServer()), pin);
                    break;
                /*case COOIN_SEND:
                    CooinBatch cooinBatch = new CooinBatch(transactionVS.getAmount(),
                            transactionVS.getAmount(), transactionVS.getCurrencyCode(), tagVS,
                            transactionVS.isTimeLimited(), contextVS.getCooinServer());
                    requestAndSendCooin(serviceCaller, cooinBatch, pin);
                    break;*/
                case SIGNED_TRANSACTION:
                case ANONYMOUS_SIGNED_TRANSACTION:
                    processTransaction(serviceCaller, transactionRequest, operation);
                    break;
                case CASH_SEND:
                    break;
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            broadCastResponse(Utils.getBroadcastResponse(operation, serviceCaller,
                    ResponseVS.getExceptionResponse(ex, this), this));
        }
    }

    private void processTransaction(String serviceCaller,
              TransactionRequest transactionRequest, TypeVS operation) {
        LOGD(TAG + ".processTransaction", "processTransaction - operation: " + operation);
        UserVS userVS = PrefUtils.getSessionUserVS(this);
        ResponseVS responseVS = null;
        if(transactionRequest.getDate() != null && DateUtils.inRange(transactionRequest.getDate(),
                Calendar.getInstance().getTime(), FOUR_MINUTES)) {
            try {
                switch (transactionRequest.getPaymentMethod()) {
                    case SIGNED_TRANSACTION:
                        responseVS = sendTransactionVS(transactionRequest.getIBAN(),
                                transactionRequest.getSignedTransaction(userVS.getIBAN()));
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                                    ContentTypeVS.TEXT, transactionRequest.getPaymentConfirmURL());
                        }
                        break;
                    case ANONYMOUS_SIGNED_TRANSACTION:
                        responseVS = sendAnonymousSignedTransactionVS(transactionRequest);
                        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                                    ContentTypeVS.TEXT, transactionRequest.getPaymentConfirmURL());
                            responseVS.setMessage(MsgUtils.getAnonymousSignedTransactionOKMsg(
                                    transactionRequest, this));
                        }
                        break;
                    case CASH_SEND:
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseVS.getExceptionResponse(ex, this);
            }
        } else responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                getString(R.string.payment_session_expired_msg));
        broadCastResponse(Utils.getBroadcastResponse(operation, serviceCaller, responseVS,
                contextVS));
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
            //TODO reingresar cooins en cuenta
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            if(ResponseVS.SC_OK != responseVS.getStatusCode() &&
                    ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
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

    private ResponseVS cooinRequest(String serviceCaller, CooinBatch requestBatch, String pin){
        CooinServer cooinServer = requestBatch.getCooinServer();
        ResponseVS responseVS = null;
        try {
            LOGD(TAG + ".cooinRequest", "Amount: " + requestBatch.getTotalAmount().toPlainString());
            String messageSubject = getString(R.string.cooin_request_msg_subject);
            String fromUser = contextVS.getUserVS().getNif();
            String requestDataFileName = ContextVS.COOIN_REQUEST_DATA_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED.getName();
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            JSONArray cooinCSRList = new JSONArray(requestBatch.getCooinCSRList());
            mapToSend.put(ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.JSON.getName(),
                    cooinCSRList.toString().getBytes());
            SignedMapSender signedMapSender = new SignedMapSender(fromUser,
                    cooinServer.getName(), requestBatch.getRequestDataToSignJSON().toString(),
                    mapToSend, messageSubject, null, cooinServer.getCooinRequestServiceURL(),
                    requestDataFileName, (AppContextVS)getApplicationContext());
            responseVS = signedMapSender.call();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONObject responseJSON = new JSONObject (new String(responseVS.getMessageBytes(), "UTF-8"));
                requestBatch.initCooins(responseJSON.getJSONArray("issuedCooins"));
                responseVS.setCaption(getString(R.string.cooin_request_ok_caption)).setNotificationMessage(
                        getString(R.string.cooin_request_ok_msg, requestBatch.getTotalAmount(),
                        requestBatch.getCurrencyCode()));
                Wallet.saveCooinList(requestBatch.getCooinsMap().values(), pin, contextVS);
                updateUserInfo(serviceCaller);
            } else responseVS.setCaption(getString(
                    R.string.cooin_request_error_caption));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            contextVS.broadcastResponse(
                    responseVS.setTypeVS(TypeVS.COOIN_REQUEST).setServiceCaller(serviceCaller));
            return responseVS;
        }
    }

    private ResponseVS sendTransactionVS(String toUserIBAN, JSONObject transactionVSJSON) {
        LOGD(TAG + ".sendTransactionVS", "transactionVS: " + transactionVSJSON.toString());
        ResponseVS responseVS = null;
        try {
            CooinServer cooinServer = contextVS.getCooinServer();
            responseVS = contextVS.signMessage(toUserIBAN,
                    transactionVSJSON.toString(), getString(R.string.FROM_USERVS_msg_subject));
            responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(),
                    ContentTypeVS.JSON_SIGNED, cooinServer.getTransactionVSServiceURL());
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            return responseVS;
        }
    }

    private ResponseVS sendAnonymousSignedTransactionVS(TransactionRequest transactionRequest) {
        LOGD(TAG + ".sendAnonymousSignedTransactionVS", "sendAnonymousSignedTransactionVS");
        ResponseVS responseVS = null;
        try {
            CooinServer cooinServer = contextVS.getCooinServer();
            Wallet.CooinBundle cooinBundle = Wallet.getCooinBundleForTransaction(
                    transactionRequest.getAmount(), transactionRequest.getCurrencyCode(),
                    transactionRequest.getTagVS());
            Map requestMap = new HashMap();
            Cooin leftOverCooin = cooinBundle.getLeftOverCooin(transactionRequest.getAmount(),
                    cooinServer.getServerURL());
            if(leftOverCooin != null) requestMap.put("csrCooin", new String(
                    leftOverCooin.getCertificationRequest().getCsrPEM(), "UTF-8"));
            requestMap.put("cooins", cooinBundle.getTransactionData(transactionRequest, contextVS));
            responseVS = HttpHelper.sendData(new JSONObject(requestMap).toString().getBytes(),
                    ContentTypeVS.JSON, cooinServer.getCooinTransactionServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONObject responseJSON = responseVS.getMessageJSON();
                if(leftOverCooin != null) {
                    leftOverCooin.initSigner(responseJSON.getString("leftOverCoin").getBytes());
                    leftOverCooin.setState(Cooin.State.OK);
                }
                SMIMEMessage receipt = new SMIMEMessage(new ByteArrayInputStream(
                        Base64.decode(responseJSON.getString("receipt"))));
                transactionRequest.setAnonymousSignedTransactionReceipt(receipt,
                        cooinBundle.getCooinList());
                cooinBundle.updateWallet(leftOverCooin, contextVS);
                responseVS.setSMIME(receipt);
            } else if(ResponseVS.SC_COOIN_EXPENDED == responseVS.getStatusCode()) {
                Cooin expendedCooin = Wallet.removeExpendedCooin(responseVS.getMessage(), contextVS);
                responseVS.setMessage(getString(R.string.expended_cooin_error_msg, expendedCooin.
                        getAmount().toString() + " " + expendedCooin.getCurrencyCode()));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = ResponseVS.getExceptionResponse(ex, this);
        } finally {
            return responseVS;
        }
    }

    private void updateUserInfo(String serviceCaller) {
        LOGD(TAG + ".updateUserInfo", "updateUserInfo");
        if(contextVS.getCooinServer() == null) {
            LOGD(TAG + ".updateUserInfo", "missing connection to Cooin Server");
            return;
        }
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

    private void checkCooins(String serviceCaller) {
        LOGD(TAG + ".checkCooins", "checkCooins");
        ResponseVS responseVS = null;
        try {
            List<String> hashCertVSList = Wallet.getHashCertVSList();
            if(hashCertVSList == null) {
                LOGD(TAG + ".checkCooins", "empty hashCertVSList");
                return;
            }
            responseVS = HttpHelper.sendData(new JSONArray(hashCertVSList).toString().getBytes(),
                    ContentTypeVS.JSON,  contextVS.getCooinServer().getCooinBundleStateServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONArray result = new JSONArray(responseVS.getMessage());
                List<String> cooinWithErrorList = new ArrayList<>();
                List<Cooin> cooinWithErrors = null;
                for(int i = 0; i < result.length(); i++) {
                    JSONObject cooinData = result.getJSONObject(i);
                    if(Cooin.State.OK != Cooin.State.valueOf(cooinData.getString("state"))) {
                        cooinWithErrorList.add(cooinData.getString("hashCertVS"));
                    }
                    if(cooinWithErrorList.size() > 0) {
                        cooinWithErrors = Wallet.updateCooinWithErrors(cooinWithErrorList, contextVS);
                    }
                }
                if(cooinWithErrors != null && !cooinWithErrors.isEmpty()) {
                    responseVS = new ResponseVS(ResponseVS.SC_ERROR, MsgUtils.getUpdateCooinsWithErrorMsg(
                            cooinWithErrors, contextVS));
                    responseVS.setCaption(getString(R.string.error_lbl)).setServiceCaller(
                            serviceCaller).setTypeVS(TypeVS.COOIN_CHECK);
                    Intent intent = new Intent(this, WalletActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
                    startActivity(intent);
                }
            }
        } catch(Exception ex) {  ex.printStackTrace(); }
    }

    private void broadCastResponse(ResponseVS responseVS) {
        contextVS.showNotification(responseVS);
        contextVS.broadcastResponse(responseVS);
    }

}