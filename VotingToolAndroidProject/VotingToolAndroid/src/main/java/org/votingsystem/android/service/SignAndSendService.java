package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.callable.PDFSignedSender;
import org.votingsystem.android.callable.SMIMESignedSender;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.HttpHelper;

import java.util.List;

import javax.mail.internet.ContentType;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SignAndSendService extends IntentService {

    public static final String TAG = SignAndSendService.class.getSimpleName();

    public SignAndSendService() { super(TAG); }

    private AppContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        contextVS = (AppContextVS) getApplicationContext();
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        TypeVS operationType = (TypeVS) intent.getSerializableExtra(ContextVS.TYPEVS_KEY);
        OperationVS operationVS = (OperationVS)arguments.getSerializable(ContextVS.OPERATIONVS_KEY);
        if(operationVS != null) {
            processOperation(operationVS, serviceCaller);
            return;
        }
        ResponseVS responseVS = null;
        int resultIcon = R.drawable.fa_times_32;
        try {
            Long eventId = arguments.getLong(ContextVS.ITEM_ID_KEY);
            String signatureContent = arguments.getString(ContextVS.MESSAGE_KEY);
            String messageSubject = arguments.getString(ContextVS.MESSAGE_SUBJECT_KEY);
            String serviceURL = arguments.getString(ContextVS.URL_KEY);
            String toUser = arguments.getString(ContextVS.RECEIVER_KEY);
            if(toUser == null) toUser = contextVS.getAccessControl().
                    getNameNormalized();
            ContentTypeVS contentType = (ContentTypeVS)intent.getSerializableExtra(
                    ContextVS.CONTENT_TYPE_KEY);
            String caption = null;
            byte[] pdfBytes = null;
            String notificationMessage = null;
            Log.d(TAG + ".onHandleIntent(...) ", "operationType: " + operationType +
                    " - contentType: " + contentType);
            switch(operationType) {
                case MANIFEST_PUBLISHING:
                    //Get the PDF to sign
                    responseVS = HttpHelper.sendData(signatureContent.getBytes(), null,
                            serviceURL, "eventId");
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        String manifestId = ((List<String>)responseVS.getData()).iterator().next();
                        pdfBytes = responseVS.getMessageBytes();
                        serviceURL = serviceURL + "/" + manifestId;
                        PDFSignedSender pdfSignedSender = new PDFSignedSender(pdfBytes, serviceURL,
                                null, null,
                                (AppContextVS)getApplicationContext());
                        responseVS = pdfSignedSender.call();
                    }
                    break;
                case MANIFEST_EVENT:
                    //Get the PDF to sign
                    responseVS = HttpHelper.getData(contextVS.getAccessControl().
                            getEventVSManifestURL(eventId), ContentTypeVS.PDF);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        pdfBytes = responseVS.getMessageBytes();
                        PDFSignedSender pdfSignedSender = new PDFSignedSender(pdfBytes,
                                contextVS.getAccessControl().getEventVSManifestCollectorURL(eventId),
                                null, null, (AppContextVS)getApplicationContext());
                        responseVS = pdfSignedSender.call();
                    }
                    break;
                case VOTING_PUBLISHING:
                case CLAIM_PUBLISHING:
                case CONTROL_CENTER_ASSOCIATION:
                case SMIME_CLAIM_SIGNATURE:
                case CLAIM_EVENT:
                case REPRESENTATIVE_REVOKE:
                case REPRESENTATIVE_SELECTION:
                case ANONYMOUS_REPRESENTATIVE_SELECTION:
                    SMIMESignedSender smimeSignedSender = new SMIMESignedSender(
                            contextVS.getUserVS().getNif(), toUser, serviceURL, signatureContent,
                            contentType, messageSubject, contextVS.getAccessControl().getCertificate(),
                            (AppContextVS)getApplicationContext());
                    responseVS = smimeSignedSender.call();
                    break;
                default:
                    responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, getString(
                            R.string.operation_unknown_error_msg, operationType.toString()));
                    break;
            }
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                switch(operationType) {
                    case CLAIM_PUBLISHING:
                        notificationMessage = getString(R.string.claim_published_ok_msg);
                        break;
                    case VOTING_PUBLISHING:
                        notificationMessage = getString(R.string.election_published_ok_msg);
                        break;
                    case REPRESENTATIVE_REVOKE:
                        notificationMessage = getString(R.string.representative_revoke_ok_msg);
                        break;
                    default:
                        notificationMessage = getString(R.string.signature_ok_notification_msg);
                }
                caption = getString(R.string.operation_ok_msg);
                resultIcon = R.drawable.signature_ok_32;
            } else {
                caption = getString(R.string.signature_error_notification_msg);
                notificationMessage = responseVS.getMessage();
                if(ContentTypeVS.JSON == responseVS.getContentType()) {
                    try {
                        JSONObject responseJSON = new JSONObject(responseVS.getNotificationMessage());
                        notificationMessage = responseJSON.getString("message");
                        responseVS.setData(responseJSON.getString("URL"));
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            responseVS.setCaption(caption);
            responseVS.setNotificationMessage(notificationMessage);
        } catch(Exception ex) {
            ex.printStackTrace();
            String message = ex.getMessage();
            if(message == null || message.isEmpty()) message = contextVS.getString(R.string.exception_lbl);
            responseVS = ResponseVS.getExceptionResponse(contextVS.getString(R.string.exception_lbl),
                    message);
        } finally {
            responseVS.setIconId(resultIcon);
            responseVS.setTypeVS(operationType);
            responseVS.setServiceCaller(serviceCaller);
            contextVS.showNotification(responseVS);
            contextVS.sendBroadcast(responseVS);
        }
    }

    private void processOperation(OperationVS operation, String serviceCaller) {
        Log.d(TAG + ".processOperation(...) ", "operation" + operation.getTypeVS() + " - serviceCaller: " +
                serviceCaller);
        ActorVS targetServer = getActorVS(operation.getServerURL());
        ResponseVS responseVS = null;
        if(targetServer == null) {
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, contextVS.getString(
                    R.string.connection_error_msg));
            responseVS.setIconId(R.drawable.fa_times_32);
        } else {
            responseVS = sendSMIME(targetServer, operation);
        }
        responseVS.setTypeVS(operation.getTypeVS());
        responseVS.setServiceCaller(serviceCaller);
        responseVS.setOperation(operation);
        contextVS.showNotification(responseVS);
        contextVS.sendBroadcast(responseVS);
    }


    private ResponseVS sendSMIME(ActorVS targetServer, OperationVS operationVS, String... header) {
        Log.d(TAG + ".sendSMIME(...) ", "sendSMIME");
        String toUser = operationVS.getNormalizedReceiverName();
        String serviceURL = operationVS.getServiceURL();
        ContentTypeVS contentType = ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED;
        String messageSubject = operationVS.getSignedMessageSubject();
        String signatureContent = operationVS.getSignedContent().toString();
        SMIMESignedSender smimeSignedSender = new SMIMESignedSender(
                contextVS.getUserVS().getNif(), toUser, serviceURL, signatureContent,
                contentType, messageSubject, targetServer.getCertificate(),
                (AppContextVS)getApplicationContext());
        ResponseVS responseVS = smimeSignedSender.call();
        if(ResponseVS.SC_ERROR == responseVS.getStatusCode()) {
            responseVS.setIconId(R.drawable.fa_times_32);
        } else responseVS.setIconId(R.drawable.fa_check_32);
        return responseVS;
    }

    public ActorVS getActorVS(String serverURL) {
        ActorVS targetServer = contextVS.getServer(serverURL);
        if(targetServer == null) {
            try {
                ResponseVS responseVS = HttpHelper.getData(ActorVS.getServerInfoURL(serverURL), ContentTypeVS.JSON);
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    targetServer = ActorVS.parse(new JSONObject(responseVS.getMessage()));
                    contextVS.setServer(targetServer);
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return targetServer;
    }

}