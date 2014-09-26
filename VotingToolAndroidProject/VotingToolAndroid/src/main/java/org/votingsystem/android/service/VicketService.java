package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.bouncycastle2.asn1.DERTaggedObject;
import org.bouncycastle2.asn1.DERUTF8String;
import org.bouncycastle2.util.encoders.Base64;
import org.bouncycastle2.x509.extension.X509ExtensionUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.callable.MessageTimeStamper;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.android.callable.SignedMapSender;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.contentprovider.VicketContentProvider;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVSTransactionVSListInfo;
import org.votingsystem.model.Vicket;
import org.votingsystem.model.VicketServer;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TimestampException;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        TypeVS operationType = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        Uri uriData =  arguments.getParcelable(ContextVS.URI_KEY);;
        BigDecimal amount = (BigDecimal) arguments.getSerializable(ContextVS.VALUE_KEY);
        String currencyCode = (String) arguments.getSerializable(ContextVS.CURRENCY_KEY);
        ResponseVS responseVS = null;
        switch(operationType) {
            case VICKET_USER_INFO:
                responseVS = updateUserInfo();
                responseVS.setTypeVS(operationType);
                responseVS.setServiceCaller(serviceCaller);
                contextVS.sendBroadcast(responseVS);
                break;
            case VICKET_REQUEST:
                responseVS = vicketRequest(amount, currencyCode);
                responseVS.setTypeVS(operationType);
                responseVS.setServiceCaller(serviceCaller);
                contextVS.showNotification(responseVS);
                contextVS.sendBroadcast(responseVS);
                break;
            case VICKET_SEND:
                amount = new BigDecimal(uriData.getQueryParameter("amount"));
                currencyCode = uriData.getQueryParameter("currency");
                String subject = uriData.getQueryParameter("subject");
                String receptor = uriData.getQueryParameter("receptor");
                String IBAN = uriData.getQueryParameter("IBAN");
                String tagVS = uriData.getQueryParameter("tagVS");
                responseVS = vicketSend(amount, currencyCode, subject, receptor, IBAN, tagVS);
                responseVS.setTypeVS(operationType);
                responseVS.setServiceCaller(serviceCaller);
                contextVS.showNotification(responseVS);
                contextVS.sendBroadcast(responseVS);
                break;
            case VICKET_CANCEL:
                Integer vicketCursorPosition = arguments.getInt(ContextVS.ITEM_ID_KEY);
                Cursor cursor = getContentResolver().query(VicketContentProvider.CONTENT_URI,
                        null, null, null, null);
                cursor.moveToPosition(vicketCursorPosition);
                byte[] serializedVicket = cursor.getBlob(cursor.getColumnIndex(
                        VicketContentProvider.SERIALIZED_OBJECT_COL));
                Long vicketId = cursor.getLong(cursor.getColumnIndex(VicketContentProvider.ID_COL));
                try {
                    Vicket vicket = (Vicket) ObjectUtils.deSerializeObject(serializedVicket);
                    vicket.setLocalId(vicketCursorPosition.longValue());
                    responseVS = cancelVicket(vicket);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        vicket.setCancellationReceipt(responseVS.getSmimeMessage());
                        vicket.setState(Vicket.State.CANCELLED);
                        ContentValues values = VicketContentProvider.populateVicketContentValues(
                                contextVS, vicket);
                        getContentResolver().update(VicketContentProvider.getVicketURI(vicketId),
                                values, null, null);
                        responseVS.setCaption(getString(R.string.vicket_cancellation_msg_subject));
                        responseVS.setNotificationMessage(getString(R.string.vicket_cancellation_msg));
                        responseVS.setIconId(R.drawable.accept_22);
                    } else {
                        responseVS.setCaption(getString(R.string.vicket_cancellation_error_msg_subject));
                        responseVS.setNotificationMessage(getString(R.string.vicket_cancellation_error_msg_subject));
                        responseVS.setIconId(R.drawable.cancel_22);
                        if(responseVS.getContentType() == ContentTypeVS.JSON_SIGNED) {
                            SMIMEMessageWrapper signedMessage = responseVS.getSmimeMessage();
                            Log.d(TAG + ".cancelVicket(...)", "error JSON response: " + signedMessage.getSignedContent());
                            JSONObject jsonResponse = new JSONObject(signedMessage.getSignedContent());
                            TypeVS operation = TypeVS.valueOf(jsonResponse.getString("operation"));
                            if(TypeVS.VICKET_CANCEL == operation) {
                                vicket.setCancellationReceipt(responseVS.getSmimeMessage());
                                vicket.setState(Vicket.State.LAPSED);
                                vicket.setLocalId(vicketId);
                                VicketContentProvider.updateVicket(contextVS, vicket);
                                responseVS.setCaption(getString(R.string.vicket_cancellation_msg_subject));
                                responseVS.setNotificationMessage(getString(R.string.vicket_cancellation_msg));
                                responseVS.setIconId(R.drawable.accept_22);
                            }
                        }
                    }
                    responseVS.setServiceCaller(serviceCaller);
                    contextVS.showNotification(responseVS);
                    contextVS.sendBroadcast(responseVS);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
        }
    }

    private ResponseVS cancelVicket(Vicket vicket) {
        ResponseVS responseVS = getVicketServer();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        VicketServer vicketServer = (VicketServer) responseVS.getData();
        Map vicketCancellationDataMap = new HashMap();
        vicketCancellationDataMap.put("UUID", UUID.randomUUID().toString());
        vicketCancellationDataMap.put("operation", TypeVS.VICKET_CANCEL.toString());
        vicketCancellationDataMap.put("hashCertVSBase64", vicket.getHashCertVSBase64());
        vicketCancellationDataMap.put("originHashCertVS", vicket.getOriginHashCertVS());
        vicketCancellationDataMap.put("vicketCertSerialNumber", vicket.getCertificationRequest().
                getCertificate().getSerialNumber().longValue());
        String textToSing = new JSONObject(vicketCancellationDataMap).toString();
        SMIMESignedSender signedSender = new SMIMESignedSender(contextVS.getUserVS().getNif(),
                vicketServer.getNameNormalized(), vicketServer.getVicketCancelServiceURL(),
                textToSing, ContentTypeVS.JSON_SIGNED,
                getString(R.string.vicket_cancellation_msg_subject), vicketServer.getCertificate(),
                (AppContextVS)getApplicationContext());
        responseVS = signedSender.call();
        return responseVS;
    }

    private ResponseVS cancelVickets(Collection<Vicket> sendedVickets) {
        Log.d(TAG + ".cancelVickets(...)", "cancelVickets");
        VicketServer vicketServer = contextVS.getVicketServer();
        ResponseVS responseVS = null;
        try {
            List<String> cancellationList = new ArrayList<String>();
            for(Vicket vicket : sendedVickets) {
                Map vicketCancellationDataMap = new HashMap();
                vicketCancellationDataMap.put("UUID", UUID.randomUUID().toString());
                vicketCancellationDataMap.put("operation", TypeVS.VICKET_CANCEL.toString());
                vicketCancellationDataMap.put("hashCertVSBase64", vicket.getHashCertVSBase64());
                vicketCancellationDataMap.put("originHashCertVS", vicket.getOriginHashCertVS());
                vicketCancellationDataMap.put("vicketCertSerialNumber", vicket.getCertificationRequest().
                        getCertificate().getSerialNumber().longValue());
                String textToSing = new JSONObject(vicketCancellationDataMap).toString();
                responseVS = contextVS.signMessage(vicketServer.getNameNormalized(), textToSing,
                        getString(R.string.vicket_cancellation_msg_subject));
                cancellationList.add(new String(Base64.encode(responseVS.getSmimeMessage().getBytes())));
            }
            Map requestMap = new HashMap();
            requestMap.put("vicketCancellationList", cancellationList);
            responseVS = HttpHelper.sendData(new JSONObject(requestMap).toString().getBytes(),
                    ContentTypeVS.JSON, vicketServer.getVicketBatchCancellationServiceURL());
        } catch(Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            if(message == null || message.isEmpty()) message = getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(getString(R.string.exception_lbl),
                    message);
        } finally {
            return responseVS;
        }
    }

    private ResponseVS vicketSend(BigDecimal requestAmount, String currencyCode,
              String subject, String receptor, String IBAN, String tagVS) {
        ResponseVS responseVS = null;
        String message = null;
        String caption = null;
        byte[] decryptedMessageBytes = null;
        Integer iconId = R.drawable.cancel_22;
        Map <Long,Vicket> sendedVicketsMap = new HashMap<Long, Vicket>();
        try {
            UserVSTransactionVSListInfo userInfo = contextVS.getUserVSTransactionVSListInfo();
            BigDecimal available = userInfo.getAvailableForTagVS(currencyCode, tagVS);
            if(available.compareTo(requestAmount) < 0) {
                throw new Exception(getString(R.string.insufficient_cash_msg, currencyCode,
                        requestAmount.toString(), available.toString()));
            }
            responseVS = getVicketServer();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            VicketServer vicketServer = (VicketServer) responseVS.getData();

            BigDecimal vicketAmount = new BigDecimal(10);
            int numVickets = requestAmount.divide(vicketAmount).intValue();
            List<Vicket> vicketsToSend = new ArrayList<Vicket>();
            for(int i = 0; i < numVickets; i++) {
                vicketsToSend.add(contextVS.getVicketList(Currency.getInstance("EUR").getCurrencyCode()).remove(0));
            }
            Map mapToSend = new HashMap();
            mapToSend.put("receptor", receptor);
            mapToSend.put("operation", TypeVS.VICKET.toString());
            mapToSend.put("subject", subject);
            mapToSend.put("IBAN", IBAN);
            mapToSend.put("tagVS", tagVS);
            mapToSend.put("currency", currencyCode);
            mapToSend.put("amount", requestAmount);

            List<String> smimeVicketList = new ArrayList<String>();

            for(Vicket vicket : vicketsToSend) {
                mapToSend.put("UUID", UUID.randomUUID().toString());
                String textToSign = new JSONObject(mapToSend).toString();
                SMIMEMessageWrapper smimeMessage = vicket.getCertificationRequest().genMimeMessage(
                        vicket.getHashCertVSBase64(), StringUtils.getNormalized(receptor),
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
                mapToSend.put("amount", requestAmount.toString());
                mapToSend.put("currency", currencyCode);
                mapToSend.put("operation", TypeVS.VICKET_SEND.toString());
                mapToSend.put("vickets", smimeVicketList);
                mapToSend.put("publicKey", publicKeyStr);
                String textToSign = new JSONObject(mapToSend).toString();
                responseVS = HttpHelper.sendData(textToSign.getBytes(), ContentTypeVS.JSON,
                        vicketServer.getVicketBatchServiceURL());
                if(responseVS.getContentType()!= null && responseVS.getContentType().isEncrypted()) {
                    decryptedMessageBytes = Encryptor.decryptCMS(keyPair.getPrivate(),
                            responseVS.getMessageBytes());
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        JSONObject jsonResponse = new JSONObject(new String(decryptedMessageBytes));
                        JSONArray jsonArray = jsonResponse.getJSONArray("vickets");
                        Date dateCreated = null;
                        for(int i = 0; i < jsonArray.length(); i ++) {
                            SMIMEMessageWrapper smimeMessage = new SMIMEMessageWrapper(null,
                                    new ByteArrayInputStream(Base64.decode(jsonArray.getString(i))), null);
                            dateCreated = smimeMessage.getSigner().getTimeStampToken().
                                    getTimeStampInfo().getGenTime();
                            Vicket sendedVicket = sendedVicketsMap.get(smimeMessage.getSigner().getCertificate().
                                    getSerialNumber().longValue());
                            sendedVicket.setReceiptBytes(smimeMessage.getBytes());
                            sendedVicket.setState(Vicket.State.EXPENDED);
                            VicketContentProvider.updateVicket(contextVS, sendedVicket);
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
        } catch(TimestampException tex) {
            responseVS.setStatusCode(ResponseVS.SC_ERROR_TIMESTAMP);
            responseVS.setCaption(getString(R.string.timestamp_service_error_caption));
            responseVS.setNotificationMessage(tex.getMessage());
        } catch(Exception ex) {
            ex.printStackTrace();
            cancelVickets(sendedVicketsMap.values());
            message = ex.getMessage();
            if(message == null || message.isEmpty()) message = getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(getString(R.string.exception_lbl),
                    message);
        } finally {
            if(ResponseVS.SC_OK != responseVS.getStatusCode() &&
                    ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                Log.d(TAG + ".cancelVicketRepeated(...)", "cancelVicketRepeated");
                try {
                    JSONObject responseJSON = new JSONObject(new String(decryptedMessageBytes, ContextVS.UTF_8));
                    String base64EncodedVicketRepeated = responseJSON.getString("messageSMIME");
                    SMIMEMessageWrapper smimeMessage = new SMIMEMessageWrapper(null,
                            new ByteArrayInputStream(Base64.decode(base64EncodedVicketRepeated)), null);
                    Long repeatedCertSerialNumber = smimeMessage.getSigner().getCertificate().
                            getSerialNumber().longValue();
                    Vicket repeatedVicket = sendedVicketsMap.get(repeatedCertSerialNumber);
                    repeatedVicket.setState(Vicket.State.EXPENDED);
                    VicketContentProvider.updateVicket(contextVS, repeatedVicket);
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
                for(Vicket vicket:sendedVicketsMap.values())
                    VicketContentProvider.updateVicket(contextVS, vicket);

                iconId = R.drawable.fa_money_24;
                caption = getString(R.string.vicket_send_ok_caption);
                message = getString(R.string.vicket_send_ok_msg, requestAmount.toString(),
                        currencyCode, subject, receptor);
            }
            responseVS.setIconId(iconId);
            responseVS.setCaption(caption);
            responseVS.setNotificationMessage(message);
            return responseVS;
        }
    }

    private ResponseVS vicketRequest(BigDecimal requestAmount, String currencyCode) {
        ResponseVS responseVS = getVicketServer();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        VicketServer vicketServer = (VicketServer) responseVS.getData();
        Map<String, Vicket> vicketsMap = new HashMap<String, Vicket>();
        String caption = null;
        String message = null;
        int iconId = R.drawable.cancel_22;
        try {
            BigDecimal numVickets = requestAmount.divide(new BigDecimal(10));
            BigDecimal vicketsValue = new BigDecimal(10);
            List<Vicket> vicketList = new ArrayList<Vicket>();
            for(int i = 0; i < numVickets.intValue(); i++) {
                Vicket vicket = new Vicket(vicketServer.getServerURL(),
                        vicketsValue, currencyCode, TypeVS.VICKET);
                vicketList.add(vicket);
                vicketsMap.put(vicket.getHashCertVSBase64(), vicket);
            }

            String messageSubject = getString(R.string.vicket_request_msg_subject);
            String fromUser = contextVS.getUserVS().getNif();

            Map requestVicketMap = new HashMap();
            requestVicketMap.put("numVickets", numVickets.intValue());
            requestVicketMap.put("vicketValue", vicketsValue.intValue());

            List vicketsMapList = new ArrayList();
            vicketsMapList.add(requestVicketMap);

            Map smimeContentMap = new HashMap();
            smimeContentMap.put("totalAmount", requestAmount.toString());
            smimeContentMap.put("currency", currencyCode);
            smimeContentMap.put("vickets", vicketsMapList);
            smimeContentMap.put("UUID", UUID.randomUUID().toString());
            smimeContentMap.put("serverURL", contextVS.getVicketServer().getServerURL());
            smimeContentMap.put("operation", TypeVS.VICKET_REQUEST.toString());
            JSONObject requestJSON = new JSONObject(smimeContentMap);

            String requestDataFileName = ContextVS.VICKET_REQUEST_DATA_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED.getName();

            List<Map> vicketCSRList = new ArrayList<Map>();
            for(Vicket vicket : vicketList) {
                Map csrVicketMap = new HashMap();
                csrVicketMap.put("currency", currencyCode);
                csrVicketMap.put("vicketValue", vicketsValue);
                csrVicketMap.put("csr", new String(vicket.getCertificationRequest().getCsrPEM(), "UTF-8"));
                vicketCSRList.add(csrVicketMap);
            }
            Map csrRequestMap = new HashMap();
            csrRequestMap.put("vicketCSR", vicketCSRList);
            JSONObject csrRequestJSON = new JSONObject(csrRequestMap);

            byte[] encryptedCSRBytes = Encryptor.encryptMessage(csrRequestJSON.toString().getBytes(),
                    vicketServer.getCertificate());
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.ENCRYPTED.getName();
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, encryptedCSRBytes);

            KeyStore.PrivateKeyEntry keyEntry = contextVS.getUserPrivateKey();
            SignedMapSender signedMapSender = new SignedMapSender(fromUser,
                    vicketServer.getNameNormalized(),
                    requestJSON.toString(), mapToSend, messageSubject, null,
                    vicketServer.getVicketRequestServiceURL(),
                    requestDataFileName, ContentTypeVS.JSON_SIGNED,
                    vicketServer.getCertificate(),
                    keyEntry.getCertificate().getPublicKey(), keyEntry.getPrivateKey(),
                    (AppContextVS)getApplicationContext());
            responseVS = signedMapSender.call();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONObject issuedVicketsJSON = new JSONObject(new String(
                        responseVS.getMessageBytes(), "UTF-8"));

                JSONArray transactionsArray = issuedVicketsJSON.getJSONArray("transactionList");
                for(int i = 0; i < transactionsArray.length(); i++) {
                    TransactionVS transaction = TransactionVS.parse(transactionsArray.getJSONObject(i));
                    TransactionVSContentProvider.addTransaction(contextVS, transaction, null);
                }

                JSONArray issuedVicketsArray = issuedVicketsJSON.getJSONArray("issuedVickets");
                Log.d(TAG + "vicketRequest(...)", "Num IssuedVickets: " + issuedVicketsArray.length());
                if(issuedVicketsArray.length() != vicketList.size()) {
                    Log.e(TAG + "vicketRequest(...)", "ERROR - Num vickets requested: " +
                            vicketList.size() + " - num. vickets received: " +
                            issuedVicketsArray.length());
                }
                for(int i = 0; i < issuedVicketsArray.length(); i++) {
                    Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(
                            issuedVicketsArray.getString(i).getBytes());
                    if(certificates.isEmpty()) throw new Exception (" --- missing certs --- ");
                    X509Certificate x509Certificate = certificates.iterator().next();
                    byte[] vicketExtensionValue = x509Certificate.getExtensionValue(ContextVS.VICKET_OID);
                    DERTaggedObject vicketCertDataDER = (DERTaggedObject)
                            X509ExtensionUtil.fromExtensionValue(vicketExtensionValue);
                    JSONObject vicketCertData = new JSONObject(((DERUTF8String)
                            vicketCertDataDER.getObject()).toString());
                    String hashCertVS = vicketCertData.getString("hashCertVS");
                    Vicket vicket = vicketsMap.get(hashCertVS);
                    vicket.setState(Vicket.State.OK);
                    vicket.getCertificationRequest().initSigner(issuedVicketsArray.getString(i).getBytes());
                }
                VicketContentProvider.insertVickets(contextVS, vicketsMap.values());
                caption = getString(R.string.vicket_request_ok_caption);
                message = getString(R.string.vicket_request_ok_msg, requestAmount.toString(),
                        currencyCode);
                iconId = R.drawable.fa_money_24;
            } else {
                caption = getString(R.string.vicket_request_error_caption);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            message = ex.getMessage();
            if(message == null || message.isEmpty()) message = getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(getString(R.string.exception_lbl),
                    message);
        } finally {
            responseVS.setIconId(iconId);
            responseVS.setCaption(caption);
            responseVS.setNotificationMessage(message);
            return responseVS;
        }
    }

    private ResponseVS updateUserInfo() {
        Log.d(TAG + ".updateUserInfo(...)", "updateUserInfo");
        ResponseVS responseVS = getVicketServer();
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        VicketServer vicketServer = (VicketServer) responseVS.getData();
        Map mapToSend = new HashMap();
        mapToSend.put("NIF", contextVS.getUserVS().getNif());
        mapToSend.put("operation", TypeVS.VICKET_USER_INFO.toString());
        mapToSend.put("UUID", UUID.randomUUID().toString());
        String msgSubject = getString(R.string.vicket_user_info_request_msg_subject);
        try {
            JSONObject userInfoRequestJSON = new JSONObject(mapToSend);
            SMIMESignedSender smimeSignedSender = new SMIMESignedSender(contextVS.getUserVS().getNif(),
                    vicketServer.getNameNormalized(), vicketServer.getUserInfoServiceURL(),
                    userInfoRequestJSON.toString(), ContentTypeVS.JSON_SIGNED,
                    msgSubject, vicketServer.getCertificate(), contextVS);
            responseVS = smimeSignedSender.call();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                String responseStr = responseVS.getMessage();
                UserVSTransactionVSListInfo userInfo = UserVSTransactionVSListInfo.parse(
                        new JSONObject(responseStr));
                contextVS.setUserVSTransactionVSListInfo(userInfo,
                        DateUtils.getWeekPeriod(Calendar.getInstance()));
                TransactionVSContentProvider.updateUserVSTransactionVSList(contextVS, userInfo);
            } else {
                responseVS.setCaption(getString(R.string.error_lbl));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            if(message == null || message.isEmpty()) message = getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(getString(R.string.exception_lbl),
                    message);
        } finally {
            return responseVS;
        }
    }

    private ResponseVS getVicketServer() {
        ResponseVS responseVS = null;
        VicketServer vicketServer = contextVS.getVicketServer();
        if(vicketServer != null) {
            responseVS = new ResponseVS(ResponseVS.SC_OK);
            responseVS.setData(vicketServer);
        } else {
            try {
                responseVS = HttpHelper.getData(ActorVS.getServerInfoURL(contextVS.getVicketServerURL()),
                        ContentTypeVS.JSON);
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    vicketServer = (VicketServer) ActorVS.parse(new JSONObject(responseVS.getMessage()));
                    contextVS.setVicketServer(vicketServer);
                }
            } catch(Exception ex) {
                ex.printStackTrace();
                String message = ex.getMessage();
                if(message == null || message.isEmpty()) message = getString(R.string.exception_lbl);
                responseVS = ResponseVS.getExceptionResponse(getString(R.string.exception_lbl),
                        message);
            }
        }
        responseVS.setData(vicketServer);
        return responseVS;
    }

}