package org.votingsystem.client.service;

import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.Button;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.VotingSystemApp;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.model.Representation;
import org.votingsystem.client.util.Utils;
import org.votingsystem.client.util.WebSocketMessage;
import org.votingsystem.model.*;
import org.votingsystem.signature.dnie.DNIeContentSigner;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SMIMESignedGeneratorVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import javax.mail.Header;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.votingsystem.client.VotingSystemApp.showMessage;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SessionService {

    private static Logger log = Logger.getLogger(SessionService.class);

    private UserVS userVS;
    private File sessionFile;
    private File representativeStateFile;
    private AnonymousDelegationRequest anonymousDelegationRequest;
    private JSONObject representativeStateJSON;
    private JSONObject browserSessionDataJSON;
    private JSONObject sessionDataJSON;
    private static CountDownLatch countDownLatch;
    private static SMIMEMessage smimeMessage;
    private static ResponseVS<SMIMEMessage> messageToDeviceResponse;
    private static final SessionService INSTANCE = new SessionService();

    private SessionService() {
        try {
            sessionFile = new File(ContextVS.APPDIR + File.separator + ContextVS.BROWSER_SESSION_FILE);
            if(sessionFile.createNewFile()) {
                sessionDataJSON = new JSONObject();
                browserSessionDataJSON = new JSONObject();
                browserSessionDataJSON.put("deviceId", UUID.randomUUID().toString());
                browserSessionDataJSON.put("fileType", ContextVS.BROWSER_SESSION_FILE);
                sessionDataJSON.put("browserSession", browserSessionDataJSON);
            } else {
                sessionDataJSON = (JSONObject) JSONSerializer.toJSON(FileUtils.getStringFromFile(sessionFile));
                browserSessionDataJSON = sessionDataJSON.getJSONObject("browserSession");
            }
            browserSessionDataJSON.put("isConnected", false);
            if(browserSessionDataJSON.get("userVS") != null) userVS =
                    UserVS.parse((java.util.Map) browserSessionDataJSON.get("userVS"));
            flush();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void loadRepresentationData() throws IOException {
        JSONObject userVSStateOnServerJSON = null;
        if(userVS != null) {
            ResponseVS responseVS = HttpHelper.getInstance().getData(ContextVS.getInstance().getAccessControl().
                    getRepresentationStateServiceURL(userVS.getNif()), ContentTypeVS.JSON);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                userVSStateOnServerJSON = (JSONObject) responseVS.getMessageJSON();
            }
        }
        representativeStateFile = new File(ContextVS.APPDIR + File.separator + ContextVS.REPRESENTATIVE_STATE_FILE);
        if(representativeStateFile.createNewFile()) {
            representativeStateJSON = userVSStateOnServerJSON;
            flush();
        } else {
            representativeStateJSON = (JSONObject) JSONSerializer.toJSON(
                    FileUtils.getStringFromFile(representativeStateFile));
            if(userVSStateOnServerJSON != null) {
                if(!userVSStateOnServerJSON.getString("base64ContentDigest").equals(
                        representativeStateJSON.getString("base64ContentDigest"))) {
                    representativeStateJSON = userVSStateOnServerJSON;
                    flush();
                }
            }
        }
    }

    public void setAnonymousDelegationRequest(AnonymousDelegationRequest delegation) {
        try {
            loadRepresentationData();
            String serializedDelegation = new String(ObjectUtils.serializeObject(delegation), "UTF-8");
            representativeStateJSON.put("state", Representation.State.WITH_ANONYMOUS_REPRESENTATION.toString());
            representativeStateJSON.put("lastCheckedDate", DateUtils.getDateStr(Calendar.getInstance().getTime()));
            representativeStateJSON.put("representative", delegation.getRepresentative().toJSON());
            representativeStateJSON.put("anonymousDelegationObject", serializedDelegation);
            representativeStateJSON.put("dateFrom", DateUtils.getDayWeekDateStr(delegation.getDateFrom()));
            representativeStateJSON.put("dateTo", DateUtils.getDayWeekDateStr(delegation.getDateTo()));
            representativeStateJSON.put("base64ContentDigest", delegation.getCancelVoteReceipt().getContentDigestStr());
            this.anonymousDelegationRequest = delegation;
            flush();
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public JSONObject getRepresentationState() {
        JSONObject result = null;
        try {
            loadRepresentationData();
            result = (JSONObject) JSONSerializer.toJSON(representativeStateJSON);
            result.remove("anonymousDelegationObject");
            Representation.State representationState =
                    Representation.State.valueOf((String) representativeStateJSON.get("state"));
            String stateMsg = null;
            switch (representationState) {
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
            Date lastCheckedDate = DateUtils.getDateFromString(representativeStateJSON.getString("lastCheckedDate"));
            result.put("stateMsg", stateMsg);
            result.put("lastCheckedDateMsg", ContextVS.getMessage("lastCheckedDateMsg",
                    DateUtils.getDayWeekDateStr(lastCheckedDate)));
        } catch(Exception ex) { log.error(ex.getMessage(), ex);
        } finally {
            return result;
        }
    }

    public AnonymousDelegationRequest getAnonymousDelegationRequest() {
        if(anonymousDelegationRequest != null) return anonymousDelegationRequest;
        try {
            loadRepresentationData();
            String serializedDelegation = representativeStateJSON.getString("anonymousDelegationObject");
            if(serializedDelegation != null) {
                anonymousDelegationRequest = (AnonymousDelegationRequest) ObjectUtils.deSerializeObject(
                        serializedDelegation.getBytes());
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            return anonymousDelegationRequest;
        }
    }

    public static SessionService getInstance() {
        return INSTANCE;
    }

    public void setIsConnected(boolean isConnected) {
        browserSessionDataJSON.put("isConnected", isConnected);
        flush();
    }

    public WebSocketMessage initAuthenticatedSession(WebSocketMessage socketMsg, UserVS userVS) {
        try {
            if(ResponseVS.SC_WS_CONNECTION_INIT_OK == socketMsg.getStatusCode()) {
                socketMsg.getMessageJSON().put("userVS", userVS.toJSON());
                socketMsg.setUserVS(userVS);
                browserSessionDataJSON.put("userVS", userVS.toJSON());
                browserSessionDataJSON.put("isConnected", true);
                flush();
                VotingSystemApp.getInstance().setDeviceId(socketMsg.getDeviceId());
                BrowserVS.getInstance().execCommandJS(
                        socketMsg.getWebSocketCoreSignalJSCommand(WebSocketMessage.ConnectionStatus.OPEN));
            } else {
                showMessage(ResponseVS.SC_ERROR, socketMsg.getMessage());
                log.error("ERROR - initAuthenticatedSession - statusCode: " + socketMsg.getStatusCode());
            }
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return socketMsg;
    }

    public void setCSRRequestId(Long id) {
        browserSessionDataJSON.put("csrRequestId", id);
        flush();
    }

    public void setCryptoToken(CryptoTokenVS cryptoTokenVS, JSONObject deviceDataJSON) {
        log.debug("setCryptoToken - type: " + cryptoTokenVS.toString() + "- deviceDataJSON: " +
                ((deviceDataJSON != null)?deviceDataJSON.toString():"null"));
        ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN, cryptoTokenVS.toString());
        if(deviceDataJSON == null) deviceDataJSON = new JSONObject();
        deviceDataJSON.put("type", cryptoTokenVS.toString());
        browserSessionDataJSON.put("cryptoTokenVS", deviceDataJSON);
        flush();
    }

    //{"id":,"deviceName":"","certPEM":""}
    public JSONObject getCryptoToken() {
        return browserSessionDataJSON.getJSONObject("cryptoTokenVS");
    }

    public static CryptoTokenVS getCryptoTokenType () {
        String  tokenType = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.DNIe.toString());
        return CryptoTokenVS.valueOf(tokenType);
    }

    public String getCryptoTokenName() {
        if(browserSessionDataJSON.has("cryptoTokenVS")) {
            if(browserSessionDataJSON.getJSONObject("cryptoTokenVS").has("deviceName")) {
                return browserSessionDataJSON.getJSONObject("cryptoTokenVS").getString("deviceName");
            } else return null;
        } else return null;
    }

    public Long getCSRRequestId() {
        return browserSessionDataJSON.getLong("csrRequestId");
    }

    public String getDeviceId() {
        return browserSessionDataJSON.getString("deviceId");
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS, boolean isConnected) throws Exception {
        this.userVS = userVS;
        JSONArray userVSList = null;
        if(browserSessionDataJSON.has("userVSList")) {
            userVSList = browserSessionDataJSON.getJSONArray("userVSList");
            boolean updated = false;
            for(int i = 0; i < userVSList.size(); i++) {
                JSONObject user = (JSONObject) userVSList.get(i);
                if(user.getString("nif").equals(userVS.getNif())) {
                    userVSList.remove(i);
                    userVSList.add(userVS.toJSON());
                    updated = true;
                }
            }
            if(!updated) userVSList.add(userVS.toJSON());
        } else {
            userVSList = new JSONArray();
            userVSList.add(userVS.toJSON());
            browserSessionDataJSON.put("userVSList", userVSList);
        }
        browserSessionDataJSON.put("isConnected", isConnected);
        browserSessionDataJSON.put("userVS", userVS.toJSON());
        flush();
    }

    public JSONObject getBrowserSessionData() {
        return browserSessionDataJSON;
    }

    public void setCSRRequest(Long requestId, Encryptor.EncryptedBundle bundle) {
        try {
            File csrFile = new File(ContextVS.APPDIR + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
            csrFile.createNewFile();
            JSONObject jsonData = bundle.toJSON();
            jsonData.put("requestId", requestId);
            FileUtils.copyStreamToFile(new ByteArrayInputStream(jsonData.toString().getBytes()), csrFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }


    public void checkCSRRequest() {
        PlatformImpl.runLater(() -> checkCSR());
    }

    private void deleteCSR() {
        log.debug("deleteCSR");
        File csrFile = new File(ContextVS.APPDIR + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
        if(csrFile.exists()) csrFile.delete();
    }

    private void checkCSR() {
        File csrFile = new File(ContextVS.APPDIR + File.separator + ContextVS.USER_CSR_REQUEST_FILE_NAME);
        if(csrFile.exists()) {
            log.debug("csr request found");
            try {
                JSONObject jsonData = (JSONObject) JSONSerializer.toJSON(FileUtils.getStringFromFile(csrFile));
                String serviceURL = ContextVS.getInstance().getAccessControl().getUserCSRServiceURL(
                        jsonData.getLong("requestId"));
                ResponseVS responseVS = HttpHelper.getInstance().getData(serviceURL, null);
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(
                            responseVS.getMessage().getBytes());
                    X509Certificate userCert = certificates.iterator().next();
                    UserVS user = UserVS.getUserVS(userCert);
                    setUserVS(user, false);
                    log.debug("user: " + user.getNif() + " - certificates.size(): " + certificates.size());
                    X509Certificate[] certsArray = new X509Certificate[certificates.size()];
                    certificates.toArray(certsArray);
                    String passwd = null;
                    byte[] serializedCertificationRequest = null;
                    while(passwd == null) {
                        PasswordDialog passwordDialog = new PasswordDialog();
                        passwordDialog.show(ContextVS.getMessage("csrPasswMsg"));
                        passwd = passwordDialog.getPassword();
                        if(passwd == null) {
                            Button optionButton = new Button(ContextVS.getMessage("deletePendingCsrMsg"));
                            optionButton.setGraphic(Utils.getImage(FontAwesome.Glyph.TIMES, Utils.COLOR_RED_DARK));
                            optionButton.setOnAction(event -> deleteCSR());
                            showMessage(ContextVS.getMessage("certPendingMissingPasswdMsg"), optionButton);
                            return;
                        }
                        Encryptor.EncryptedBundle bundle = Encryptor.EncryptedBundle.parse(jsonData);
                        try {
                            serializedCertificationRequest = Encryptor.pbeAES_Decrypt(passwd, bundle);
                        } catch (Exception ex) {
                            passwd = null;
                            showMessage(ContextVS.getMessage("cryptoTokenPasswdErrorMsg"), ContextVS.getMessage("errorLbl"));
                        }
                    }
                    CertificationRequestVS certificationRequest =
                            (CertificationRequestVS) ObjectUtils.deSerializeObject(serializedCertificationRequest);
                    KeyStore userKeyStore = KeyStore.getInstance("JKS");
                    userKeyStore.load(null);
                    userKeyStore.setKeyEntry(ContextVS.KEYSTORE_USER_CERT_ALIAS, certificationRequest.getPrivateKey(),
                            passwd.toCharArray(), certsArray);
                    ContextVS.saveUserKeyStore(userKeyStore, passwd);
                    ContextVS.getInstance().setProperty(ContextVS.CRYPTO_TOKEN,
                            CryptoTokenVS.JKS_KEYSTORE.toString());
                    showMessage(ResponseVS.SC_OK, ContextVS.getMessage("certInstallOKMsg"));
                    csrFile.delete();
                } else showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("certPendingMsg"));
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("errorStoringKeyStoreMsg"));
            }
        }

    }

    public static SMIMEMessage getSMIME(String fromUser, String toUser, String textToSign,
            String password, String subject, Header... headers) throws Exception {
        String  tokenType = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.DNIe.toString());
        log.debug("getSMIME - tokenType: " + tokenType);
        switch(CryptoTokenVS.valueOf(tokenType)) {
            case JKS_KEYSTORE:
                KeyStore keyStore = ContextVS.getUserKeyStore(password.toCharArray());
                SMIMESignedGeneratorVS signedGenerator = new SMIMESignedGeneratorVS(keyStore,
                        ContextVS.KEYSTORE_USER_CERT_ALIAS, password.toCharArray(), ContextVS.DNIe_SIGN_MECHANISM);
                return signedGenerator.getSMIME(fromUser, toUser, textToSign, subject, headers);
            case DNIe:
                return DNIeContentSigner.getSMIME(fromUser, toUser, textToSign, password.toCharArray(), subject, headers);
            case MOBILE:
                countDownLatch = new CountDownLatch(1);
                DeviceVS deviceVS = DeviceVS.parse(getInstance().getCryptoToken());
                JSONObject jsonObject = WebSocketMessage.getSignRequest(deviceVS, toUser, textToSign, subject, headers);
                PlatformImpl.runLater(() -> {//Service must only be used from the FX Application Thread
                    try {
                        WebSocketService.getInstance().sendMessage(jsonObject.toString());
                    } catch (Exception ex) { log.error(ex.getMessage(), ex); }
                });
                countDownLatch.await();
                ResponseVS<SMIMEMessage> responseVS = getMessageToDeviceResponse();
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
                else return responseVS.getData();
            default: return null;
        }
    }

    public static void setSignResponse(WebSocketMessage socketMsg) {
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
                    log.error(ex.getMessage(), ex);
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR, ex.getMessage());
                }
                countDownLatch.countDown();
        }
    }

    public static ResponseVS getMessageToDeviceResponse() {
        return messageToDeviceResponse;
    }

    private void flush() {
        log.debug("flush");
        try {
            sessionDataJSON.put("browserSession", browserSessionDataJSON);
            FileUtils.copyStreamToFile(new ByteArrayInputStream(sessionDataJSON.toString().getBytes()), sessionFile);
            if(representativeStateJSON != null) FileUtils.copyStreamToFile(new ByteArrayInputStream(
                            representativeStateJSON.toString().getBytes()), representativeStateFile);
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}
