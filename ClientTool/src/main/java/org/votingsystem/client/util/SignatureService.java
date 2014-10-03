package org.votingsystem.client.util;

import com.itextpdf.text.pdf.PdfReader;
import com.sun.javafx.application.PlatformImpl;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.votingsystem.callable.*;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.*;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
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
                    case MESSAGEVS_DECRYPT:
                        responseVS = new ResponseVS(ResponseVS.SC_OK);
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
                        case CANCEL_VOTE:
                            responseVS = cancelVote(operationVS.getTargetServer(), operationVS);
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
                        case MESSAGEVS:
                            responseVS = sendMessageVS(operationVS.getTargetServer(), operationVS);
                            break;
                        case MESSAGEVS_DECRYPT:
                            responseVS = decryptMessageVS(operationVS);
                            break;
                        case OPEN_RECEIPT_FROM_URL:
                            responseVS = openReceiptFromURL(operationVS);
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

        private ResponseVS openReceiptFromURL(final OperationVS operationVS) throws Exception {
            ResponseVS responseVS = null;
            if(VotingSystemApp.getInstance().getSmimeMessage(operationVS.getServiceURL()) != null) {
                responseVS = new ResponseVS(ResponseVS.SC_OK,
                        VotingSystemApp.getInstance().getSmimeMessage(operationVS.getServiceURL()));
            } else {
                responseVS = HttpHelper.getInstance().getData(operationVS.getServiceURL(), ContentTypeVS.TEXT);
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    VotingSystemApp.getInstance().setSmimeMessage(operationVS.getServiceURL(),
                            responseVS.getMessage());
                }
            }
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_INITIALIZED);
                operationVS.setMessage(responseVS.getMessage());
                PlatformImpl.runLater(new Runnable() {
                    @Override public void run() {
                        SignedDocumentsBrowser.showDialog(operationVS.getMessage(), operationVS.getDocument());
                    }
                });
            }
            return responseVS;
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
                    password.toCharArray(), msgSubject, null);
            //No se hace la comprobación antes porque no hay usuario en contexto
            //hasta que no se firma al menos una vez
            eventVS.setUserVS(ContextVS.getInstance().getSessionUser());
            AccessRequestDataSender accessRequestDataSender = new AccessRequestDataSender(smimeMessage, eventVS.getVoteVS());
            ResponseVS responseVS = accessRequestDataSender.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
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
                ResponseVS voteResponse = new ResponseVS(ResponseVS.SC_OK, ContentTypeVS.VOTE);
                voteResponse.setType(TypeVS.VOTEVS);
                voteResponse.setData(eventVS.getVoteVS());
                ContextVS.getInstance().addHashCertVSData(eventVS.getVoteVS().getHashCertVSBase64(), voteResponse);
                //voteURL header
                JSONObject responseJSON = new JSONObject();
                responseJSON.put("statusCode", ResponseVS.SC_OK);
                responseJSON.put("voteURL", ((List<String>)responseVS.getData()).iterator().next());
                responseJSON.put("hashCertVSBase64", eventVS.getVoteVS().getHashCertVSBase64());
                responseJSON.put("hashCertVSHex", new String(Hex.encode(eventVS.getVoteVS().getHashCertVSBase64().getBytes())));
                responseJSON.put("voteVSReceipt", new String(validatedVote.getBytes(), "UTF-8"));
                //HexBinaryAdapter hexConverter = new HexBinaryAdapter();
                //String hashCertVSBase64 = new String(hexConverter.unmarshal(hashCertVSHex));
                responseVS.setContentType(ContentTypeVS.JSON);
                responseVS.setMessageJSON(responseJSON);
            }
            return responseVS;
        }

        //we know this is done in a background thread
        private ResponseVS cancelVote(ActorVS targetServer, OperationVS operationVS) throws Exception {
            logger.debug("cancelVote");
            Map documentToSignMap = new HashMap<String, String>();
            documentToSignMap.put("operation", TypeVS.CANCEL_VOTE.toString());
            ResponseVS voteResponse = ContextVS.getInstance().getHashCertVSData(operationVS.getMessage());
            VoteVS voteVS = (VoteVS) voteResponse.getData();
            documentToSignMap.put("originHashAccessRequest", voteVS.getOriginHashAccessRequest());
            documentToSignMap.put("hashAccessRequestBase64", voteVS.getAccessRequestHashBase64());
            documentToSignMap.put("originHashCertVote", voteVS.getOriginHashCertVote());
            documentToSignMap.put("hashCertVSBase64", voteVS.getHashCertVSBase64());
            documentToSignMap.put("UUID", UUID.randomUUID().toString());
            operationVS.setDocumentToSignMap(documentToSignMap);
            return sendSMIME(targetServer, operationVS);
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
        private ResponseVS sendMessageVS(ActorVS targetServer, OperationVS operationVS) throws Exception {
            logger.debug("sendMessageVS");
            //operationVS.getContentType(); -> MessageVS
            List signedDataList = new ArrayList<>();
            List encryptedDataList = new ArrayList<>();
            List<Map> targetCertList = operationVS.getTargetCertList();
            for(Map receiverCertDataMap : targetCertList) {
                X509Certificate receiverCert = CertUtil.fromPEMToX509Cert(((String) receiverCertDataMap.get("pemCert")).getBytes());
                JSONObject documentToEncrypt = (JSONObject) JSONSerializer.toJSON(operationVS.getDocumentToEncrypt());
                ResponseVS responseVS = Encryptor.encryptToCMS(documentToEncrypt.toString().getBytes(), receiverCert);
                String encryptedMessageStr = new String(responseVS.getMessageBytes(), "UTF-8");
                String encryptedMessageHash = CMSUtils.getHashBase64(encryptedMessageStr, ContextVS.VOTING_DATA_DIGEST);
                Map signedMap = new HashMap<>();
                signedMap.put("serialNumber", receiverCertDataMap.get("serialNumber"));
                signedMap.put("encryptedMessageHashBase64", encryptedMessageHash);
                signedDataList.add(signedMap);

                Map encryptedMap = new HashMap<>();
                encryptedMap.put("serialNumber", receiverCertDataMap.get("serialNumber"));
                encryptedMap.put("encryptedData", encryptedMessageStr);
                encryptedDataList.add(encryptedMap);
            }
            operationVS.getDocumentToSignMap().put("encryptedDataInfo", signedDataList);
            JSONObject documentToSignJSON = (JSONObject) JSONSerializer.toJSON(operationVS.getDocumentToSignMap());
            SMIMEMessageWrapper smimeMessage = ContentSignerHelper.genMimeMessage(null, targetServer.getNameNormalized(),
                    documentToSignJSON.toString(), password.toCharArray(), operationVS.getSignedMessageSubject(), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, targetServer.getTimeStampServiceURL());
            ResponseVS responseVS = timeStamper.call();
            smimeMessage = timeStamper.getSmimeMessage();
            try {
                String base64ResultDigest = new String(Base64.encode(smimeMessage.getBytes()));
                operationVS.getDocumentToSignMap().put("smimeMessage", base64ResultDigest);
                operationVS.getDocumentToSignMap().put("encryptedDataList", encryptedDataList);
                JSONObject documentToSendJSON = (JSONObject) JSONSerializer.toJSON(operationVS.getDocumentToSignMap());
                responseVS = HttpHelper.getInstance().sendData(documentToSendJSON.toString().getBytes(),
                        ContentTypeVS.MESSAGEVS, operationVS.getServiceURL());
            } catch (Exception ex) {
                logger.debug(ex.getMessage(), ex);
                responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            } finally {
                return responseVS;
            }
        }

        private ResponseVS decryptMessageVS(OperationVS operationVS) throws Exception {
            logger.debug("decryptMessageVS");
            Map documentToDecrypt = operationVS.getDocumentToDecrypt();
            List<Map> encryptedDataList = (List) documentToDecrypt.get("encryptedDataList");
            X509Certificate cryptoTokenCert = null;
            PrivateKey privateKey = null;
            try{
                KeyStore keyStore = ContextVS.getUserKeyStore(password.toCharArray());
                privateKey = (PrivateKey)keyStore.getKey(ContextVS.KEYSTORE_USER_CERT_ALIAS, password.toCharArray());
                java.security.cert.Certificate[] chain = keyStore.getCertificateChain(ContextVS.KEYSTORE_USER_CERT_ALIAS);
                cryptoTokenCert = (X509Certificate) chain[0];
            } catch(Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR_REQUEST, ex.getMessage());
            }
            String encryptedData = null;
            for(Map encryptedDataMap : encryptedDataList) {
                Long serialNumber = Long.valueOf((String) encryptedDataMap.get("serialNumber"));
                if(serialNumber == cryptoTokenCert.getSerialNumber().longValue()) {
                    logger.debug("Cert matched - serialNumber: " + serialNumber);
                    encryptedData = (String) encryptedDataMap.get("encryptedData");
                }
            }
            ResponseVS responseVS = null;
            if(encryptedData != null) {
                responseVS = Encryptor.decryptCMS(encryptedData.getBytes(), privateKey);
                responseVS.setContentType(ContentTypeVS.JSON);
                Map editDataMap = new HashMap();
                editDataMap.put("operation", TypeVS.MESSAGEVS_EDIT.toString());
                editDataMap.put("locale", ContextVS.getInstance().getLocale().getLanguage());
                editDataMap.put("state", "CONSUMED");
                editDataMap.put("messageId", documentToDecrypt.get("id"));
                JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(editDataMap);
                WebSocketService.getInstance().sendMessage(jsonObject.toString());
            }
            else {
                logger.error("Unable to decrypt from this device");
                responseVS = new ResponseVS(ResponseVS.SC_ERROR);
            }
            //[id:messageVS.fromUserVS.id, name:messageVS.fromUserVS.name]
            //messageVSList.add([fromUser: fromUser, dateCreated:messageVS.dateCreated,
            //encryptedDataList:messageVSJSON.encryptedDataList]

            return responseVS;
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
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
                else certificationRequest = (CertificationRequestVS) responseVS.getData();

                jsonObject = (JSONObject) JSONSerializer.toJSON(operationVS.getDocumentToSignMap());
                String textToSign = jsonObject.toString();
                fromUser = hashCertVSBase64;
                toUser = StringUtils.getNormalized(ContextVS.getInstance().getAccessControl().getName());
                smimeMessage = certificationRequest.genMimeMessage(fromUser, toUser, textToSign,
                        operationVS.getSignedMessageSubject(), null);
                String anonymousDelegationURL = ContextVS.getInstance().getAccessControl().
                        getAnonymousDelegationServiceURL();
                SMIMESignedSender signedSender = new SMIMESignedSender(smimeMessage, anonymousDelegationURL,
                        ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                        ContentTypeVS.JSON_SIGNED, certificationRequest.getKeyPair(),
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
            String documentToSignStr = null;
            if(operationVS.getAsciiDoc() != null) {
                documentToSignStr = operationVS.getAsciiDoc();
            } else {
                JSONObject documentToSignJSON = (JSONObject) JSONSerializer.toJSON(operationVS.getDocumentToSignMap());
                documentToSignStr = documentToSignJSON.toString();
            }
            SMIMEMessageWrapper smimeMessage = ContentSignerHelper.genMimeMessage(null,
                    operationVS.getNormalizedReceiverName(), documentToSignStr,
                    password.toCharArray(), operationVS.getSignedMessageSubject(), null);
            if(operationVS.getAsciiDoc() != null) {
                smimeMessage.setHeader(SMIMEMessageWrapper.CONTENT_TYPE_VS, ContentTypeVS.ASCIIDOC.getName());
            }
            SMIMESignedSender senderWorker = new SMIMESignedSender(smimeMessage, operationVS.getServiceURL(),
                    targetServer.getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED, null,
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
                actorVS = ActorVS.parse(responseVS.getMessageJSON());
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
