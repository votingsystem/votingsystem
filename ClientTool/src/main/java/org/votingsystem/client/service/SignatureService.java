package org.votingsystem.client.service;

import com.sun.javafx.application.PlatformImpl;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.encoders.Hex;
import org.votingsystem.callable.AccessRequestDataSender;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.callable.RepresentativeDataSender;
import org.votingsystem.callable.SMIMESignedSender;
import org.votingsystem.client.Browser;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.pane.DocumentVSBrowserPane;
import org.votingsystem.client.util.InboxMessage;
import org.votingsystem.client.util.MsgUtils;
import org.votingsystem.client.util.Utils;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.CurrencyIssuedDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.dto.voting.AnonymousDelegationDto;
import org.votingsystem.dto.voting.EventVSDto;
import org.votingsystem.dto.voting.VoteVSDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyRequestBatch;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.model.voting.AccessControlVS;
import org.votingsystem.model.voting.ControlCenterVS;
import org.votingsystem.model.voting.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.EncryptedBundle;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;
import org.votingsystem.util.currency.MapUtils;
import org.votingsystem.util.currency.Wallet;

import java.io.File;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toSet;
import static org.votingsystem.util.ContextVS.*;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignatureService extends Service<ResponseVS> {

    private static Logger log = Logger.getLogger(SignatureService.class.getSimpleName());

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
            log.info("SignatureService.SignatureTask - call:" + operationVS.getType());
            ResponseVS responseVS = null;
            updateProgress(5, 100);
            try {
                switch (operationVS.getType()) {
                    case SEND_SMIME_VOTE:
                        String accessControlURL = operationVS.getEventVS().getServerURL();
                        responseVS = Utils.checkServer(accessControlURL);
                        if (ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
                        else ContextVS.getInstance().setServer((AccessControlVS) responseVS.getData());
                        operationVS.setTargetServer((AccessControlVS) responseVS.getData());
                        String controlCenterURL = operationVS.getEventVS().getControlCenter().getServerURL();
                        responseVS = Utils.checkServer(controlCenterURL);
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            ContextVS.getInstance().setControlCenter((ControlCenterVS) responseVS.getData());
                        }
                        break;
                    case CURRENCY_DELETE:
                        responseVS = new ResponseVS(ResponseVS.SC_OK);
                        break;
                    default:
                        responseVS = Utils.checkServer(operationVS.getServerURL().trim());
                        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                            ContextVS.getInstance().setServer((ActorVS) responseVS.getData());
                            operationVS.setTargetServer((ActorVS) responseVS.getData());
                        }
                }
                updateProgress(25, 100);
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    updateMessage(getOperationMessage(operationVS));
                    updateProgress(40, 100);
                    switch(operationVS.getType()) {
                        case SEND_SMIME_VOTE:
                            responseVS = sendVote(operationVS);
                            break;
                        case CANCEL_VOTE:
                            responseVS = cancelVote(operationVS);
                            break;
                        case CERT_USER_NEW:
                            responseVS = sendCSRRequest(operationVS);
                            break;
                        case NEW_REPRESENTATIVE:
                            responseVS = processNewRepresentative(operationVS);
                            break;
                        case ANONYMOUS_REPRESENTATIVE_SELECTION:
                            responseVS = processAnonymousDelegation(operationVS);
                            break;
                        case ANONYMOUS_REPRESENTATIVE_SELECTION_CANCELED:
                            responseVS = processCancelAnonymousDelegation(operationVS);
                            break;
                        case VOTING_PUBLISHING:
                            responseVS = publishSMIME(operationVS);
                            break;
                        case MESSAGEVS:
                            responseVS = WebSocketAuthenticatedService.getInstance().sendMessageVS(operationVS);
                            break;
                        case OPEN_SMIME_FROM_URL:
                            responseVS = openReceiptFromURL(operationVS);
                            break;
                        case FILE_FROM_URL:
                            responseVS = openFileFromURL(operationVS);
                            break;
                        case CURRENCY_REQUEST:
                            responseVS = sendCurrencyRequest(operationVS);
                            break;
                        case CURRENCY_DELETE:
                            responseVS = deleteCurrency(operationVS);
                            break;
                        case REPRESENTATIVE_SELECTION:
                            responseVS = sendSMIME(operationVS);
                            break;
                        default:
                            responseVS = sendSMIME(operationVS);
                    }
                    updateProgress(100, 100);
                    return responseVS;
                } else return responseVS;
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

        private String getOperationMessage(OperationVS operationVS) {
            if(CryptoTokenVS.MOBILE == SessionService.getCryptoTokenType()) {
                return ContextVS.getMessage("messageToDeviceProgressMsg",
                        SessionService.getInstance().getDeviceVS().getType());
            } else return operationVS.getSignedMessageSubject();
        }

        private ResponseVS openFileFromURL(final OperationVS operationVS) throws Exception {
            ResponseVS responseVS = HttpHelper.getInstance().getData(operationVS.getDocumentURL(), null);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(null,
                        FileUtils.getFileFromBytes(responseVS.getMessageBytes()), null);
                Browser.getInstance().newTab(documentVSBrowserPane, documentVSBrowserPane.getCaption());
                return new ResponseVS(ResponseVS.SC_OK);
            }
            return responseVS;
        }

        private ResponseVS openReceiptFromURL(final OperationVS operationVS) throws Exception {
            ResponseVS responseVS = null;
            if(VotingSystemApp.getInstance().getSMIME(operationVS.getServiceURL()) != null) {
                responseVS = new ResponseVS(ResponseVS.SC_OK,
                        VotingSystemApp.getInstance().getSMIME(operationVS.getServiceURL()));
            } else {
                responseVS = HttpHelper.getInstance().getData(operationVS.getServiceURL(), ContentTypeVS.TEXT);
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    VotingSystemApp.getInstance().setSMIME(operationVS.getServiceURL(),
                            responseVS.getMessage());
                }
            }
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                responseVS.setStatusCode(ResponseVS.SC_INITIALIZED);
                operationVS.setMessage(responseVS.getMessage());
                PlatformImpl.runLater(() -> {
                        DocumentVSBrowserPane documentVSBrowserPane = new DocumentVSBrowserPane(operationVS.getMessage(),
                                null, operationVS.getDocument());
                        Browser.getInstance().newTab(documentVSBrowserPane, documentVSBrowserPane.getCaption());
                    });
            }
            return responseVS;
        }

        private ResponseVS sendCSRRequest(OperationVS operationVS) throws Exception {
            CertificationRequestVS certificationRequest = CertificationRequestVS.getUserRequest(
                    ContextVS.KEY_SIZE, SIG_NAME, SIGN_MECHANISM, PROVIDER,
                    (String)operationVS.getDocument().get("nif"), (String)operationVS.getDocument().get("email"),
                    (String)operationVS.getDocument().get("phone"), SessionService.getInstance().getDeviceVS().getDeviceId(),
                    (String)operationVS.getDocument().get("givenname"),
                    (String)operationVS.getDocument().get("surname"),
                    InetAddress.getLocalHost().getHostName(), DeviceVS.Type.PC);
            byte[] csrBytes = certificationRequest.getCsrPEM();
            ResponseVS responseVS = HttpHelper.getInstance().sendData(csrBytes, null,
                    ((AccessControlVS) operationVS.getTargetServer()).getUserCSRServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                Long requestId = Long.valueOf(responseVS.getMessage());
                byte[] serializedCertificationRequest = ObjectUtils.serializeObject(certificationRequest);
                EncryptedBundle bundle = Encryptor.pbeAES_Encrypt(password, serializedCertificationRequest);
                SessionService.getInstance().setCSRRequest(requestId, bundle);
            }
            return responseVS;
        }

        //we know this is done in a background thread
        private ResponseVS sendVote(OperationVS operationVS) throws Exception {
            log.info("sendVote");
            String fromUser = ContextVS.getInstance().getMessage("electorLbl");
            EventVSDto eventVS = operationVS.getEventVS();
            VoteVS voteVS = new VoteVS(eventVS.getVoteVS());
            voteVS.genVote();
            String toUser = eventVS.getAccessControl().getName();
            String msgSubject = ContextVS.getInstance().getMessage("accessRequestMsgSubject")  + eventVS.getId();
            SMIMEMessage smimeMessage = SessionService.getSMIME(fromUser, toUser, 
                    JSON.getMapper().writeValueAsString(voteVS.getAccessRequestDto()), password, msgSubject);
            AccessRequestDataSender accessRequestDataSender = new AccessRequestDataSender(smimeMessage, voteVS);
            ResponseVS responseVS = accessRequestDataSender.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            updateProgress(60, 100);
            CertificationRequestVS certificationRequest = (CertificationRequestVS) responseVS.getData();
            String textToSign = JSON.getMapper().writeValueAsString(new VoteVSDto(voteVS, null)); ;
            fromUser = eventVS.getVoteVS().getHashCertVSBase64();
            msgSubject = ContextVS.getInstance().getMessage("voteVSSubject");
            smimeMessage = certificationRequest.getSMIME(fromUser, toUser, textToSign, msgSubject, null);
            String urlVoteService = ((ControlCenterVS)ContextVS.getInstance().getControlCenter()).getVoteServiceURL();
            updateProgress(70, 100);
            SMIMESignedSender signedSender = new SMIMESignedSender(smimeMessage, urlVoteService,
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                    ContentTypeVS.VOTE, certificationRequest.getKeyPair(), ContextVS.getInstance().getControlCenter().
                    getX509Certificate(), "voteURL");
            responseVS = signedSender.call();
            updateProgress(90, 100);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessage validatedVote = responseVS.getSMIME();
                voteVS.setReceipt(validatedVote);
                ResponseVS voteResponse = new ResponseVS(ResponseVS.SC_OK, ContentTypeVS.VOTE);
                voteResponse.setType(TypeVS.VOTEVS);
                voteResponse.setData(voteVS);
                ContextVS.getInstance().addHashCertVSData(voteVS.getHashCertVSBase64(), voteResponse);
                Map dataMap = new HashMap<>();
                dataMap.put("statusCode", ResponseVS.SC_OK);
                dataMap.put("voteURL", ((List<String>)responseVS.getData()).iterator().next());
                dataMap.put("hashCertVSBase64", voteVS.getHashCertVSBase64());
                dataMap.put("hashCertVSHex", new String(Hex.encode(voteVS.getHashCertVSBase64().getBytes())));
                dataMap.put("voteVSReceipt", Base64.getEncoder().encodeToString(validatedVote.getBytes()));
                //HexBinaryAdapter hexConverter = new HexBinaryAdapter();
                //String hashCertVSBase64 = new String(hexConverter.unmarshal(hashCertVSHex));
                responseVS.setContentType(ContentTypeVS.JSON);
                responseVS.setMessageMap(dataMap);
            }
            return responseVS;
        }

        //we know this is done in a background thread
        private ResponseVS cancelVote(OperationVS operationVS) throws Exception {
            log.info("cancelVote");
            Map documentToSignMap = new HashMap<String, String>();
            documentToSignMap.put("operation", TypeVS.CANCEL_VOTE.toString());
            ResponseVS voteResponse = ContextVS.getInstance().getHashCertVSData(operationVS.getMessage());
            VoteVS voteVS = (VoteVS) voteResponse.getData();
            documentToSignMap.put("originHashAccessRequest", voteVS.getOriginHashAccessRequest());
            documentToSignMap.put("hashAccessRequestBase64", voteVS.getHashAccessRequestBase64());
            documentToSignMap.put("originHashCertVote", voteVS.getOriginHashCertVote());
            documentToSignMap.put("hashCertVSBase64", voteVS.getHashCertVSBase64());
            documentToSignMap.put("UUID", UUID.randomUUID().toString());
            operationVS.setDocumentToSignMap(documentToSignMap);
            ResponseVS responseVS = sendSMIME(operationVS);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                responseVS.setMessage(Base64.getEncoder().encodeToString(responseVS.getSMIME().getBytes()));
            }
            return responseVS;
        }

        //we know this is done in a background thread
        private ResponseVS processNewRepresentative(OperationVS operationVS) throws Exception {
            byte[] imageFileBytes = FileUtils.getBytesFromFile(operationVS.getFile());
            log.info(" - imageFileBytes.length: " + imageFileBytes.length);
            if(imageFileBytes.length > ContextVS.IMAGE_MAX_FILE_SIZE) {
                log.info(" - MAX_FILE_SIZE exceeded ");
                return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("fileSizeExceeded",
                        ContextVS.IMAGE_MAX_FILE_SIZE_KB));
            } else {
                MessageDigest messageDigest = MessageDigest.getInstance(ContextVS.VOTING_DATA_DIGEST);
                byte[] resultDigest =  messageDigest.digest(imageFileBytes);
                String base64ResultDigest = Base64.getEncoder().encodeToString(resultDigest);
                operationVS.getDocumentToSignMap().put("base64ImageHash", base64ResultDigest);
                //String base64RepresentativeEncodedImage = Base64.getEncoder().encodeToString(imageFileBytes);
                //operation.getContentFirma().put("base64RepresentativeEncodedImage", base64RepresentativeEncodedImage);
                SMIMEMessage representativeRequestSMIME = SessionService.getSMIME(null, operationVS.getReceiverName(),
                        JSON.getMapper().writeValueAsString(operationVS.getDocumentToSignMap()),
                        password, operationVS.getSignedMessageSubject());
                RepresentativeDataSender dataSender = new RepresentativeDataSender(representativeRequestSMIME,
                        operationVS.getFile(), operationVS.getServiceURL());
                ResponseVS responseVS = dataSender.call();
                return responseVS;
            }
        }

        private ResponseVS sendCurrencyRequest(OperationVS operationVS) throws Exception {
            log.info("sendCurrencyRequest");
            TransactionVSDto transactionVSDto = operationVS.getDocumentToSign(TransactionVSDto.class);
            TagVS tag = new TagVS(transactionVSDto.getTags().iterator().next());
            CurrencyRequestBatch currencyBatch = CurrencyRequestBatch.createRequest(transactionVSDto.getAmount(),
                    transactionVSDto.getAmount(), transactionVSDto.getCurrencyCode(), tag,
                    transactionVSDto.isTimeLimited(), operationVS.getTargetServer().getServerURL());
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            byte[] fileContent = JSON.getMapper().writeValueAsString(currencyBatch.getCurrencyCSRList()).getBytes();
            mapToSend.put(ContextVS.CSR_FILE_NAME, fileContent);
            String textToSign = JSON.getMapper().writeValueAsString(currencyBatch.getRequestDto());
            SMIMEMessage smimeMessage = SessionService.getSMIME(null, operationVS.getReceiverName(), textToSign,
                    password, operationVS.getSignedMessageSubject());
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage,
                    operationVS.getTargetServer().getTimeStampServiceURL());
            smimeMessage = timeStamper.call();
            mapToSend.put(ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME, smimeMessage.getBytes());
            ResponseVS responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                    ((CurrencyServer)operationVS.getTargetServer()).getCurrencyRequestServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                CurrencyIssuedDto dto = (CurrencyIssuedDto) responseVS.getMessage(CurrencyIssuedDto.class);
                currencyBatch.loadIssuedCurrency(dto.getIssuedCurrency());
                Wallet.saveToPlainWallet(currencyBatch.getCurrencyMap().values());
                Map responseMap = new HashMap<>();
                responseMap.put("statusCode", responseVS.getStatusCode());
                responseMap.put("message", dto.getMessage());
                responseVS.setContentType(ContentTypeVS.JSON);
                responseVS.setMessageMap(responseMap);
                InboxMessage inboxMessage = new InboxMessage(ContextVS.getMessage("systemLbl"), new Date());
                inboxMessage.setMessage(MsgUtils.getPlainWalletNotEmptyMsg(MapUtils.getCurrencyMap(
                        currencyBatch.getCurrencyMap().values()))).setTypeVS(TypeVS.CURRENCY_IMPORT);
                InboxService.getInstance().newMessage(inboxMessage, false);
            }
            return responseVS;
        }

        private ResponseVS deleteCurrency(OperationVS operationVS) throws Exception {
            log.info("deleteCurrency");
            try {
                Set<Currency> wallet = Wallet.getWallet(password);
                wallet = wallet.stream().filter(currency -> {
                    if (currency.getHashCertVS().equals(operationVS.getMessage())) {
                        log.info("deleted currency with hashCertVS: " + operationVS.getMessage());
                        return false;
                    } else return true;
                }).collect(toSet());
                Wallet.saveWallet(CurrencyDto.serializeCollection(wallet), password);
                return new ResponseVS(ResponseVS.SC_OK).setType(TypeVS.CURRENCY_DELETE).setStatus(new StatusVS() {});
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

        //we know this is done in a background thread
        private ResponseVS processCancelAnonymousDelegation(OperationVS operationVS) throws Exception {
            String outputFolder = ContextVS.APPTEMPDIR + File.separator + UUID.randomUUID();
            AnonymousDelegationDto delegation = SessionService.getInstance().getAnonymousDelegationDto();
            if(delegation == null) return new ResponseVS(ResponseVS.SC_ERROR,
                    ContextVS.getMessage("anonymousDelegationDataMissingMsg"));
            SMIMEMessage smimeMessage = SessionService.getSMIME(null,
                    operationVS.getReceiverName(), delegation.getCancellationRequest().toString(),
                    password, operationVS.getSignedMessageSubject());
            SMIMESignedSender senderWorker = new SMIMESignedSender(smimeMessage, operationVS.getServiceURL(),
                    operationVS.getTargetServer().getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED, null, null);
            ResponseVS responseVS = senderWorker.call();
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                SMIMEMessage delegationReceipt = responseVS.getSMIME();
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    Collection matches = delegationReceipt.checkSignerCert(
                            ContextVS.getInstance().getAccessControl().getX509Certificate());
                    if(!(matches.size() > 0)) throw new ExceptionVS("Response without server signature");
                    responseVS.setSMIME(delegationReceipt);
                    responseVS.setMessage(ContextVS.getMessage("cancelAnonymousRepresentationOkMsg"));
                    return responseVS;
                }
            }
            return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage("errorLbl"));
        }

        //we know this is done in a background thread
        private ResponseVS processAnonymousDelegation(OperationVS operationVS) throws Exception {
            String caption = operationVS.getCaption();
            UserVSDto representative = operationVS.getData(UserVSDto.class);
            if(caption.length() > 50) caption = caption.substring(0, 50) + "...";
            AnonymousDelegationDto anonymousDelegation = new AnonymousDelegationDto(
                    Integer.valueOf((String) operationVS.getDocumentToSignMap().get("weeksOperationActive")),
                    (String) operationVS.getDocumentToSignMap().get("representativeNif"),
                    (String) operationVS.getDocumentToSignMap().get("representativeName"),
                    ContextVS.getInstance().getAccessControl().getServerURL());
            try {
                SMIMEMessage smimeMessage = SessionService.getSMIME(null, ContextVS.getInstance().getAccessControl().
                        getName(), anonymousDelegation.getRequest().toString(), password,
                        operationVS.getSignedMessageSubject());
                TimeStampRequest timeStampRequest = smimeMessage.getTimeStampRequest();
                ResponseVS responseVS = HttpHelper.getInstance().sendData(timeStampRequest.getEncoded(),
                        ContentTypeVS.TIMESTAMP_QUERY, ContextVS.getInstance().getAccessControl().getTimeStampServiceURL());
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    byte[] bytesToken = responseVS.getMessageBytes();
                    TimeStampToken timeStampToken = new TimeStampToken(new CMSSignedData(bytesToken));
                    X509Certificate timeStampCert = ContextVS.getInstance().getAccessControl().getTimeStampCert();
                    SignerInformationVerifier timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().
                            setProvider(ContextVS.PROVIDER).build(timeStampCert);
                    timeStampToken.validate(timeStampSignerInfoVerifier);
                    smimeMessage.setTimeStampToken(timeStampToken);
                    //byte[] encryptedCSRBytes = Encryptor.encryptMessage(certificationRequest.getCsrPEM(),destinationCert);
                    //byte[] delegationEncryptedBytes = Encryptor.encryptSMIME(smimeMessage, destinationCert);
                    String representationDataFile = ContextVS.REPRESENTATIVE_DATA_FILE_NAME + ":" +
                            MediaTypeVS.JSON_SIGNED;
                    Map<String, Object> mapToSend = new HashMap<String, Object>();
                    mapToSend.put(ContextVS.CSR_FILE_NAME, anonymousDelegation.getCertificationRequest().getCsrPEM());
                    mapToSend.put(representationDataFile, smimeMessage.getBytes());
                    responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                            ContextVS.getInstance().getAccessControl().getAnonymousDelegationRequestServiceURL());
                    if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        //byte[] decryptedData = Encryptor.decryptFile(responseVS.getMessageBytes(),
                        // certificationRequest.getPublicKey(), certificationRequest.getPrivateKey());
                        anonymousDelegation.getCertificationRequest().initSigner(responseVS.getMessageBytes());
                    } else return responseVS;
                }
                updateProgress(60, 100);
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
                smimeMessage = anonymousDelegation.getCertificationRequest().getSMIME(anonymousDelegation.getHashCertVS(),
                        ContextVS.getInstance().getAccessControl().getName(),
                        anonymousDelegation.getDelegation().toString(),
                        operationVS.getSignedMessageSubject(), null);
                SMIMESignedSender signedSender = new SMIMESignedSender(smimeMessage,
                        ContextVS.getInstance().getAccessControl().getAnonymousDelegationServiceURL(),
                        ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                        ContentTypeVS.JSON_SIGNED, anonymousDelegation.getCertificationRequest().getKeyPair(),
                        ContextVS.getInstance().getAccessControl().getX509Certificate());
                responseVS = signedSender.call();
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    anonymousDelegation.setDelegationReceipt(responseVS.getSMIME(),
                            ContextVS.getInstance().getAccessControl().getX509Certificate());
                    anonymousDelegation.setRepresentative(representative);
                    SessionService.getInstance().setAnonymousDelegationDto(anonymousDelegation);
                }
                return responseVS;
            } catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
            }
        }

        //we know this is done in a background thread
        private ResponseVS sendSMIME(OperationVS operationVS, String... header) throws Exception {
            log.info("sendSMIME");
            String documentToSignStr = null;
            if(operationVS.getAsciiDoc() != null) {
                documentToSignStr = operationVS.getAsciiDoc();
            } else {
                documentToSignStr = JSON.getMapper().writeValueAsString(operationVS.getDocumentToSignMap());
            }
            SMIMEMessage smimeMessage = SessionService.getSMIME(null, operationVS.getReceiverName(),
                    documentToSignStr, password, operationVS.getSignedMessageSubject());
            updateMessage(operationVS.getSignedMessageSubject());
            if(operationVS.getAsciiDoc() != null) {
                smimeMessage.setHeader(SMIMEMessage.CONTENT_TYPE_VS, ContentTypeVS.ASCIIDOC.getName());
            }
            SMIMESignedSender senderWorker = new SMIMESignedSender(smimeMessage, operationVS.getServiceURL(),
                    operationVS.getTargetServer().getTimeStampServiceURL(), ContentTypeVS.JSON_SIGNED, null,
                    operationVS.getTargetServer().getX509Certificate(), header);
            return senderWorker.call();
        }

        //we know this is done in a background thread
        private ResponseVS<ActorVS> publishSMIME(OperationVS operationVS) throws Exception {
            log.info("publishPoll");
            ResponseVS responseVS = sendSMIME(operationVS, "eventURL");
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                String eventURL = ((List<String>)responseVS.getData()).iterator().next() +"?menu=admin";
                operationVS.setDocumentURL(eventURL);
                //String receipt = responseVS.getMessage();
                responseVS.setMessage(eventURL);
            }
            return responseVS;
        }
    }

}
