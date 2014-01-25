package org.votingsystem.android.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;

import org.bouncycastle2.asn1.DERTaggedObject;
import org.bouncycastle2.asn1.DERUTF8String;
import org.bouncycastle2.x509.extension.X509ExtensionUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.MessageActivity;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.android.callable.SignedMapSender;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.AnonymousDelegationVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.CurrencyVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TicketAccount;
import org.votingsystem.model.TicketServer;
import org.votingsystem.model.TicketVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ObjectUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketService extends IntentService {

    public static final String TAG = "TicketService";

    public TicketService() { super(TAG); }

    private AppContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {
        contextVS = (AppContextVS) getApplicationContext();
        final Bundle arguments = intent.getExtras();
        TypeVS operationType = (TypeVS)arguments.getSerializable(ContextVS.TYPEVS_KEY);
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        String pin = arguments.getString(ContextVS.PIN_KEY);
        BigDecimal value = (BigDecimal) arguments.getSerializable(ContextVS.VALUE_KEY);
        ResponseVS responseVS = null;
        switch(operationType) {
            case TICKET_USER_INFO:
                responseVS = updateUserInfo(pin);
                responseVS.setTypeVS(operationType);
                responseVS.setServiceCaller(serviceCaller);
                sendMessage(responseVS);
                break;
            case TICKET_REQUEST:
                responseVS = ticketRequest(value, pin);
                responseVS.setTypeVS(operationType);
                responseVS.setServiceCaller(serviceCaller);
                showNotification(responseVS);
                sendMessage(responseVS);
                break;
        }
    }

    private ResponseVS ticketRequest(BigDecimal withdrawalAmount, String pin) {
        ResponseVS responseVS = null;
        TicketServer ticketServer = contextVS.getTicketServer();
        Map<String, TicketVS> ticketsMap = new HashMap<String, TicketVS>();
        String caption = null;
        String message = null;
        int iconId = R.drawable.cancel_22;
        try {
            if(ticketServer == null) {
                responseVS = initTicketServer();
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
                else ticketServer = contextVS.getTicketServer();
            }
            BigDecimal numTickets = withdrawalAmount.divide(new BigDecimal(10));
            BigDecimal ticketsValue = new BigDecimal(10);
            List<TicketVS> ticketList = new ArrayList<TicketVS>();
            for(int i = 0; i < numTickets.intValue(); i++) {
                TicketVS ticketVS = new TicketVS(ticketServer.getServerURL(),
                        ticketsValue, CurrencyVS.Euro, TypeVS.TICKET);
                ticketList.add(ticketVS);
                ticketsMap.put(ticketVS.getHashCertVSBase64(), ticketVS);
            }

            String messageSubject = getString(R.string.ticket_withdrawal_msg_subject);
            String fromUser = contextVS.getUserVS().getNif();

            Map requestTicketMap = new HashMap();
            requestTicketMap.put("numTickets", numTickets.intValue());
            requestTicketMap.put("ticketValue", ticketsValue.intValue());

            List ticketsMapList = new ArrayList();
            ticketsMapList.add(requestTicketMap);

            Map smimeContentMap = new HashMap();
            smimeContentMap.put("totalAmount", withdrawalAmount.toString());
            smimeContentMap.put("currency", CurrencyVS.Euro.toString());
            smimeContentMap.put("tickets", ticketsMapList);
            smimeContentMap.put("UUID", UUID.randomUUID().toString());
            smimeContentMap.put("serverURL", contextVS.getTicketServer().getServerURL());
            smimeContentMap.put("operation", TypeVS.TICKET_REQUEST.toString());
            JSONObject requestJSON = new JSONObject(smimeContentMap);

            String withdrawalDataFileName = ContextVS.TICKET_REQUEST_DATA_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED.getName();

            List<Map> ticketCSRList = new ArrayList<Map>();
            for(TicketVS ticket : ticketList) {
                Map csrTicketMap = new HashMap();
                csrTicketMap.put("currency", CurrencyVS.Euro.toString());
                csrTicketMap.put("ticketValue", ticketsValue);
                csrTicketMap.put("csr", new String(ticket.getCertificationRequest().getCsrPEM(),"UTF-8"));
                ticketCSRList.add(csrTicketMap);
            }
            Map csrRequestMap = new HashMap();
            csrRequestMap.put("ticketCSR", ticketCSRList);
            JSONObject csrRequestJSON = new JSONObject(csrRequestMap);

            byte[] encryptedCSRBytes = Encryptor.encryptMessage(csrRequestJSON.toString().getBytes(),
                    ticketServer.getCertificate());
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.ENCRYPTED.getName();
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, encryptedCSRBytes);

            FileInputStream fis = contextVS.openFileInput(KEY_STORE_FILE);
            byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, pin.toCharArray());
            PrivateKey privateKey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS, pin.toCharArray());
            Certificate[] chain = keyStore.getCertificateChain(USER_CERT_ALIAS);
            PublicKey publicKey = ((X509Certificate)chain[0]).getPublicKey();

            SignedMapSender signedMapSender = new SignedMapSender(fromUser,
                    ticketServer.getNameNormalized(),
                    requestJSON.toString(), mapToSend, messageSubject, null,
                    ticketServer.getTicketRequestServiceURL(),
                    withdrawalDataFileName, ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED,
                    pin.toCharArray(), ticketServer.getCertificate(),
                    publicKey, privateKey, (AppContextVS)getApplicationContext());
            responseVS = signedMapSender.call();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                JSONObject issuedTicketsJSON = new JSONObject(new String(
                        responseVS.getMessageBytes(), "UTF-8"));
                JSONArray issuedTicketsArray = issuedTicketsJSON.getJSONArray("issuedTickets");
                Log.d(TAG + "ticketRequest(...)", "Num IssuedTickets: " + issuedTicketsArray.length());
                if(issuedTicketsArray.length() != ticketList.size()) {
                    Log.e(TAG + "ticketRequest(...)", "ERROR - Num tickets requested: " +
                            ticketList.size() + " - num. tickets received: " +
                            issuedTicketsArray.length());
                }
                for(int i = 0; i < issuedTicketsArray.length(); i++) {
                    Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(
                            issuedTicketsArray.getString(i).getBytes());
                    if(certificates.isEmpty()) throw new Exception (" --- missing certs --- ");
                    X509Certificate x509Certificate = certificates.iterator().next();
                    byte[] ticketExtensionValue = x509Certificate.getExtensionValue(ContextVS.TICKET_OID);
                    DERTaggedObject ticketCertDataDER = (DERTaggedObject)
                            X509ExtensionUtil.fromExtensionValue(ticketExtensionValue);
                    JSONObject ticketCertData = new JSONObject(((DERUTF8String)
                            ticketCertDataDER.getObject()).toString());
                    String hashCertVS = ticketCertData.getString("hashCertVS");
                    TicketVS ticket = ticketsMap.get(hashCertVS);
                    ticket.getCertificationRequest().initSigner(issuedTicketsArray.getString(i).getBytes());
                }
                contextVS.updateTickets(ticketsMap.values());
                caption = getString(R.string.ticket_request_ok_caption);
                message = getString(R.string.ticket_request_ok_msg, withdrawalAmount.toString(),
                        CurrencyVS.Euro.toString());
                iconId = R.drawable.euro_24;
            } else {
                caption = getString(R.string.ticket_request_error_caption);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            message = ex.getMessage();
            if(message == null || message.isEmpty()) message = contextVS.getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(contextVS.getString(R.string.exception_lbl),
                    message);
        } finally {
            responseVS.setIconId(iconId);
            responseVS.setCaption(caption);
            responseVS.setNotificationMessage(message);
            return responseVS;
        }
    }

    private ResponseVS updateUserInfo(String pin) {
        ResponseVS responseVS = null;
        Map mapToSend = new HashMap();
        mapToSend.put("NIF", contextVS.getUserVS().getNif());
        mapToSend.put("operation", TypeVS.TICKET_USER_INFO.toString());
        mapToSend.put("UUID", UUID.randomUUID().toString());
        String msgSubject = getString(R.string.ticket_user_info_request_msg_subject);
        try {
            JSONObject userInfoRequestJSON = new JSONObject(mapToSend);
            TicketServer ticketServer = contextVS.getTicketServer();
            if(ticketServer == null) {
                responseVS = initTicketServer();
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    ticketServer = contextVS.getTicketServer();
                } else return responseVS;
            }
            SMIMESignedSender smimeSignedSender = new SMIMESignedSender(contextVS.getUserVS().getNif(),
                    ticketServer.getNameNormalized(), ticketServer.getUserInfoServiceURL(),
                    userInfoRequestJSON.toString(), ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED,
                    msgSubject, pin.toCharArray(), ticketServer.getCertificate(), contextVS);
            responseVS = smimeSignedSender.call();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                String responseStr = responseVS.getMessage();
                TicketAccount ticketAccount = TicketAccount.parse(new JSONObject(responseStr));
                contextVS.setTicketAccount(ticketAccount);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            if(message == null || message.isEmpty()) message = contextVS.getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(contextVS.getString(R.string.exception_lbl),
                    message);
        } finally {
            return responseVS;
        }
    }

    private ResponseVS initTicketServer() {
        ResponseVS responseVS = null;
        TicketServer ticketServer = null;
        try {
            responseVS = HttpHelper.getData(ActorVS.getServerInfoURL(contextVS.getTicketServerURL()),
                    ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                ticketServer = (TicketServer) ActorVS.parse(new JSONObject(responseVS.getMessage()));
                contextVS.setTicketServer(ticketServer);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            if(message == null || message.isEmpty()) message = contextVS.getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(contextVS.getString(R.string.exception_lbl),
                    message);
        } finally {
            responseVS.setData(ticketServer);
            return responseVS;
        }
    }

    private void showNotification(ResponseVS responseVS){
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Intent clickIntent = new Intent(this, MessageActivity.class);
        clickIntent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, ContextVS.
                TICKET_SERVICE_NOTIFICATION_ID, clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(responseVS.getCaption()).setContentText(Html.fromHtml(
                responseVS.getNotificationMessage())).setSmallIcon(responseVS.getIconId())
                .setContentIntent(pendingIntent);
        Notification note = builder.build();
        // hide the notification after its selected
        note.flags |= Notification.FLAG_AUTO_CANCEL;
        //Identifies our service icon in the icon tray.
        notificationManager.notify(ContextVS.REPRESENTATIVE_SERVICE_NOTIFICATION_ID, note);
    }

    private void sendMessage(ResponseVS responseVS) {
        Log.d(TAG + ".sendMessage(...) ", "statusCode: " + responseVS.getStatusCode() +
                " - type: " + responseVS.getTypeVS() + " - serviceCaller: " +
                responseVS.getServiceCaller());
        Intent intent = new Intent(responseVS.getServiceCaller());
        intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
        intent.putExtra(ContextVS.TYPEVS_KEY, responseVS.getTypeVS());
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}