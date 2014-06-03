package org.votingsystem.client.util;

import com.itextpdf.text.pdf.PdfReader;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.votingsystem.callable.*;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CMSUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.ContentSignerHelper;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.MessageDigest;
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

    public void processOperationVS(String password, OperationVS operationVS) {
        this.operationVS = operationVS;
        this.password = password;
        restart();
    }

    public OperationVS getOperationVS() {
        return operationVS;
    }

    class SignatureTask extends Task<ResponseVS> {

        @Override protected ResponseVS call() throws Exception {
            logger.debug("SignatureService.SignatureTask - call:" + operationVS.getType());
            ResponseVS responseVS = null;
            updateProgress(5, 100);
            try {
                switch (operationVS.getType()) {
                    case SEND_SMIME_VOTE:
                        String accessControlURL = operationVS.getEventVS().getAccessControlVS().getServerURL();
                        responseVS = checkServer(accessControlURL);
                        if (ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
                        else ContextVS.getInstance().setServer((AccessControlVS) responseVS.getData());
                        operationVS.setTargetServer((AccessControlVS) responseVS.getData());
                        String controlCenterURL = operationVS.getEventVS().getControlCenterVS().getServerURL();
                        responseVS = checkServer(controlCenterURL);
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            ContextVS.getInstance().setControlCenter((ControlCenterVS) responseVS.getData());
                        }
                        break;
                    default:
                        responseVS = checkServer(operationVS.getServerURL().trim());
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            ContextVS.getInstance().setServer((ActorVS) responseVS.getData());
                            operationVS.setTargetServer((ActorVS) responseVS.getData());
                        }
                }
                updateProgress(25, 100);
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    updateMessage(operationVS.getSignedMessageSubject());
                    updateProgress(40, 100);
                    switch(operationVS.getType()) {
                        case SEND_SMIME_VOTE:
                            responseVS = sendVote(operationVS);
                            break;
                        case NEW_REPRESENTATIVE:
                            responseVS = processNewRepresentative(operationVS);
                            break;
                        case ANONYMOUS_REPRESENTATIVE_SELECTION:
                            responseVS = processAnonymousDelegation(operationVS.getTargetServer(), operationVS);
                            break;
                        case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED:
                            responseVS = processCancelAnonymousDelegation(operationVS.getTargetServer(), operationVS);
                            break;
                        case MANIFEST_PUBLISHING:
                            responseVS = publishManifest(operationVS.getTargetServer(), operationVS);
                            break;
                        case MANIFEST_SIGN:
                            responseVS = signManifest(operationVS);
                            break;
                        case CLAIM_PUBLISHING:
                        case VOTING_PUBLISHING:
                            responseVS = publishSMIME(operationVS.getTargetServer(), operationVS);
                            break;
                        default:
                            responseVS = sendSMIME(operationVS.getTargetServer(), operationVS);
                    }
                    updateProgress(100, 100);
                    return responseVS;
                } else return responseVS;
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

        //we know this is done in a background thread
        private ResponseVS sendVote(OperationVS operationVS) throws Exception {
            logger.debug("sendVote");
            String fromUser = ContextVS.getInstance().getMessage("electorLbl");
            EventVS eventVS = operationVS.getEventVS();
            eventVS.getVoteVS().genVote();
            String toUser =  eventVS.getAccessControlVS().getNameNormalized();
            String msgSubject = ContextVS.getInstance().getMessage("accessRequestMsgSubject")  + eventVS.getId();
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(eventVS.getVoteVS().getAccessRequestDataMap());
            SMIMEMessageWrapper smimeMessage = ContentSignerHelper.genMimeMessage(fromUser, toUser, jsonObject.toString(),
                    password.toCharArray(), operationVS.getSignedMessageSubject(), null);
            //No se hace la comprobación antes porque no hay usuario en contexto
            //hasta que no se firma al menos una vez
            eventVS.setUserVS(ContextVS.getInstance().getSessionUser());
            AccessRequestDataSender accessRequestDataSender = new AccessRequestDataSender(
                    smimeMessage, eventVS.getVoteVS());
            ResponseVS responseVS = accessRequestDataSender.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                return responseVS;
            }
            updateProgress(60, 100);
            CertificationRequestVS certificationRequest = (CertificationRequestVS) responseVS.getData();
            jsonObject = (JSONObject) JSONSerializer.toJSON(eventVS.getVoteVS().getVoteDataMap());
            String textToSign = jsonObject.toString();
            fromUser = eventVS.getVoteVS().getHashCertVSBase64();
            toUser = StringUtils.getNormalized(eventVS.getControlCenterVS().getName());
            msgSubject = ContextVS.getInstance().getMessage("voteVSSubject");
            smimeMessage = certificationRequest.genMimeMessage(fromUser, toUser, textToSign, msgSubject, null);
            String urlVoteService = ((ControlCenterVS)ContextVS.getInstance().getControlCenter()).getVoteServiceURL();
            updateProgress(70, 100);
            SMIMESignedSender signedSender = new SMIMESignedSender(smimeMessage, urlVoteService,
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                    ContentTypeVS.VOTE, certificationRequest.getKeyPair(), ContextVS.getInstance().getControlCenter().
                    getX509Certificate(), "voteURL");
            responseVS = signedSender.call();
            updateProgress(90, 100);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessageWrapper validatedVote = responseVS.getSmimeMessage();
                Map validatedVoteDataMap = (JSONObject) JSONSerializer.toJSON(validatedVote.getSignedContent());
                eventVS.getVoteVS().setReceipt(validatedVote);
                ResponseVS voteResponse = new ResponseVS(ResponseVS.SC_OK);
                voteResponse.setType(TypeVS.VOTEVS);
                voteResponse.setData(eventVS.getVoteVS());
                ContextVS.getInstance().addHashCertVSData(eventVS.getVoteVS().getHashCertVSBase64(), voteResponse);
                //voteURL header
                responseVS.setMessage(((List<String>)responseVS.getData()).iterator().next());
            }
            return responseVS;
        }

        //we know this is done in a background thread
        private ResponseVS processNewRepresentative(OperationVS operationVS) throws Exception {
            byte[] imageFileBytes = FileUtils.getBytesFromFile(operationVS.getFile());
            logger.debug(" - imageFileBytes.length: " + imageFileBytes.length);
            if(imageFileBytes.length > ContextVS.IMAGE_MAX_FILE_SIZE) {
                logger.debug(" - MAX_FILE_SIZE exceeded ");
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("fileSizeExceeded",
                        ContextVS.IMAGE_MAX_FILE_SIZE_KB));
            } else {
                MessageDigest messageDigest = MessageDigest.getInstance(ContextVS.VOTING_DATA_DIGEST);
                byte[] resultDigest =  messageDigest.digest(imageFileBytes);
                String base64ResultDigest = new String(Base64.encode(resultDigest));
                operationVS.getDocumentToSignMap().put("base64ImageHash", base64ResultDigest);
                //String base64RepresentativeEncodedImage = new String(Base64.encode(imageFileBytes));
                //operation.getContentFirma().put("base64RepresentativeEncodedImage", base64RepresentativeEncodedImage);
                JSONObject documentToSignJSON = (JSONObject)JSONSerializer.toJSON(operationVS.getDocumentToSignMap());
                SMIMEMessageWrapper representativeRequestSMIME = ContentSignerHelper.genMimeMessage(null,
                        operationVS.getNormalizedReceiverName(), documentToSignJSON.toString(),
                        password.toCharArray(), operationVS.getSignedMessageSubject(), null);
                RepresentativeDataSender dataSender = new RepresentativeDataSender(representativeRequestSMIME,
                        operationVS.getFile(), operationVS.getServiceURL());
                ResponseVS responseVS = dataSender.call();
                return responseVS;
            }
        }

        //we know this is done in a background thread
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
            PdfReader readerManifest = new PdfReader(pdfDocumentBytes);

            String reason = null;
            String location = null;
            PDFSignedSender pdfSignedSender = new PDFSignedSender(operationVS.getServiceURL(),
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                    reason, location, password.toCharArray(), readerManifest, null, null,
                    ContextVS.getInstance().getAccessControl().getX509Certificate());
            responseVS = pdfSignedSender.call();
            return responseVS;
        }

        //we know this is done in a background thread
        private ResponseVS processCancelAnonymousDelegation(ActorVS targetServer, OperationVS operationVS) throws Exception {
            String outputFolder = ContextVS.APPTEMPDIR + File.separator + UUID.randomUUID();
            FileUtils.unpackZip(operationVS.getFile(), new File(outputFolder));
            File cancelDataFile = new File(outputFolder + File.separator + ContextVS.CANCEL_DATA_FILE_NAME);
            if(cancelDataFile.exists()) {
                String jsonDataStr = FileUtils.getStringFromFile(cancelDataFile);
                JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(jsonDataStr);
                if(jsonObject.containsKey(ContextVS.ORIGIN_HASH_CERTVS_KEY)) {
                    jsonObject.put("operation", TypeVS.ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELLED);
                    jsonObject.put("UUID", UUID.randomUUID().toString());
                    String documentToSignStr = jsonObject.toString();
                    SMIMEMessageWrapper smimeMessage = ContentSignerHelper.genMimeMessage(null,
                            operationVS.getNormalizedReceiverName(), documentToSignStr,
                            password.toCharArray(), operationVS.getSignedMessageSubject(), null);
                    SMIMESignedSender senderWorker = new SMIMESignedSender(smimeMessage, operationVS.getServiceURL(),
                            targetServer.getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED, null,
                            targetServer.getX509Certificate());
                    ResponseVS responseVS = senderWorker.call();
                    if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        if(responseVS.getContentType() != null && responseVS.getContentType().isSigned()) {
                            SMIMEMessageWrapper smimeMessageResp = responseVS.getSmimeMessage();
                            if(smimeMessageResp == null) {
                                ByteArrayInputStream bais = new ByteArrayInputStream(responseVS.getMessageBytes());
                                smimeMessageResp = new SMIMEMessageWrapper(bais);
                            }
                            jsonObject = (JSONObject)JSONSerializer.toJSON(smimeMessageResp.getSignedContent());
                            return new ResponseVS(responseVS.getStatusCode(), jsonObject.getString("message"));
                        }
                    }
                }
            }
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("errorLbl"));
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
                    responseVS.setMessage(hashCertVSBase64);
                }
                return responseVS;
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

        //we know this is done in a background thread
        private ResponseVS sendSMIME(ActorVS targetServer, OperationVS operationVS, String... header) throws Exception {
            logger.debug("sendSMIME");
            JSONObject documentToSignJSON = (JSONObject) JSONSerializer.toJSON(operationVS.getDocumentToSignMap());
            SMIMEMessageWrapper smimeMessage = ContentSignerHelper.genMimeMessage(null,
                    operationVS.getNormalizedReceiverName(), documentToSignJSON.toString(),
                    password.toCharArray(), operationVS.getSignedMessageSubject(), null);
            SMIMESignedSender senderWorker = new SMIMESignedSender(smimeMessage, operationVS.getServiceURL(),
                    targetServer.getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED, null,
                    targetServer.getX509Certificate(), header);
            return senderWorker.call();
        }


        //we know this is done in a background thread
        private ResponseVS<ActorVS> publishSMIME(ActorVS targetServer, OperationVS operationVS) throws Exception {
            logger.debug("publishPoll");
            ResponseVS responseVS = sendSMIME(targetServer, operationVS, "eventURL");
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                String eventURL = ((List<String>)responseVS.getData()).iterator().next() +"?menu=admin";
                operationVS.setDocumentURL(eventURL);
                //String receipt = responseVS.getMessage();
                responseVS.setMessage(eventURL);
            }
            return responseVS;
        }
    }

    //we know this is done in a background thread
    public static ResponseVS<ActorVS> checkServer(String serverURL) throws Exception {
        logger.debug(" - checkServer: " + serverURL);
        ActorVS actorVS = serverMap.get(serverURL.trim());
        if (actorVS == null) {
            String serverInfoURL = ActorVS.getServerInfoURL(serverURL);
            ResponseVS responseVS = HttpHelper.getInstance().getData(serverInfoURL, ContentTypeVS.JSON);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                actorVS = ActorVS.populate(responseVS.getJSONMessage());
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
