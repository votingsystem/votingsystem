package org.votingsystem.client.util;

import com.itextpdf.text.pdf.PdfReader;
import com.sun.javafx.application.PlatformImpl;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.callable.AnonymousDelegationRequestDataSender;
import org.votingsystem.callable.PDFSignedSender;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.ContentSignerHelper;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class SignatureService extends Service<ResponseVS> {

    private static Logger logger = Logger.getLogger(SignatureService.class);

    private static final Map<String, ActorVS> serverMap = new HashMap<String, ActorVS>();

    private OperationVS operationVS = null;
    private String password = null;

    @Override protected Task createTask() {
        return new SignatureTask();
    }

    public void processOperationVS(OperationVS operationVS) {
        this.operationVS = operationVS;
        PlatformImpl.runLater(new Runnable() {
            @Override
            public void run() {
                PasswordDialog passwordDialog = new PasswordDialog();
                passwordDialog.show(ContentSignerHelper.getPasswordRequestMsg());
                password = passwordDialog.getPassword();
                if (password != null) SignatureService.this.restart();
            }
        });
    }

    public OperationVS getOperationVS() {
        return operationVS;
    }

    class SignatureTask extends Task<ResponseVS> {

        @Override protected ResponseVS call() throws Exception {
            logger.debug("PreconditionsCheckerService.CheckTask - call:" + operationVS.getType());
            ResponseVS responseVS = null;
            ActorVS targetServer = null;
            updateProgress(5, 100);
            try {
                switch (operationVS.getType()) {
                    case SEND_SMIME_VOTE:
                        String accessControlURL = operationVS.getEventVS().getAccessControlVS().getServerURL();
                        responseVS = checkServer(accessControlURL);
                        if (ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
                        else ContextVS.getInstance().setAccessControl((AccessControlVS) responseVS.getData());
                        String controlCenterURL = operationVS.getEventVS().getControlCenterVS().getServerURL();
                        responseVS = checkServer(controlCenterURL);
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            ContextVS.getInstance().setControlCenter((ControlCenterVS) responseVS.getData());
                        }
                        break;
                    case VICKET_GROUP_SUBSCRIBE:
                        responseVS = checkServer(operationVS.getServerURL().trim());
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            ContextVS.getInstance().setVicketServer((VicketServer) responseVS.getData());
                        }
                        break;
                    default:
                        responseVS = checkServer(operationVS.getServerURL().trim());
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            if(operationVS.getType().toString().contains("VICKET_"))
                                ContextVS.getInstance().setVicketServer((VicketServer) responseVS.getData());
                            else ContextVS.getInstance().setAccessControl((AccessControlVS) responseVS.getData());
                        }
                }
                if(operationVS.getType() != null && operationVS.getType().toString().contains("VICKET_"))
                    targetServer =  ContextVS.getInstance().getVicketServer();
                else targetServer =  ContextVS.getInstance().getAccessControl();
                updateProgress(25, 100);
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    updateMessage(operationVS.getSignedMessageSubject());
                    updateProgress(40, 100);
                    switch(operationVS.getType()) {
                        case ANONYMOUS_REPRESENTATIVE_SELECTION:
                            responseVS = processAnonymousDelegation(targetServer, operationVS);
                            break;
                        case MANIFEST_PUBLISHING:
                            responseVS = publishManifest(targetServer, operationVS);
                            break;
                        case MANIFEST_SIGN:
                            responseVS = signManifest(operationVS);
                            break;
                        default:
                            responseVS = sendSMIME(targetServer, operationVS);
                    }
                    updateProgress(100, 100);
                    return responseVS;
                } else return responseVS;
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }


        private ResponseVS signManifest(OperationVS operationVS) throws Exception {
            ResponseVS responseVS = HttpHelper.getInstance().getData(operationVS.getDocumentURL(), ContentTypeVS.PDF);
            if (ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            byte[] pdfDocumentBytes = responseVS.getMessageBytes();
            PdfReader readerManifesto = new PdfReader(pdfDocumentBytes);
            String reason = null;
            String location = null;
            PDFSignedSender pdfSignedSender = new PDFSignedSender(operationVS.getServiceURL(),
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                    reason, location, password.toCharArray(), readerManifesto, null, null,
                    ContextVS.getInstance().getAccessControl().getX509Certificate());
            return pdfSignedSender.call();
        }

        //we know this is done in a background thread
        private ResponseVS publishManifest(ActorVS targetServer, OperationVS operationVS) throws Exception {
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(operationVS.getDocumentToSignMap());
            ResponseVS responseVS = HttpHelper.getInstance().sendData(jsonObject.toString().getBytes(),
                    ContentTypeVS.JSON, operationVS.getServiceURL(), "eventId");
            byte [] pdfDocumentBytes = null;
            updateProgress(60, 100);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                pdfDocumentBytes = responseVS.getMessageBytes();
                String eventId = ((List<String>)responseVS.getData()).iterator().next();
                String serviceURL = operationVS.getServiceURL() +  "/" + eventId;
                operationVS.setServiceURL(serviceURL);
            } else {
                return new ResponseVS(responseVS.getStatusCode(), ContextVS.getInstance().getMessage(
                        "errorDownloadingDocument") + " - " + responseVS.getMessage());
            }
            PdfReader readerManifesto = new PdfReader(pdfDocumentBytes);
            String reason = null;
            String location = null;
            PDFSignedSender pdfSignedSender = new PDFSignedSender(operationVS.getServiceURL(),
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                    reason, location, password.toCharArray(), readerManifesto, null, null,
                    ContextVS.getInstance().getAccessControl().getX509Certificate());
            responseVS = pdfSignedSender.call();
            return responseVS;
        }

        //we know this is done in a background thread
        private ResponseVS processAnonymousDelegation(ActorVS targetServer, OperationVS operationVS) throws Exception {
            String representativeName = (String) operationVS.getDocumentToSignMap().get("representativeName");
            String caption = operationVS.getCaption();
            if(caption.length() > 50) caption = caption.substring(0, 50) + "...";
            Map documentToSignMap = new HashMap();
            documentToSignMap.put("weeksOperationActive", operationVS.getDocumentToSignMap().get("weeksOperationActive"));
            documentToSignMap.put("UUID", UUID.randomUUID().toString());
            documentToSignMap.put("accessControlURL", ContextVS.getInstance().getAccessControl().getServerURL());
            documentToSignMap.put("operation", TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST);
            try {
                String fromUser = null;
                String toUser =  ContextVS.getInstance().getAccessControl().getNameNormalized();
                JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(documentToSignMap);
                SMIMEMessageWrapper smimeMessage = ContentSignerHelper.genMimeMessage(fromUser, toUser, jsonObject.toString(),
                        password.toCharArray(), operationVS.getSignedMessageSubject(), null);
                String originHashCertVS = UUID.randomUUID().toString();
                String hashCertVSBase64 = CMSUtils.getHashBase64(originHashCertVS, ContextVS.VOTING_DATA_DIGEST);
                String weeksOperationActive = (String)operationVS.getDocumentToSignMap().get("weeksOperationActive");
                AnonymousDelegationRequestDataSender anonymousDelegatorDataSender = new AnonymousDelegationRequestDataSender(
                        smimeMessage, weeksOperationActive, hashCertVSBase64);
                ResponseVS responseVS = anonymousDelegatorDataSender.call();
                CertificationRequestVS certificationRequest = null;
                updateProgress(60, 100);
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                    jsonObject = (JSONObject) JSONSerializer.toJSON(responseVS.getMessage());
                    responseVS.setMessage(jsonObject.getString("message"));
                    return responseVS;
                }
                else certificationRequest = (CertificationRequestVS) responseVS.getData();

                jsonObject = (JSONObject) JSONSerializer.toJSON(operationVS.getDocumentToSignMap());
                String textToSign = jsonObject.toString();
                fromUser = hashCertVSBase64;
                toUser = StringUtils.getNormalized(ContextVS.getInstance().getAccessControl().getName());
                smimeMessage = certificationRequest.genMimeMessage(fromUser, toUser, textToSign,
                        operationVS.getSignedMessageSubject(), null);
                String anonymousDelegationService = ContextVS.getInstance().getAccessControl().
                        getAnonymousDelegationServiceURL();
                SMIMESignedSender signedSender = new SMIMESignedSender(smimeMessage, anonymousDelegationService,
                        ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                        ContentTypeVS.SIGNED_AND_ENCRYPTED, certificationRequest.getKeyPair(),
                        ContextVS.getInstance().getAccessControl().getX509Certificate());
                responseVS = signedSender.call();
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    SMIMEMessageWrapper receipt = responseVS.getSmimeMessage();
                    Map receiptDataMap = (JSONObject) JSONSerializer.toJSON(receipt.getSignedContent());
                    responseVS = operationVS.validateReceiptDataMap(receiptDataMap);
                    Map delegationDataMap = new HashMap();
                    delegationDataMap.put(ContextVS.HASH_CERTVS_KEY, hashCertVSBase64);
                    delegationDataMap.put(ContextVS.ORIGIN_HASH_CERTVS_KEY, originHashCertVS);
                    ResponseVS hashCertVSData = new ResponseVS(ResponseVS.SC_OK);
                    hashCertVSData.setSmimeMessage(receipt);
                    hashCertVSData.setData(delegationDataMap);
                    hashCertVSData.setType(TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION);
                    ContextVS.getInstance().addHashCertVSData(hashCertVSBase64, hashCertVSData);
                }
                return responseVS;
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

        //we know this is done in a background thread
        private ResponseVS sendSMIME(ActorVS targetServer, OperationVS operationVS) throws Exception {
            JSONObject documentToSignJSON = (JSONObject) JSONSerializer.toJSON(operationVS.getDocumentToSignMap());
            SMIMEMessageWrapper smimeMessage = ContentSignerHelper.genMimeMessage(null,
                    operationVS.getNormalizedReceiverName(), documentToSignJSON.toString(),
                    password.toCharArray(), operationVS.getSignedMessageSubject(), null);
            SMIMESignedSender senderWorker = new SMIMESignedSender(smimeMessage, operationVS.getServiceURL(),
                    targetServer.getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED, null,
                    targetServer.getX509Certificate());
            return senderWorker.call();
        }

        //we know this is done in a background thread
        private ResponseVS<ActorVS> checkServer(String serverURL) throws Exception {
            logger.debug(" - checkServer: " + serverURL);
            ActorVS actorVS = serverMap.get(serverURL.trim());
            if (actorVS == null) {
                String serverInfoURL = ActorVS.getServerInfoURL(serverURL);
                ResponseVS responseVS = HttpHelper.getInstance().getData(serverInfoURL, ContentTypeVS.JSON);
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(responseVS.getMessage());
                    actorVS = ActorVS.populate(jsonObject);
                    responseVS.setData(actorVS);
                    logger.error("checkServer - adding " + serverURL.trim() + " to sever map");
                    serverMap.put(serverURL.trim(), actorVS);
                    switch (actorVS.getType()) {
                        case ACCESS_CONTROL:
                            ContextVS.getInstance().setAccessControl((AccessControlVS) actorVS);
                            break;
                        case VICKETS:
                            ContextVS.getInstance().setVicketServer((VicketServer) actorVS);
                            ContextVS.getInstance().setTimeStampServerCert(actorVS.getTimeStampCert());
                            break;
                        case CONTROL_CENTER:
                            ContextVS.getInstance().setControlCenter((ControlCenterVS) actorVS);
                            break;
                        default:
                            logger.debug("Unprocessed actor:" + actorVS.getType());
                    }
                } else if (ResponseVS.SC_NOT_FOUND == responseVS.getStatusCode()) {
                    responseVS.setMessage(ContextVS.getMessage("serverNotFoundMsg", serverURL.trim()));
                }
                return responseVS;
            } else {
                ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK);
                responseVS.setData(actorVS);
                return responseVS;
            }
        }
    }
}
