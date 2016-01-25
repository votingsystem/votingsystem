package org.votingsystem.client.webextension.service;

import com.google.common.eventbus.Subscribe;
import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.Button;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.webextension.BrowserHost;
import org.votingsystem.client.webextension.dialog.PasswordDialog;
import org.votingsystem.client.webextension.dto.BrowserSessionDto;
import org.votingsystem.client.webextension.util.Utils;
import org.votingsystem.dto.*;
import org.votingsystem.dto.voting.RepresentationStateDto;
import org.votingsystem.dto.voting.RepresentativeDelegationDto;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.voting.RepresentationState;
import org.votingsystem.service.EventBusService;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SMIMESignedGeneratorVS;
import org.votingsystem.signature.util.*;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.webextension.BrowserHost.showMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserSessionService {

    private static Logger log = Logger.getLogger(BrowserSessionService.class.getSimpleName());

    private ResponseVS currentResponseVS;
    private File sessionFile;
    private File representativeStateFile;
    private RepresentativeDelegationDto anonymousDelegationDto;
    private RepresentationStateDto representativeStateDto;
    private BrowserSessionDto browserSessionDto;
    private static PrivateKey privateKey;
    private static Certificate[] chain;
    private static CountDownLatch countDownLatch;
    private static SMIMEMessage smimeMessage;
    private static ResponseVS<SMIMEMessage> messageToDeviceResponse;
    private static final BrowserSessionService INSTANCE = new BrowserSessionService();

    class EventBusWebSocketListener {
        @Subscribe public void call(SocketMessageDto socketMessage) {
            switch(socketMessage.getOperation()) {
                case DISCONNECT:
                    browserSessionDto.setIsConnected(false);
                    flush();
                    break;
                case CONNECT:
                    browserSessionDto.setIsConnected(true);
                    flush();
                    break;
            }
        }
    }

    private BrowserSessionService() {
        try {
            sessionFile = new File(ContextVS.getInstance().getAppDir() + File.separator + ContextVS.BROWSER_SESSION_FILE);
            if(sessionFile.createNewFile()) {
                browserSessionDto = new BrowserSessionDto();
            } else {
                try {
                    browserSessionDto = JSON.getMapper().readValue(sessionFile, BrowserSessionDto.class);
                } catch (Exception ex) {
                    browserSessionDto = new BrowserSessionDto();
                    log.log(Level.SEVERE, "CORRUPTED BROWSER SESSION FILE!!!", ex);
                }
            }
            browserSessionDto.setIsConnected(false);
            flush();
            EventBusService.getInstance().register(new EventBusWebSocketListener());
        } catch (Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
    }

    private void loadRepresentationData() throws Exception {
        RepresentationStateDto stateDto = null;
        UserVS userVS = null;
        if(browserSessionDto.getUserVS() != null) userVS = browserSessionDto.getUserVS().getUserVS();
        if(userVS != null) {
            stateDto = HttpHelper.getInstance().getData(RepresentationStateDto.class,
                    ContextVS.getInstance().getAccessControl().getRepresentationStateServiceURL(userVS.getNif()),
                    MediaTypeVS.JSON);
        }
        representativeStateFile = new File(ContextVS.getInstance().getAppDir() + File.separator + ContextVS.REPRESENTATIVE_STATE_FILE);
        if(representativeStateFile.createNewFile()) {
            representativeStateDto = stateDto;
            flush();
        } else {
            representativeStateDto = JSON.getMapper().readValue(representativeStateFile, RepresentationStateDto.class);
            if(stateDto != null && stateDto.getBase64ContentDigest() != null) {
                if(!stateDto.getBase64ContentDigest().equals(representativeStateDto.getBase64ContentDigest())) {
                    log.info("Base64ContentDigest mismatch - updating local representativeState");
                    representativeStateDto = stateDto;
                    flush();
                }
            } else if(stateDto != null && stateDto.getState() == RepresentationState.WITHOUT_REPRESENTATION) {
                if(representativeStateDto.getState() != RepresentationState.WITHOUT_REPRESENTATION) {
                    representativeStateDto = stateDto;
                    flush();
                }
            }
        }
    }

    public void setAnonymousDelegationDto(RepresentativeDelegationDto delegation) {
        try {
            loadRepresentationData();
            representativeStateDto = new RepresentationStateDto(delegation);
            anonymousDelegationDto = delegation;
            flush();
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
    }

    public RepresentationStateDto getRepresentationState() throws Exception {
        loadRepresentationData();
        RepresentationStateDto result = representativeStateDto.clone();
        String stateMsg = null;
        switch (representativeStateDto.getState()) {
            case WITH_ANONYMOUS_REPRESENTATION:
                stateMsg = ContextVS.getMessage("withAnonymousRepresentationMsg");
                break;
            case REPRESENTATIVE:
                stateMsg = ContextVS.getMessage("userRepresentativeMsg");
                break;
            case WITH_PUBLIC_REPRESENTATION:
                stateMsg = ContextVS.getMessage("withPublicRepresentationMsg");
                break;
            case WITHOUT_REPRESENTATION:
                stateMsg = ContextVS.getMessage("withoutRepresentationMsg");
                break;
        }
        result.setStateMsg(stateMsg);
        result.setLastCheckedDateMsg(ContextVS.getMessage("lastCheckedDateMsg",
                DateUtils.getDayWeekDateStr(result.getLastCheckedDate(), "HH:mm")));
        return result;
    }

    public RepresentativeDelegationDto getAnonymousDelegationDto() {
        if(anonymousDelegationDto != null) return anonymousDelegationDto;
        try {
            loadRepresentationData();
            String serializedDelegation = representativeStateDto.getAnonymousDelegationObject();
            if(serializedDelegation != null) {
                anonymousDelegationDto = (RepresentativeDelegationDto) ObjectUtils.deSerializeObject(
                        serializedDelegation.getBytes());
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        } finally {
            return anonymousDelegationDto;
        }
    }

    public static BrowserSessionService getInstance() {
        return INSTANCE;
    }

    public SocketMessageDto initAuthenticatedSession(SocketMessageDto socketMsg, UserVS userVS) {
        try {
            if(ResponseVS.SC_WS_CONNECTION_INIT_OK == socketMsg.getStatusCode()) {
                socketMsg.setUserVS(userVS);
                browserSessionDto.setUserVS(UserVSDto.COMPLETE(userVS));
                browserSessionDto.setIsConnected(true);
                browserSessionDto.getUserVS().setDeviceVS(socketMsg.getConnectedDevice());
                flush();
                ContextVS.getInstance().setConnectedDevice(socketMsg.getConnectedDevice());
                BrowserHost.sendMessageToBrowser(MessageDto.WEB_SOCKET(ResponseVS.SC_WS_CONNECTION_INIT_OK, null));
            } else {
                showMessage(ResponseVS.SC_ERROR, socketMsg.getMessage());
                log.log(Level.SEVERE,"ERROR - initAuthenticatedSession - statusCode: " + socketMsg.getStatusCode());
            }
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
        return socketMsg;
    }

    public static void decryptMessage(SocketMessageDto socketMessage) throws Exception {
        if(privateKey != null) socketMessage.decryptMessage(privateKey);
        else {
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("improperTokenErrorMsg"));
            throw new ExceptionVS(ContextVS.getMessage("improperTokenErrorMsg"));
        }
    }

    public void setCSRRequestId(Long id) {
        browserSessionDto.setCsrRequestId(id);
        flush();
    }

    public void setCryptoToken(DeviceVSDto cryptoToken) {
        log.info("setCryptoToken - type: " + cryptoToken.getType());
        ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN, cryptoToken.getType().toString());
        browserSessionDto.setCryptoToken(cryptoToken);
        flush();
    }

    public DeviceVSDto getCryptoToken() {
        return browserSessionDto.getCryptoToken();
    }

    public static CryptoTokenVS getCryptoTokenType () {
        String  tokenType = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.JKS_KEYSTORE.toString());
        return CryptoTokenVS.valueOf(tokenType);
    }

    public Long getCSRRequestId() {
        return browserSessionDto.getCsrRequestId();
    }

    public UserVS getUserVS()  {
        try {
            if(browserSessionDto.getUserVS() != null) return browserSessionDto.getUserVS().getUserVS();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    public DeviceVSDto getConnectedDevice()  {
        try {
            if(browserSessionDto.getUserVS() != null) return browserSessionDto.getUserVS().getDeviceVS();
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    public void setUserVS(UserVS userVS, boolean isConnected) throws Exception {
        browserSessionDto.setUserVS(UserVSDto.COMPLETE(userVS));
        List<UserVSDto> userVSList = browserSessionDto.getUserVSList();
        boolean updated = false;
        for(int i = 0; i < userVSList.size(); i++) {
            UserVSDto user = userVSList.get(i);
            if(user.getNIF().equals(userVS.getNif())) {
                userVSList.remove(i);
                userVSList.add(browserSessionDto.getUserVS());
                updated = true;
            }
        }
        if(!updated) userVSList.add(browserSessionDto.getUserVS());
        browserSessionDto.setIsConnected(isConnected);
        flush();
    }

    public UserVS getKeyStoreUserVS() {
        return ContextVS.getInstance().getKeyStoreUserVS();
    }

    public BrowserSessionDto getBrowserSessionData() {
        return browserSessionDto;
    }

    public void setCSRRequest(Long requestId, EncryptedBundle bundle) {
        try {
            File csrFile = new File(ContextVS.getInstance().getAppDir() + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
            csrFile.createNewFile();
            EncryptedBundleDto bundleDto = new EncryptedBundleDto(bundle);
            bundleDto.setId(requestId);
            JSON.getMapper().writeValue(csrFile, bundleDto);
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
    }

    private void deleteCSR() {
        log.info("deleteCSR");
        File csrFile = new File(ContextVS.getInstance().getAppDir() + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
        if(csrFile.exists()) csrFile.delete();
    }

    public void checkCSR() {
        PlatformImpl.runLater(() -> {
            File csrFile = new File(ContextVS.getInstance().getAppDir() + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
            if(csrFile.exists()) {
                log.info("csr request found");
                try {
                    EncryptedBundleDto bundleDto = JSON.getMapper().readValue(csrFile, EncryptedBundleDto.class);
                    String serviceURL = ContextVS.getInstance().getAccessControl().getUserCSRServiceURL(bundleDto.getId());
                    ResponseVS responseVS = HttpHelper.getInstance().getData(serviceURL, null);
                    if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        currentResponseVS = responseVS;
                        PasswordDialog.Listener passwordListener = new PasswordDialog.Listener() {
                            @Override public void processPassword(char[] password) {
                                if(password == null) {
                                    Button optionButton = new Button(ContextVS.getMessage("deletePendingCsrMsg"),
                                            Utils.getIcon(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
                                    optionButton.setOnAction(event -> deleteCSR());
                                    showMessage(null, ContextVS.getMessage("certPendingMissingPasswdMsg"), optionButton, null);
                                } else {
                                    try {
                                        File csrFile = new File(ContextVS.getInstance().getAppDir() + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
                                        EncryptedBundleDto bundleDto = JSON.getMapper().readValue(csrFile, EncryptedBundleDto.class);
                                        Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(
                                                currentResponseVS.getMessage().getBytes());
                                        X509Certificate userCert = certificates.iterator().next();
                                        UserVS user = UserVS.getUserVS(userCert);
                                        setUserVS(user, false);
                                        log.info("user: " + user.getNif() + " - certificates.size(): " + certificates.size());
                                        X509Certificate[] certsArray = new X509Certificate[certificates.size()];
                                        certificates.toArray(certsArray);
                                        byte[] serializedCertificationRequest = null;
                                        EncryptedBundle bundle = bundleDto.getEncryptedBundle();
                                        try {
                                            serializedCertificationRequest = Encryptor.pbeAES_Decrypt(password, bundle);
                                            CertificationRequestVS certificationRequest =
                                                    (CertificationRequestVS) ObjectUtils.deSerializeObject(serializedCertificationRequest);
                                            KeyStore userKeyStore = KeyStore.getInstance("JKS");
                                            userKeyStore.load(null);
                                            userKeyStore.setKeyEntry(ContextVS.KEYSTORE_USER_CERT_ALIAS, certificationRequest.getPrivateKey(),
                                                    password, certsArray);
                                            ContextVS.getInstance().saveUserKeyStore(userKeyStore, password);
                                            ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.JKS_KEYSTORE.toString());
                                            showMessage(ResponseVS.SC_OK, ContextVS.getMessage("certInstallOKMsg"));
                                            csrFile.delete();
                                            CertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CertExtensionDto.class,
                                                    user.getCertificate(), ContextVS.DEVICEVS_OID);
                                            DeviceVSDto deviceVSDto = new DeviceVSDto(user, certExtensionDto);
                                            deviceVSDto.setType(CryptoTokenVS.JKS_KEYSTORE);
                                            deviceVSDto.setDeviceName(user.getNif() + " - " + user.getName());
                                            setCryptoToken(deviceVSDto);
                                            currentResponseVS = null;
                                        } catch (Exception ex) {
                                            showMessage(ContextVS.getMessage("cryptoTokenPasswdErrorMsg"), ContextVS.getMessage("errorLbl"));
                                        }
                                    } catch (Exception ex) {
                                        showMessage(ResponseVS.SC_ERROR, ex.getMessage());
                                    }
                                }
                            }
                        };
                        PasswordDialog.showWithPasswordConfirm(passwordListener, ContextVS.getMessage("csrPasswMsg"));
                    } else showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("certPendingMsg"));
                } catch (Exception ex) {
                    log.log(Level.SEVERE,ex.getMessage(), ex);
                    showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("errorStoringKeyStoreMsg"));
                }
            }
        });
    }

    public static SMIMEMessage getSMIME(String fromUser, String toUser, String textToSign,
            char[] password, String subject) throws Exception {
        String  tokenType = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.JKS_KEYSTORE.toString());
        log.info("getSMIME - tokenType: " + tokenType);
        switch(CryptoTokenVS.valueOf(tokenType)) {
            case JKS_KEYSTORE:
                if(password != null) {
                    KeyStore keyStore = ContextVS.getInstance().getUserKeyStore(password);
                    privateKey = (PrivateKey)keyStore.getKey(ContextVS.KEYSTORE_USER_CERT_ALIAS, password);
                    chain = keyStore.getCertificateChain(ContextVS.KEYSTORE_USER_CERT_ALIAS);
                }
                SMIMESignedGeneratorVS signedGenerator = new SMIMESignedGeneratorVS(
                        privateKey, chain, ContextVS.DNIe_SIGN_MECHANISM);
                SMIMEMessage smime = signedGenerator.getSMIME(fromUser, toUser, textToSign, subject);
                MessageTimeStamper timeStamper = new MessageTimeStamper(smime,
                        ContextVS.getInstance().getDefaultServer().getTimeStampServiceURL());
                timeStamper.call();
                return timeStamper.getSMIME();
            case MOBILE:
                countDownLatch = new CountDownLatch(1);
                DeviceVS deviceVS = getInstance().getCryptoToken().getDeviceVS();
                SocketMessageDto messageDto = SocketMessageDto.getSignRequest(deviceVS, toUser, textToSign, subject);
                PlatformImpl.runLater(() -> {//Service must only be used from the FX Application Thread
                    try {
                        WebSocketService.getInstance().sendMessage(JSON.getMapper().writeValueAsString(messageDto));
                    } catch (Exception ex) { log.log(Level.SEVERE,ex.getMessage(), ex); }
                });
                countDownLatch.await();
                ResponseVS<SMIMEMessage> responseVS = getMessageToDeviceResponse();
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
                else return responseVS.getData();
            default: return null;
        }
    }

    public static void setSignResponse(SocketMessageDto socketMsg) {
        switch(socketMsg.getStatusCode()) {
            case ResponseVS.SC_WS_MESSAGE_SEND_OK:
                break;
            case ResponseVS.SC_WS_CONNECTION_NOT_FOUND:
                messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR,
                        ContextVS.getMessage("deviceVSTokenNotFoundErrorMsg"));
                countDownLatch.countDown();
                break;
            case ResponseVS.SC_ERROR:
                messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR, socketMsg.getMessage());
                countDownLatch.countDown();
                break;
            default:
                try {
                    smimeMessage = socketMsg.getSMIME();
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_OK, null, smimeMessage);
                } catch(Exception ex) {
                    log.log(Level.SEVERE,ex.getMessage(), ex);
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR, ex.getMessage());
                }
                countDownLatch.countDown();
        }
    }

    public static ResponseVS getMessageToDeviceResponse() {
        return messageToDeviceResponse;
    }

    private void flush() {
        log.info("flush");
        try {
            JSON.getMapper().writeValue(sessionFile, browserSessionDto);
            if(representativeStateDto != null) JSON.getMapper().writeValue(representativeStateFile, representativeStateDto);
        } catch(Exception ex) {
            log.log(Level.SEVERE,ex.getMessage(), ex);
        }
    }

}
