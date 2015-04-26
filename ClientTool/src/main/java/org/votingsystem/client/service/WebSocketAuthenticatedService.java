package org.votingsystem.client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.client.Browser;
import org.votingsystem.client.dialog.CertNotFoundDialog;
import org.votingsystem.client.dialog.PasswordDialog;
import org.votingsystem.client.dialog.ProgressDialog;
import org.votingsystem.client.util.InboxMessage;
import org.votingsystem.dto.DeviceVSDto;
import org.votingsystem.dto.OperationVS;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.ActorVS;
import org.votingsystem.model.DeviceVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.CurrencyServer;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CryptoTokenVS;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.votingsystem.client.Browser.showMessage;

/**
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WebSocketAuthenticatedService extends Service<ResponseVS> {

    private static Logger log = Logger.getLogger(WebSocketAuthenticatedService.class.getSimpleName());

    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    private static WebSocketAuthenticatedService instance;
    private final ClientManager client = ClientManager.createClient();
    private final String keyStorePassw = "";
    private ActorVS targetServer;
    private Session session;
    private UserVS userVS;
    private String connectionMessage = null;

    private WebSocketAuthenticatedService(Collection<X509Certificate> sslServerCertCollection, ActorVS targetServer) {
        this.targetServer = targetServer;
        if(targetServer.getWebSocketURL().startsWith("wss")) {
            log.info("settings for SECURE connetion");
            try {
                KeyStore p12Store = KeyStore.getInstance("PKCS12");
                p12Store.load(null, null);
                for(X509Certificate serverCert: sslServerCertCollection) {
                    p12Store.setCertificateEntry(serverCert.getSubjectDN().toString(), serverCert);
                }
                byte[] p12KeyStoreBytes = KeyStoreUtil.getBytes(p12Store, keyStorePassw.toCharArray());
                // Grizzly ssl configuration
                SSLContextConfigurator sslContext = new SSLContextConfigurator();
                sslContext.setTrustStoreType("PKCS12");
                sslContext.setTrustStoreBytes(p12KeyStoreBytes);
                sslContext.setTrustStorePass(keyStorePassw);
                SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslContext, true, false, false);
                client.getProperties().put(SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        } else log.info("settings for INSECURE connection");
    }

    public static WebSocketAuthenticatedService getInstance() {
        try {
            if(instance == null) instance =  new WebSocketAuthenticatedService(ContextVS.getInstance().
                    getVotingSystemSSLCerts(), ContextVS.getInstance().getCurrencyServer());
        } catch (Exception ex) { log.log(Level.SEVERE, ex.getMessage(), ex);}
        return instance;
    }

    public static class EndpointConfigurator extends ClientEndpointConfig.Configurator {

        @Override public void beforeRequest(Map<String, List<String>> headers) {
            //headers.put("Cookie", Arrays.asList("sessionVS=7180db71-3331-4e57-a448-5e7755e5dd3c"));
            headers.put("Origin", Arrays.asList(ContextVS.getInstance().getCurrencyServer().getServerURL()));
        }

        @Override public void afterResponse(HandshakeResponse handshakeResponse) {
            //final Map<String, List<String>> headers = handshakeResponse.getHeaders();
        }
    }

    @ClientEndpoint(configurator = EndpointConfigurator.class)
    public class WSEndpoint {

        @OnOpen public void onOpen(Session session) throws IOException {
            session.getBasicRemote().sendText(connectionMessage);
            WebSocketAuthenticatedService.this.session = session;
        }

        @OnClose public void onClose(Session session, CloseReason closeReason) {
            broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
            SessionService.getInstance().setIsConnected(false);
            EventBusService.getInstance().post(new ResponseVS(TypeVS.DISCONNECT));
        }

        @OnMessage public void onMessage(String message) {
            consumeMessage(message);
        }
    }

    @Override protected Task<ResponseVS> createTask() {
        return new WebSocketTask();
    }
    class WebSocketTask extends Task<ResponseVS> {

        @Override protected ResponseVS call() throws Exception {
            try {
                log.info("WebSocketTask - Connecting to " + targetServer.getWebSocketURL() + " ...");
                client.connectToServer(new WSEndpoint(), URI.create(targetServer.getWebSocketURL()));
            }catch (Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
            return null;
        }
    }

    private void connect(String password) {
        ProgressDialog.showDialog(new InitValidatedSessionTask(password, targetServer),
                ContextVS.getMessage("connectLbl"), Browser.getWindow());
    }

    public void setConnectionEnabled(boolean isConnectionEnabled){
        if(SessionService.getInstance().getUserVS() == null) {
            CertNotFoundDialog.show(Browser.getWindow());
            return;
        }
        if(isConnectionEnabled) {
            Platform.runLater(() -> {
                if(CryptoTokenVS.MOBILE != SessionService.getCryptoTokenType()) {
                    String password = null;
                    PasswordDialog passwordDialog = new PasswordDialog();
                    passwordDialog.showWithoutPasswordConfirm(
                            ContextVS.getMessage("initAuthenticatedSessionPasswordMsg"));
                    password = passwordDialog.getPassword();
                    if(password == null) {
                        broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
                    } else connect(password);
                } else if(CryptoTokenVS.MOBILE == SessionService.getCryptoTokenType()) {
                    connect(null);
                }
            });
        } else  {
            if(session != null && session.isOpen()) {
                try {session.close();}
                catch(Exception ex) {log.log(Level.SEVERE, ex.getMessage(), ex);}
            } else broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
        }
    }

    public boolean isConnected() {
        if(session != null && session.isOpen()) return true;
        else return false;
    }

    public boolean isConnectedWithAlert() {
        if(isConnected()) return true;
        else {
            showMessage(ResponseVS.SC_ERROR, ContextVS.getMessage("connectionRequiredForServiceErrorMsg"));
            return false;
        }
    }

    public void sendMessage(String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public ResponseVS sendMessageVS(OperationVS operationVS) throws Exception {
        log.info("sendMessageVS");
        if(isConnected()) {
            ResultListDto<DeviceVSDto> resultListDto = HttpHelper.getInstance().getData(
                    new TypeReference<ResultListDto<DeviceVSDto>>(){}, ((CurrencyServer) operationVS.getTargetServer())
                    .getDeviceVSConnectedServiceURL(operationVS.getNif()),  MediaTypeVS.JSON);
            boolean isMessageDelivered = false;
            for (DeviceVSDto deviceVSDto : resultListDto.getResultList()) {
                DeviceVS deviceVS = deviceVSDto.getDeviceVS();
                if(!SessionService.getInstance().getDeviceVS().getDeviceId().equals(deviceVS.getDeviceId())) {
                    SocketMessageDto messageDto = SocketMessageDto.getMessageVSToDevice(SessionService.getInstance().getUserVS(),
                            deviceVS, operationVS.getNif(), operationVS.getMessage());
                    sendMessage(JSON.getMapper().writeValueAsString(messageDto));
                    isMessageDelivered = true;
                }
            }
            if(isMessageDelivered) return new ResponseVS(ResponseVS.SC_OK);
            else return new ResponseVS(ResponseVS.SC_ERROR, ContextVS.getMessage(
                    "uservsWithoutDevicesConnectedMsg", operationVS.getNif()));
        } throw new ExceptionVS(ContextVS.getMessage("authenticatedWebSocketConnectionRequiredMsg"));
    }

    private void consumeMessage(final String socketMsgStr) {
        try {
            SocketMessageDto messageDto = JSON.getMapper().readValue(socketMsgStr, SocketMessageDto.class);
            WebSocketSession socketSession = ContextVS.getInstance().getWSSession(messageDto.getUUID());
            log.info("consumeMessage - type: " + messageDto.getOperation() + " - status: " + messageDto.getStatusCode());
            if(ResponseVS.SC_ERROR == messageDto.getStatusCode()) {
                showMessage(messageDto.getStatusCode(), messageDto.getMessage());
                return;
            }
            if(socketSession != null && messageDto.isEncrypted()) {
                messageDto.decryptMessage(socketSession.getAESParams());
            }
            messageDto.setWebSocketSession(socketSession);
            ResponseVS responseVS = null;
            switch(messageDto.getOperation()) {
                case MESSAGEVS:
                case MESSAGEVS_TO_DEVICE:
                    InboxService.getInstance().newMessage(new InboxMessage(messageDto));
                    break;
                case MESSAGEVS_FROM_VS:
                    if(socketSession != null) {
                        messageDto.setOperation(socketSession.getTypeVS());
                        switch(socketSession.getTypeVS()) {
                            case INIT_SIGNED_SESSION:
                                SessionService.getInstance().initAuthenticatedSession(messageDto, userVS);
                                break;
                            default:
                                log.log(Level.SEVERE, "MESSAGEVS_FROM_VS - TypeVS: " + socketSession.getTypeVS());
                        }
                        responseVS = new ResponseVS(null, socketSession.getTypeVS(), messageDto);
                    }
                    break;
                case MESSAGEVS_FROM_DEVICE:
                    responseVS = new ResponseVS(null, socketSession.getTypeVS(), messageDto);
                    break;
                case MESSAGEVS_SIGN_RESPONSE:
                    SessionService.setSignResponse(messageDto);
                    break;
                default:
                    log.info("unprocessed socketMsg: " + messageDto.getOperation());
            }
            if(responseVS != null) EventBusService.getInstance().post(responseVS);
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void broadcastConnectionStatus(SocketMessageDto.ConnectionStatus status) {
        if(session == null) log.info("broadcastConnectionStatus - status: " + status.toString());
        else log.info("broadcastConnectionStatus - status: " + status.toString() + " - session: " + session.getId());
        switch (status) {
            case CLOSED:
                Browser.getInstance().runJSCommand(CoreSignal.getWebSocketCoreSignalJSCommand(
                        null, SocketMessageDto.ConnectionStatus.CLOSED));
                break;
            case OPEN:
                Browser.getInstance().runJSCommand(CoreSignal.getWebSocketCoreSignalJSCommand(
                        null, SocketMessageDto.ConnectionStatus.OPEN));
                break;
        }
    }

    public class InitValidatedSessionTask extends Task<ResponseVS> {

        private String password;
        private ActorVS targetServer;

        public InitValidatedSessionTask (String password, ActorVS targetServer) {
            this.password = password;
            this.targetServer = targetServer;
        }

        @Override protected ResponseVS call() throws Exception {
            SocketMessageDto dto = SocketMessageDto.INIT_SESSION_REQUEST(
                    SessionService.getInstance().getDeviceVS().getDeviceId());
            ResponseVS responseVS = null;
            try {
                if(SessionService.getCryptoTokenType() == CryptoTokenVS.MOBILE) {
                    updateMessage(ContextVS.getMessage("checkDeviceVSCryptoTokenMsg"));
                } else updateMessage(ContextVS.getMessage("connectionMsg"));
                SMIMEMessage smimeMessage = SessionService.getSMIME(null, targetServer.getName(),
                        JSON.getMapper().writeValueAsString(dto), password,
                        ContextVS.getMessage("initAuthenticatedSessionMsgSubject"));
                MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, targetServer.getTimeStampServiceURL());
                userVS = smimeMessage.getSigner();
                smimeMessage = timeStamper.call();
                connectionMessage = JSON.getMapper().writeValueAsString(dto.setSMIME(smimeMessage));
                PlatformImpl.runLater(() -> WebSocketAuthenticatedService.this.restart());
                responseVS = ResponseVS.OK().setSMIME(smimeMessage);

            } catch(InterruptedException ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                broadcastConnectionStatus(SocketMessageDto.ConnectionStatus.CLOSED);
                showMessage(ResponseVS.SC_ERROR_REQUEST, ex.getMessage());
            }
            return responseVS;
        }
    }

}