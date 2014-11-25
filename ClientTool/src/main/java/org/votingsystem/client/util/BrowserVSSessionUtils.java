package org.votingsystem.client.util;

import com.sun.javafx.application.PlatformImpl;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.controlsfx.glyphfont.FontAwesome;
import org.votingsystem.client.BrowserVS;
import org.votingsystem.client.dialog.MessageDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.model.Representation;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.dnie.DNIeContentSigner;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.smime.SMIMESignedGeneratorVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.*;
import javax.mail.Header;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class BrowserVSSessionUtils {

    private static Logger log = Logger.getLogger(BrowserVSSessionUtils.class);

    private UserVS userVS;
    private File sessionFile;
    private File representativeStateFile;
    private AnonymousDelegationRequest anonymousDelegationRequest;
    private JSONObject representativeStateJSON;
    private JSONObject browserSessionDataJSON;
    private JSONObject sessionDataJSON;
    private static CountDownLatch countDownLatch;
    private static WebSocketUtils.RequestBundle requestBundle;
    private static SMIMEMessage smimeMessage;
    private static ResponseVS<SMIMEMessage> messageToDeviceResponse;
    private static final BrowserVSSessionUtils INSTANCE = new BrowserVSSessionUtils();

    private BrowserVSSessionUtils() {
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

    public static BrowserVSSessionUtils getInstance() {
        return INSTANCE;
    }

    public void setIsConnected(boolean isConnected) {
        browserSessionDataJSON.put("isConnected", isConnected);
        flush();
    }

    public WebSocketMessage initAuthenticatedSession(WebSocketMessage message, UserVS userVS) {
        try {
            message.getMessageJSON().put("userVS", userVS.toJSON());
            message.setUserVS(userVS);
            browserSessionDataJSON.put("userVS", userVS.toJSON());
            browserSessionDataJSON.put("isConnected", true);
            flush();
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return message;
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
        PlatformImpl.runLater(new Runnable() { @Override public void run() { checkCSR(); } });
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
                            optionButton.setOnAction(new EventHandler<ActionEvent>() {
                                @Override public void handle(ActionEvent actionEvent) {
                                    deleteCSR();
                                }});
                            showMessage(ContextVS.getMessage("certPendingMissingPasswdMsg"), optionButton);
                            return;
                        }
                        Encryptor.EncryptedBundle bundle = Encryptor.EncryptedBundle.parse(jsonData);
                        try {
                            serializedCertificationRequest = Encryptor.pbeAES_Decrypt(passwd, bundle);
                        } catch (Exception ex) {
                            passwd = null;
                            showMessage(ContextVS.getMessage("cryptoTokenPasswdErrorMsg"), null);
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
                    showMessage(ContextVS.getMessage("certInstallOKMsg"), null);
                    csrFile.delete();
                } else showMessage(ContextVS.getMessage("certPendingMsg"), null);
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                showMessage(ContextVS.getMessage("errorStoringKeyStoreMsg"), null);
            }
        }

    }

    public static SMIMEMessage getSMIME(String fromUser, String toUser, String textToSign,
                    char[] password, String subject, Header... headers) throws Exception {
        String  tokenType = ContextVS.getInstance().getProperty(ContextVS.CRYPTO_TOKEN, CryptoTokenVS.DNIe.toString());
        log.debug("getSMIME - tokenType: " + tokenType);
        switch(CryptoTokenVS.valueOf(tokenType)) {
            case JKS_KEYSTORE:
                KeyStore keyStore = ContextVS.getUserKeyStore(password);
                SMIMESignedGeneratorVS signedGenerator = new SMIMESignedGeneratorVS(keyStore,
                        ContextVS.KEYSTORE_USER_CERT_ALIAS, password, ContextVS.DNIe_SIGN_MECHANISM);
                return signedGenerator.getSMIME(fromUser, toUser, textToSign, subject, headers);
            case DNIe:
                return DNIeContentSigner.getSMIME(fromUser, toUser, textToSign, password, subject, headers);
            case MOBILE:
                countDownLatch = new CountDownLatch(1);
                JSONObject mobileTokenJSON = getInstance().getCryptoToken();//{"id":,"deviceName":"","certPEM":""}
                Long deviceToId = mobileTokenJSON.getLong("id");
                String deviceToName = mobileTokenJSON.getString("deviceName");
                String deviceFromName = InetAddress.getLocalHost().getHostName();
                String certPEM = mobileTokenJSON.getString("certPEM");
                X509Certificate deviceToCert =  CertUtils.fromPEMToX509CertCollection(certPEM.getBytes()).iterator().next();
                requestBundle = WebSocketUtils.getSignRequest(deviceToId, deviceToName,
                    deviceFromName, toUser, textToSign, subject, ContextVS.getInstance().getLocale().getLanguage(),
                    deviceToCert, headers);
                BrowserVS.getInstance().sendWebSocketMessage(requestBundle.getRequest().toString());
                countDownLatch.await();
                ResponseVS<SMIMEMessage> responseVS = getMessageToDeviceResponse();
                if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
                else return responseVS.getData();
            default: return null;
        }
    }

    public static void setWebSocketMessage(WebSocketMessage message) {
        switch (message.getOperation()) {
            case MESSAGEVS_TO_DEVICE:
                if(ResponseVS.SC_ERROR == message.getStatusCode()) {
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR, message.getMessage());
                    countDownLatch.countDown();
                }
                break;
            case MESSAGEVS_FROM_DEVICE:
                try {
                    smimeMessage = message.getSignResponse(requestBundle);
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_OK, null, smimeMessage);
                } catch(Exception ex) {
                    log.error(ex.getMessage(), ex);
                    messageToDeviceResponse = new ResponseVS<>(ResponseVS.SC_ERROR, ex.getMessage());
                }
                countDownLatch.countDown();
                break;
        }
    }

    public static ResponseVS getMessageToDeviceResponse() {
        return messageToDeviceResponse;
    }

    public void showMessage(final String message, final Button optionButton) {
        PlatformImpl.runLater(new Runnable() {
            @Override public void run() {
                new MessageDialog().showHtmlMessage(message, optionButton);
            }
        });
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
