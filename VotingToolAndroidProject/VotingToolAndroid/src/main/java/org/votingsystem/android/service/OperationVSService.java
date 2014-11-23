package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import org.bouncycastle2.util.encoders.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.callable.MessageTimeStamper;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class OperationVSService extends IntentService {

    public static final String TAG = OperationVSService.class.getSimpleName();

    public OperationVSService() { super(TAG); }

    private AppContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {
        final Bundle arguments = intent.getExtras();
        contextVS = (AppContextVS) getApplicationContext();
        String serviceCaller = arguments.getString(ContextVS.CALLER_KEY);
        OperationVS operationVS = (OperationVS)arguments.getParcelable(ContextVS.OPERATIONVS_KEY);
        if(operationVS != null) {
            switch(operationVS.getTypeVS()) {
                case MESSAGEVS:
                    sendMessageVS(operationVS, serviceCaller);
                    break;
                default:
                    processOperation(operationVS, serviceCaller);
            }
            return;
        }
    }

    private void processOperation(OperationVS operationVS, String serviceCaller) {
        LOGD(TAG + ".processOperation", "operation: " + operationVS.getTypeVS() +
                " - serviceCaller: " + serviceCaller);
        ActorVS targetServer = contextVS.getActorVS(operationVS.getServerURL());
        ResponseVS responseVS = null;
        if(targetServer == null) {
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, contextVS.getString(
                    R.string.connection_error_msg)).setIconId(R.drawable.fa_times_32);
        } else {
            try {
                responseVS = contextVS.signMessage(operationVS.getNormalizedReceiverName(),
                        operationVS.getDocumentToSignJSON().toString(), operationVS.getSignedMessageSubject());
                responseVS = HttpHelper.sendData(responseVS.getSMIME().getBytes(), ContentTypeVS.JSON_SIGNED,
                        operationVS.getServiceURL());
            } catch (Exception ex) {
                ex.printStackTrace();
                responseVS = ResponseVS.getExceptionResponse(ex, this);
            }
        }
        responseVS.setTypeVS(operationVS.getTypeVS()).setServiceCaller(serviceCaller);
        responseVS.setOperation(operationVS);
        contextVS.showNotification(responseVS);
        contextVS.broadcastResponse(responseVS);
    }

    private void sendMessageVS(OperationVS operationVS, String serviceCaller){
        LOGD(TAG + ".sendMessageVS", "operationVS: " + operationVS.getTypeVS());
        //operationVS.getContentType(); -> MessageVS
        ResponseVS responseVS = null;
        ActorVS targetServer = contextVS.getActorVS(operationVS.getServerURL());
        List signedDataList = new ArrayList();
        List encryptedDataList = new ArrayList();
        try {
            JSONArray targetCertArray = operationVS.getTargetCertArray();
            for(int i = 0; i < targetCertArray.length(); i++) {
                JSONObject targetCertItem = targetCertArray.getJSONObject(i);
                X509Certificate receiverCert = CertUtils.fromPEMToX509Cert(
                        (targetCertItem.getString("pemCert")).getBytes());
                JSONObject documentToEncrypt = operationVS.getDocumentToEncrypt();
                byte[] encryptedBytes = Encryptor.encryptToCMS(
                        documentToEncrypt.toString().getBytes(), receiverCert);
                String encryptedMessageStr = new String(encryptedBytes, "UTF-8");
                String encryptedMessageHash = CMSUtils.getHashBase64(encryptedMessageStr,
                        ContextVS.VOTING_DATA_DIGEST);
                Map signedMap = new HashMap();
                signedMap.put("serialNumber", targetCertItem.getString("serialNumber"));
                signedMap.put("encryptedMessageHashBase64", encryptedMessageHash);
                signedDataList.add(signedMap);

                Map encryptedMap = new HashMap();
                encryptedMap.put("serialNumber", targetCertItem.getString("serialNumber"));
                encryptedMap.put("encryptedData", encryptedMessageStr);
                encryptedDataList.add(encryptedMap);
            }
            operationVS.getDocumentToSignJSON().put("encryptedDataInfo", new JSONArray(signedDataList));
            responseVS = contextVS.signMessage(targetServer.getNameNormalized(),
                    operationVS.getDocumentToSignJSON().toString(), operationVS.getSignedMessageSubject(),
                    targetServer.getTimeStampServiceURL());
            SMIMEMessage smimeMessage = responseVS.getSMIME();
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage,
                    targetServer.getTimeStampServiceURL(), contextVS);
            responseVS = timeStamper.call();
            smimeMessage = timeStamper.getSMIME();
            String smimeMessageBase64 = new String(Base64.encode(smimeMessage.getBytes()));
            operationVS.getDocumentToSignJSON().put("smimeMessage", smimeMessageBase64);
            operationVS.getDocumentToSignJSON().put("encryptedDataList", new JSONArray(encryptedDataList));
            responseVS = HttpHelper.sendData(operationVS.getDocumentToSignJSON().toString().getBytes(),
                    ContentTypeVS.MESSAGEVS, operationVS.getServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) responseVS =
                    new ResponseVS(ResponseVS.SC_OK, getString(R.string.messagevs_send_ok_msg));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        } finally {
            responseVS.setOperation(operationVS);
            responseVS.setTypeVS(operationVS.getTypeVS()).setServiceCaller(serviceCaller);
            contextVS.broadcastResponse(responseVS);
        }
    }

}